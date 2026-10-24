use spica_dsp_core::{pcm::Encoding, Controls, Engine, Parameters};
use std::alloc::{GlobalAlloc, Layout, System};
use std::cell::Cell;
use std::sync::Arc;

thread_local! {
    static TRACK: Cell<bool> = const { Cell::new(false) };
    static ALLOCATIONS: Cell<usize> = const { Cell::new(0) };
}
struct Tracking;
unsafe impl GlobalAlloc for Tracking {
    unsafe fn alloc(&self, layout: Layout) -> *mut u8 {
        if TRACK.try_with(Cell::get).unwrap_or(false) {
            ALLOCATIONS.with(|n| n.set(n.get() + 1));
        }
        System.alloc(layout)
    }
    unsafe fn dealloc(&self, ptr: *mut u8, layout: Layout) {
        System.dealloc(ptr, layout);
    }
    unsafe fn realloc(&self, ptr: *mut u8, layout: Layout, size: usize) -> *mut u8 {
        if TRACK.try_with(Cell::get).unwrap_or(false) {
            ALLOCATIONS.with(|n| n.set(n.get() + 1));
        }
        System.realloc(ptr, layout, size)
    }
}
#[global_allocator]
static ALLOCATOR: Tracking = Tracking;

#[test]
fn audio_thread_does_not_allocate_while_eq_and_loudness_process() {
    let controls = Arc::new(Controls::default());
    controls.publish(Parameters {
        eq: true,
        loudness: true,
        target_lufs: -14.0,
        gains: [3.0; 10],
    });
    let mut engine = Engine::new(controls, 192000, 8, Encoding::F32, 1024).unwrap();
    let input = vec![0; 4096 * 8 * 4];
    let mut output = vec![0; input.len()];
    engine.process(&input, &mut output).unwrap();
    ALLOCATIONS.with(|n| n.set(0));
    TRACK.with(|n| n.set(true));
    for _ in 0..64 {
        engine.process(&input, &mut output).unwrap();
    }
    TRACK.with(|n| n.set(false));
    assert_eq!(ALLOCATIONS.with(Cell::get), 0);
}

#[test]
fn audio_thread_does_not_allocate_while_feeding_and_waking_fft_worker() {
    let controls = Arc::new(Controls::default());
    controls.analysis.set_enabled(true);
    controls.analysis.set_playing(true);
    let mut engine = Engine::new(Arc::clone(&controls), 48000, 2, Encoding::F32, 960).unwrap();
    let input = vec![0; 960 * 2 * 4];
    let mut output = vec![0; input.len()];
    engine.process(&input, &mut output).unwrap();
    ALLOCATIONS.with(|n| n.set(0));
    TRACK.with(|n| n.set(true));
    // 每 20 ms 一块，约每 5 块凑满一个 FFT 窗口并唤醒 worker。
    for _ in 0..64 {
        engine.process(&input, &mut output).unwrap();
    }
    TRACK.with(|n| n.set(false));
    assert_eq!(ALLOCATIONS.with(Cell::get), 0);
}
