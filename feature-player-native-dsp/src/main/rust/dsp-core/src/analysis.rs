use oximedia_audio_dsp::spectrum::fft::FftProcessor;
use std::sync::atomic::{AtomicBool, AtomicU32, AtomicU64, AtomicUsize, Ordering};
use std::sync::{Arc, Condvar, Mutex};
use std::thread::{self, JoinHandle};
use std::time::Duration;

pub const FFT_SIZE: usize = 4096;
pub const BAND_COUNT: usize = 31;
const RING_SIZE: usize = 32768;
const FREQUENCIES: [f64; BAND_COUNT] = [
    20.0, 25.0, 32.0, 40.0, 50.0, 63.0, 80.0, 100.0, 125.0, 160.0, 200.0, 250.0, 315.0, 400.0,
    500.0, 630.0, 800.0, 1000.0, 1250.0, 1600.0, 2000.0, 2500.0, 3150.0, 4000.0, 5000.0, 6300.0,
    8000.0, 10000.0, 12500.0, 16000.0, 20000.0,
];

pub struct BandState {
    pub sequence: u64,
    pub values: [f32; BAND_COUNT],
}
pub struct Analysis {
    pub enabled: AtomicBool,
    pub playing: AtomicBool,
    generation: AtomicU64,
    ring: Vec<AtomicU32>,
    epochs: Vec<AtomicU64>,
    write: AtomicUsize,
    read: AtomicUsize,
    bands: Mutex<BandState>,
    changed: Condvar,
}
impl Default for Analysis {
    fn default() -> Self {
        Self {
            enabled: AtomicBool::new(false),
            playing: AtomicBool::new(false),
            generation: AtomicU64::new(0),
            ring: (0..RING_SIZE).map(|_| AtomicU32::new(0)).collect(),
            epochs: (0..RING_SIZE).map(|_| AtomicU64::new(0)).collect(),
            write: AtomicUsize::new(0),
            read: AtomicUsize::new(0),
            bands: Mutex::new(BandState {
                sequence: 1,
                values: [0.0; BAND_COUNT],
            }),
            changed: Condvar::new(),
        }
    }
}
impl Analysis {
    pub(crate) fn current_generation(&self) -> u64 {
        self.generation.load(Ordering::Acquire)
    }
    pub fn reset(&self) {
        let mut bands = self.bands.lock().unwrap_or_else(|p| p.into_inner());
        self.generation.fetch_add(1, Ordering::AcqRel);
        bands.values.fill(0.0);
        bands.sequence = bands.sequence.wrapping_add(1).max(1);
        self.changed.notify_all();
    }
    pub fn set_enabled(&self, enabled: bool) {
        if self.enabled.swap(enabled, Ordering::AcqRel) != enabled {
            self.reset();
        }
    }
    pub fn set_playing(&self, playing: bool) {
        if self.playing.swap(playing, Ordering::AcqRel) != playing {
            self.reset();
        }
    }
    pub fn await_bands(&self, after: u64, output: &mut [f32; BAND_COUNT], timeout_ms: u64) -> u64 {
        let bands = self.bands.lock().unwrap_or_else(|p| p.into_inner());
        let (bands, _) = self
            .changed
            .wait_timeout_while(
                bands,
                Duration::from_millis(timeout_ms.clamp(1, 1000)),
                |b| b.sequence == after,
            )
            .unwrap_or_else(|p| p.into_inner());
        *output = bands.values;
        bands.sequence
    }
}

