use oximedia_audio_dsp::dsp::biquad::BiquadCoefficients;
use oximedia_audio_dsp::dsp::eq::{EqBand, Equalizer, EqualizerConfig};
use oximedia_audio_dsp::loudness::momentary::MomentaryMeter;
use std::f64::consts::{LN_2, PI};

pub const EQ_FREQUENCIES: [f64; 10] = [
    31.0, 62.0, 125.0, 250.0, 500.0, 1000.0, 2000.0, 4000.0, 8000.0, 16000.0,
];
const SILENT_BAND_DB: f64 = 1e-4;
/// Sample-peak ceilings of the channel-linked, zero-lookahead protection.
/// Loudness gain (up to 4x) keeps headroom; EQ alone only prevents clipping.
const LOUDNESS_CEILING: f64 = 0.98;
const FULL_SCALE: f64 = 1.0;
pub fn octave_q(frequency: f64, sample_rate: u32) -> f64 {
    let w = 2.0 * PI * frequency / sample_rate as f64;
    1.0 / (2.0 * (LN_2 * 0.5 * w / w.sin()).sinh())
}
pub fn channel_weights(channels: usize) -> Option<Vec<f64>> {
    // Canonical Android decoded PCM order. Unknown layouts bypass loudness.
    Some(match channels {
        1 => vec![1.0],
        2 => vec![1.0, 1.0],
        3 => vec![1.0, 1.0, 1.0],
        4 => vec![1.0, 1.0, 1.41, 1.41],
        5 => vec![1.0, 1.0, 1.0, 1.41, 1.41],
        6 => vec![1.0, 1.0, 1.0, 0.0, 1.41, 1.41],
        7 => vec![1.0, 1.0, 1.0, 0.0, 1.41, 1.41, 1.41],
        8 => vec![1.0, 1.0, 1.0, 0.0, 1.41, 1.41, 1.41, 1.41],
        _ => return None,
    })
}
pub fn weights_for_mask(channels: usize, mask: i32) -> Option<Vec<f64>> {
    if mask < 0 {
        // Without layout metadata (e.g. the FFmpeg extension) DefaultAudioSink
        // plays the canonical layout from Util.getAudioTrackChannelConfig.
        return channel_weights(channels);
    }
    let mask = mask as u32;
    if mask.count_ones() as usize != channels || mask & !0x1ffc != 0 {
        // Mono/stereo weights do not depend on the layout, so legacy or vendor
        // masks (e.g. CHANNEL_CONFIGURATION_STEREO) must not disable loudness.
        return if channels <= 2 {
            channel_weights(channels)
        } else {
            None
        };
    }
    Some(
        (2..13)
            .filter(|bit| mask & (1 << bit) != 0)
            .map(|bit| match bit {
                5 => 0.0,                     // Android CHANNEL_OUT_LOW_FREQUENCY.
                6 | 7 | 10 | 11 | 12 => 1.41, // back / side / back-center.
                _ => 1.0,
            })
            .collect(),
    )
}
pub struct Effects {
    eq: Equalizer,
    sample_rate: u32,
    channels: usize,
    current_gains: [f64; 10],
    target_gains: [f64; 10],
    eq_enabled: bool,
    loudness_enabled: bool,
    meter: Option<MomentaryMeter>,
    target_lufs: f64,
    gain: f64,
    limiter_gain: f64,
}
impl Effects {
    /// Canonical layout for the channel count, as with Media3's NO_VALUE mask.
    pub fn new(sample_rate: u32, channels: usize) -> Self {
        Self::with_channel_mask(sample_rate, channels, -1)
    }
    pub fn with_channel_mask(sample_rate: u32, channels: usize, channel_mask: i32) -> Self {
        let weights = weights_for_mask(channels, channel_mask);
        let mut config = EqualizerConfig::new();
        for frequency in EQ_FREQUENCIES {
            let frequency = frequency.min(sample_rate as f64 * 0.45);
            config = config.add_band(EqBand::peaking(
                frequency,
                0.0,
                octave_q(frequency, sample_rate),
            ));
        }
        Self {
            eq: Equalizer::new(config, sample_rate as f64, channels),
            sample_rate,
            channels,
            current_gains: [0.0; 10],
            target_gains: [0.0; 10],
            eq_enabled: false,
            loudness_enabled: false,
            meter: weights.map(|weights| MomentaryMeter::new(sample_rate, &weights)),
            target_lufs: -14.0,
            gain: 1.0,
            limiter_gain: 1.0,
        }
    }
    pub fn set_parameters(&mut self, eq: bool, loudness: bool, target: f64, gains: [f64; 10]) {
        if loudness != self.loudness_enabled {
            if let Some(meter) = &mut self.meter {
                meter.reset();
            }
            self.gain = 1.0;
        }
        if loudness != self.loudness_enabled || eq != self.eq_enabled {
            // The protection ceiling depends on the mode; instant attack keeps
            // the output under the new ceiling right after this reset.
            self.limiter_gain = 1.0;
        }
        self.eq_enabled = eq;
        self.loudness_enabled = loudness;
        self.target_lufs = if target.is_finite() {
            target.clamp(-40.0, 0.0)
        } else {
            -14.0
        };
        self.target_gains = gains.map(|v| {
            if v.is_finite() {
                v.clamp(-12.0, 12.0)
            } else {
                0.0
            }
        });
    }
    pub fn current_gains(&self) -> [f64; 10] {
        self.current_gains
    }
    #[cfg(test)]
    pub(crate) fn processed_band_count(&self) -> usize {
        self.eq.config().bands.iter().filter(|b| b.enabled).count()
    }
    /// Continues smoothing from a previous instance instead of from flat.
    pub fn restore_gains(&mut self, gains: [f64; 10]) {
        self.current_gains = gains;
    }
    pub fn active(&self) -> bool {
        self.eq_enabled || (self.loudness_enabled && self.meter.is_some())
    }
    pub fn process(&mut self, samples: &mut [f64]) {
        let frames = samples.len() / self.channels;
        if self.eq_enabled {
            let amount = 1.0 - (-(frames as f64) / (0.03 * self.sample_rate as f64)).exp();
            let mut any_band = false;
            for (band, original_frequency) in EQ_FREQUENCIES.iter().enumerate() {
                self.current_gains[band] +=
                    (self.target_gains[band] - self.current_gains[band]) * amount;
                // A peaking band near 0 dB is an identity filter: skip its biquad
                // (as the C++ engine did). Its DF1 history already tracks the
                // input, so re-enabling it later does not click.
                let usable = *original_frequency < self.sample_rate as f64 * 0.48
                    && self.current_gains[band].abs() >= SILENT_BAND_DB;
                any_band |= usable;
                let frequency = original_frequency.min(self.sample_rate as f64 * 0.45);
                self.eq.update_band(
                    band,
                    EqBand::peaking(
                        frequency,
                        self.current_gains[band],
                        octave_q(frequency, self.sample_rate),
                    )
                    .with_enabled(usable),
                );
            }
            if any_band {
                self.eq.process_interleaved(samples, frames);
            }
        }
        let (next_gain, ceiling) = match &mut self.meter {
            Some(meter) if self.loudness_enabled => {
                for frame in samples.chunks_exact(self.channels) {
                    meter.process_frame(frame);
                }
                let lufs = meter.loudness();
                let target_gain = if lufs.is_finite() && lufs > -70.0 {
                    10.0_f64
                        .powf((self.target_lufs - lufs) / 20.0)
                        .clamp(0.25, 4.0)
                } else {
                    self.gain
                };
                let seconds = if target_gain > self.gain { 0.8 } else { 0.2 };
                let amount = 1.0 - (-(frames as f64) / (seconds * self.sample_rate as f64)).exp();
                (
                    self.gain + (target_gain - self.gain) * amount,
                    LOUDNESS_CEILING,
                )
            }
            // EQ boosts can exceed full scale, which the PCM encoder would hard
            // clip. Engage only above full scale so flat EQ and masters that
            // stay in range pass bit-exactly (gain stays 1.0 without loudness).
            _ if self.eq_enabled => (self.gain, FULL_SCALE),
            _ => return,
        };
        let release = 1.0 - (-1.0 / (0.15 * self.sample_rate as f64)).exp();
        for (i, frame) in samples.chunks_exact_mut(self.channels).enumerate() {
            let t = if frames > 1 {
                i as f64 / (frames - 1) as f64
            } else {
                1.0
            };
            let gain = self.gain + (next_gain - self.gain) * t;
            let peak = frame.iter().map(|v| (v * gain).abs()).fold(0.0, f64::max);
            let desired = if peak > ceiling { ceiling / peak } else { 1.0 };
            if desired < self.limiter_gain {
                self.limiter_gain = desired;
            } else {
                self.limiter_gain =
                    (self.limiter_gain + (1.0 - self.limiter_gain) * release).min(desired);
            }
            for sample in frame {
                *sample = (*sample * gain * self.limiter_gain).clamp(-1.0, 1.0);
            }
        }
        self.gain = next_gain;
    }
    pub fn reset(&mut self) {
        self.eq.reset();
        if let Some(meter) = &mut self.meter {
            meter.reset();
        }
        self.gain = 1.0;
        self.limiter_gain = 1.0;
    }
}
pub fn eq_coefficients(sample_rate: u32, band: usize, gain: f64) -> BiquadCoefficients {
    let frequency = EQ_FREQUENCIES[band].min(sample_rate as f64 * 0.45);
    BiquadCoefficients::peaking(
        sample_rate as f64,
        frequency,
        octave_q(frequency, sample_rate),
        gain,
    )
}
