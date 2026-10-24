#![forbid(unsafe_code)]
pub mod analysis;
pub mod effects;
pub mod pcm;

use analysis::{Analysis, Analyzer};
use effects::Effects;
use pcm::Encoding;
use std::sync::atomic::{fence, AtomicBool, AtomicU32, AtomicU64, Ordering};
use std::sync::Arc;

#[derive(Clone, Copy)]
pub struct Parameters {
    pub eq: bool,
    pub loudness: bool,
    pub target_lufs: f64,
    pub gains: [f64; 10],
}
pub struct Controls {
    sequence: AtomicU64,
    eq: AtomicBool,
    loudness: AtomicBool,
    target: AtomicU32,
    gains: [AtomicU32; 10],
    pub reset_generation: AtomicU64,
    pub analysis: Arc<Analysis>,
}
impl Default for Controls {
    fn default() -> Self {
        Self {
            sequence: AtomicU64::new(0),
            eq: AtomicBool::new(false),
            loudness: AtomicBool::new(false),
            target: AtomicU32::new((-14.0_f32).to_bits()),
            gains: std::array::from_fn(|_| AtomicU32::new(0)),
            reset_generation: AtomicU64::new(0),
            analysis: Arc::new(Analysis::default()),
        }
    }
}
impl Controls {
    /// One serialized control writer; audio readers never wait for it.
    pub fn publish(&self, parameters: Parameters) {
        self.sequence.fetch_add(1, Ordering::Relaxed);
        // Pairs with snapshot's acquire fence: a reader that observes any of the
        // stores below must also observe the odd sequence and retry. A release
        // RMW alone does not order the later relaxed stores after it.
        fence(Ordering::Release);
        self.eq.store(parameters.eq, Ordering::Relaxed);
        self.loudness.store(parameters.loudness, Ordering::Relaxed);
        self.target
            .store((parameters.target_lufs as f32).to_bits(), Ordering::Relaxed);
        for (slot, value) in self.gains.iter().zip(parameters.gains) {
            slot.store((value as f32).to_bits(), Ordering::Relaxed);
        }
        self.sequence.fetch_add(1, Ordering::Release);
    }
    fn snapshot(&self) -> Option<(u64, Parameters)> {
        let sequence = self.sequence.load(Ordering::Acquire);
        if sequence & 1 != 0 {
            return None;
        }
        let parameters = Parameters {
            eq: self.eq.load(Ordering::Relaxed),
            loudness: self.loudness.load(Ordering::Relaxed),
            target_lufs: f32::from_bits(self.target.load(Ordering::Relaxed)) as f64,
            gains: std::array::from_fn(|i| {
                f32::from_bits(self.gains[i].load(Ordering::Relaxed)) as f64
            }),
        };
        fence(Ordering::Acquire);
        (self.sequence.load(Ordering::Relaxed) == sequence).then_some((sequence, parameters))
    }
    pub fn reset(&self) {
        self.reset_generation.fetch_add(1, Ordering::AcqRel);
        self.analysis.reset();
    }
}

pub struct Engine {
    controls: Arc<Controls>,
    encoding: Encoding,
    channels: usize,
    buffer: Vec<f64>,
    analyzer: Analyzer,
    effects: Effects,
    parameter_sequence: u64,
    reset_generation: u64,
}
impl Engine {
    /// Canonical layout for the channel count, as with Media3's NO_VALUE mask.
    pub fn new(
        controls: Arc<Controls>,
        sample_rate: u32,
        channels: usize,
        encoding: Encoding,
        max_frames: usize,
    ) -> Option<Self> {
        Self::with_channel_mask(controls, sample_rate, channels, encoding, max_frames, -1)
    }
    pub fn with_channel_mask(
        controls: Arc<Controls>,
        sample_rate: u32,
        channels: usize,
        encoding: Encoding,
        max_frames: usize,
        channel_mask: i32,
    ) -> Option<Self> {
        if !(8000..=768000).contains(&sample_rate)
            || !(1..=16).contains(&channels)
            || !(1..=32768).contains(&max_frames)
        {
            return None;
        }
        let analyzer = Analyzer::new(Arc::clone(&controls.analysis), sample_rate)?;
        let reset_generation = controls.reset_generation.load(Ordering::Acquire);
        Some(Self {
            controls,
            encoding,
            channels,
            buffer: vec![0.0; max_frames * channels],
            analyzer,
            effects: Effects::with_channel_mask(sample_rate, channels, channel_mask),
            parameter_sequence: u64::MAX,
            reset_generation,
        })
    }
    /// Smoothed EQ gains currently applied, carried across reconfiguration.
    pub fn eq_gains(&self) -> [f64; 10] {
        self.effects.current_gains()
    }
    pub fn restore_eq_gains(&mut self, gains: [f64; 10]) {
        self.effects.restore_gains(gains);
    }
    pub fn process(&mut self, input: &[u8], output: &mut [u8]) -> Result<usize, i32> {
        let frame_bytes = self.encoding.bytes() * self.channels;
        if output.len() < input.len() || !input.len().is_multiple_of(frame_bytes) {
            return Err(-3);
        }
        // 在解码 PCM 前捕获代次，重置期间仍在处理的旧块不能伪装成新片段。
        let analysis_generation = self.controls.analysis.current_generation();
        let generation = self.controls.reset_generation.load(Ordering::Acquire);
        if generation != self.reset_generation {
            self.effects.reset();
            self.reset_generation = generation;
        }
        if let Some((sequence, p)) = self.controls.snapshot() {
            if sequence != self.parameter_sequence {
                self.effects
                    .set_parameters(p.eq, p.loudness, p.target_lufs, p.gains);
                self.parameter_sequence = sequence;
            }
        }
        let effects = self.effects.active();
        let fft = self.controls.analysis.enabled.load(Ordering::Acquire);
        if !effects && !fft {
            output[..input.len()].copy_from_slice(input);
            return Ok(input.len());
        }
        let max_bytes = self.buffer.len() * self.encoding.bytes();
        for (source, destination) in input.chunks(max_bytes).zip(output.chunks_mut(max_bytes)) {
            let count = source.len() / self.encoding.bytes();
            let samples = &mut self.buffer[..count];
            for (sample, bytes) in samples
                .iter_mut()
                .zip(source.chunks_exact(self.encoding.bytes()))
            {
                *sample = self.encoding.decode(bytes);
            }
            if fft {
                self.analyzer
                    .push_captured(samples, self.channels, analysis_generation);
            }
            if effects {
                self.effects.process(samples);
                for (sample, bytes) in samples
                    .iter()
                    .zip(destination.chunks_exact_mut(self.encoding.bytes()))
                {
                    self.encoding.encode(*sample, bytes);
                }
            } else {
                destination[..source.len()].copy_from_slice(source);
            }
        }
        Ok(input.len())
    }
}

#[cfg(test)]
mod tests;