pub struct Analyzer {
    shared: Arc<Analysis>,
    stop: Arc<AtomicBool>,
    worker: Option<JoinHandle<()>>,
    factor: usize,
    sum: f64,
    count: usize,
    generation: u64,
}
impl Analyzer {
    pub fn new(shared: Arc<Analysis>, sample_rate: u32) -> Option<Self> {
        let mut fft = FftProcessor::new(FFT_SIZE)?;
        let stop = Arc::new(AtomicBool::new(false));
        let factor = (sample_rate as usize).div_ceil(48000).max(1);
        let analysis_rate = sample_rate as f64 / factor as f64;
        shared.reset();
        shared
            .read
            .store(shared.write.load(Ordering::Acquire), Ordering::Release);
        let generation = shared.generation.load(Ordering::Acquire);
        let state = Arc::clone(&shared);
        let stopping = Arc::clone(&stop);
        let worker = thread::Builder::new()
            .name("OxiMedia-FFT".into())
            .spawn(move || {
                let mut input = [0.0_f32; FFT_SIZE];
                while !stopping.load(Ordering::Acquire) {
                    let current = state.generation.load(Ordering::Acquire);
                    let write = state.write.load(Ordering::Acquire);
                    let mut read = state.read.load(Ordering::Relaxed);
                    // 逐样本判断所属片段，只丢弃旧数据，保留重置后已入队的第一窗。
                    while read != write
                        && state.epochs[read & (RING_SIZE - 1)].load(Ordering::Relaxed) != current
                    {
                        if current != state.generation.load(Ordering::Acquire) {
                            break;
                        }
                        read = read.wrapping_add(1);
                    }
                    if current != state.generation.load(Ordering::Acquire) {
                        continue;
                    }
                    state.read.store(read, Ordering::Release);
                    // 停用、暂停或数据不足一窗时阻塞，由生产者凑满一窗或 Drop 时 unpark。
                    // park 令牌保证先到的 unpark 不会丢失。
                    if !state.enabled.load(Ordering::Acquire)
                        || !state.playing.load(Ordering::Acquire)
                        || write.wrapping_sub(read) < FFT_SIZE
                    {
                        thread::park();
                        continue;
                    }
                    let available = write.wrapping_sub(read);
                    if available > FFT_SIZE * 2 {
                        read = write.wrapping_sub(FFT_SIZE);
                    }
                    let mut valid = true;
                    for (i, sample) in input.iter_mut().enumerate() {
                        let slot = read.wrapping_add(i) & (RING_SIZE - 1);
                        if state.epochs[slot].load(Ordering::Relaxed) != current {
                            valid = false;
                            break;
                        }
                        *sample = f32::from_bits(state.ring[slot].load(Ordering::Relaxed));
                    }
                    if !valid {
                        continue;
                    }
                    state
                        .read
                        .store(read.wrapping_add(FFT_SIZE), Ordering::Release);
                    let spectrum = fft.process(&input);
                    let mut result = [0.0; BAND_COUNT];
                    for (band, value) in result.iter_mut().enumerate() {
                        let center = FREQUENCIES[band];
                        let low = if band == 0 {
                            16.0
                        } else {
                            (center * FREQUENCIES[band - 1]).sqrt()
                        };
                        let high = if band + 1 == BAND_COUNT {
                            22000.0
                        } else {
                            (center * FREQUENCIES[band + 1]).sqrt()
                        };
                        if low >= analysis_rate / 2.0 {
                            continue;
                        }
                        let resolution = analysis_rate / FFT_SIZE as f64;
                        let from = ((low / resolution) as usize).clamp(1, FFT_SIZE / 2 - 1);
                        let to = ((high.min(analysis_rate / 2.0) / resolution) as usize)
                            .min(FFT_SIZE / 2 - 1)
                            .max(from);
                        let average = spectrum[from..=to].iter().map(|v| v.norm()).sum::<f64>()
                            / (to - from + 1) as f64;
                        *value = if average > 0.0 {
                            (20.0 * average.log10() / 60.0).clamp(0.0, 1.0) as f32
                        } else {
                            0.0
                        };
                    }
                    let mut bands = state.bands.lock().unwrap_or_else(|p| p.into_inner());
                    if current == state.generation.load(Ordering::Acquire)
                        && state.enabled.load(Ordering::Acquire)
                        && state.playing.load(Ordering::Acquire)
                    {
                        bands.values = result;
                        bands.sequence = bands.sequence.wrapping_add(1).max(1);
                        state.changed.notify_all();
                    }
                }
            })
            .ok()?;
        Some(Self {
            shared,
            stop,
            worker: Some(worker),
            factor,
            sum: 0.0,
            count: 0,
            generation,
        })
    }
    pub fn push(&mut self, samples: &[f64], channels: usize) {
        let generation = self.shared.current_generation();
        self.push_captured(samples, channels, generation);
    }
    pub(crate) fn push_captured(&mut self, samples: &[f64], channels: usize, generation: u64) {
        if !self.shared.enabled.load(Ordering::Acquire)
            || !self.shared.playing.load(Ordering::Acquire)
        {
            return;
        }
        self.push_with_generation(samples, channels, generation);
    }
    fn push_with_generation(&mut self, samples: &[f64], channels: usize, generation: u64) {
        if self.generation != generation {
            self.sum = 0.0;
            self.count = 0;
            self.generation = generation;
        }
        let mut write = self.shared.write.load(Ordering::Relaxed);
        let read = self.shared.read.load(Ordering::Acquire);
        for frame in samples.chunks_exact(channels) {
            self.sum += if channels == 1 {
                frame[0]
            } else {
                (frame[0] + frame[1]) * 0.5
            };
            self.count += 1;
            if self.count < self.factor {
                continue;
            }
            let sample = (self.sum / self.factor as f64) as f32;
            self.sum = 0.0;
            self.count = 0;
            if write.wrapping_sub(read) >= RING_SIZE - 1 {
                break;
            }
            self.shared.ring[write & (RING_SIZE - 1)].store(sample.to_bits(), Ordering::Relaxed);
            self.shared.epochs[write & (RING_SIZE - 1)].store(generation, Ordering::Relaxed);
            write = write.wrapping_add(1);
        }
        self.shared.write.store(write, Ordering::Release);
        // unpark 不加锁也不分配，可在音频线程调用。`read` 只会前进，
        // 这里的估计最多多唤醒一次，不会漏掉已凑满的窗口。
        if write.wrapping_sub(read) >= FFT_SIZE {
            if let Some(worker) = &self.worker {
                worker.thread().unpark();
            }
        }
    }
}
impl Drop for Analyzer {
    fn drop(&mut self) {
        self.stop.store(true, Ordering::Release);
        if let Some(worker) = self.worker.take() {
            worker.thread().unpark();
            let _ = worker.join();
        }
    }
}

#[cfg(test)]
mod tests {
    use super::*;
    use std::f64::consts::PI;

    fn tone(frequency: f64) -> Vec<f64> {
        (0..FFT_SIZE)
            .map(|i| (2.0 * PI * frequency * i as f64 / 48000.0).sin())
            .collect()
    }

    #[test]
    fn first_window_queued_after_reset_is_not_discarded() {
        let shared = Arc::new(Analysis::default());
        let mut analyzer = Analyzer::new(Arc::clone(&shared), 48000).unwrap();
        shared.set_enabled(true);
        shared.set_playing(true);
        let mut bands = [0.0; BAND_COUNT];
        let after = shared.await_bands(0, &mut bands, 1);
        analyzer.push(&tone(1000.0), 1);
        assert_ne!(shared.await_bands(after, &mut bands, 1000), after);
        assert!(bands[17] > 0.0);
    }

    #[test]
    fn old_producer_finishing_after_reset_cannot_publish_old_spectrum() {
        let shared = Arc::new(Analysis::default());
        shared.set_enabled(true);
        shared.set_playing(true);
        let mut analyzer = Analyzer::new(Arc::clone(&shared), 48000).unwrap();
        let old_generation = shared.generation.load(Ordering::Acquire);
        shared.reset();
        let mut bands = [0.0; BAND_COUNT];
        let after = shared.await_bands(0, &mut bands, 1);
        // 模拟旧 PCM 块在跳转前取得代次，跳转后才完成入队。
        analyzer.push_with_generation(&tone(4000.0), 1, old_generation);
        assert_eq!(shared.await_bands(after, &mut bands, 50), after);
        assert_eq!(bands, [0.0; BAND_COUNT]);
        analyzer.push(&tone(1000.0), 1);
        assert_ne!(shared.await_bands(after, &mut bands, 1000), after);
        let peak = (0..BAND_COUNT)
            .max_by(|a, b| bands[*a].total_cmp(&bands[*b]))
            .unwrap();
        assert_eq!(peak, 17);
    }

    #[test]
    fn worker_parked_while_paused_resumes_when_playback_restarts() {
        let shared = Arc::new(Analysis::default());
        let mut analyzer = Analyzer::new(Arc::clone(&shared), 48000).unwrap();
        shared.set_enabled(true);
        // 未播放时 worker 进入 park，只能靠生产者唤醒。
        thread::sleep(Duration::from_millis(20));
        shared.set_playing(true);
        let mut bands = [0.0; BAND_COUNT];
        let after = shared.await_bands(0, &mut bands, 1);
        analyzer.push(&tone(1000.0), 1);
        assert_ne!(shared.await_bands(after, &mut bands, 1000), after);
        assert!(bands[17] > 0.0);
    }

    #[test]
    fn dropping_analyzer_wakes_parked_worker_instead_of_hanging() {
        let analyzer = Analyzer::new(Arc::new(Analysis::default()), 48000).unwrap();
        thread::sleep(Duration::from_millis(20));
        let (done, finished) = std::sync::mpsc::channel();
        thread::spawn(move || {
            drop(analyzer);
            let _ = done.send(());
        });
        assert!(finished.recv_timeout(Duration::from_secs(1)).is_ok());
    }
}
