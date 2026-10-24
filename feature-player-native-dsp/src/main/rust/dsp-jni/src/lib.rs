//! JNI is the only unsafe boundary. NativeDspEngine's lifecycle read/write
//! lock keeps handles alive until every in-flight native call has returned.
use jni::objects::{JByteBuffer, JFloatArray, JObject};
use jni::sys::{jboolean, jint, jlong, JNI_FALSE, JNI_TRUE};
use jni::JNIEnv;
use spica_dsp_core::{analysis::BAND_COUNT, pcm::Encoding, Controls, Engine, Parameters};
use std::panic::{catch_unwind, AssertUnwindSafe};
use std::sync::{Arc, Mutex, MutexGuard, TryLockError};

struct Handle {
    controls: Arc<Controls>,
    engine: Mutex<Option<Engine>>,
}

fn guarded<T: Copy>(fallback: T, action: impl FnOnce() -> T) -> T {
    catch_unwind(AssertUnwindSafe(action)).unwrap_or(fallback)
}

/// Never blocks. A panic caught by `guarded` while the guard was held poisons
/// the slot; recover it and reset the possibly half-updated filter state
/// instead of leaving DSP disabled for the rest of the process.
fn try_lock_engine(handle: &Handle) -> Option<MutexGuard<'_, Option<Engine>>> {
    match handle.engine.try_lock() {
        Ok(slot) => Some(slot),
        Err(TryLockError::WouldBlock) => None,
        Err(TryLockError::Poisoned(poisoned)) => {
            handle.engine.clear_poison();
            handle.controls.reset();
            Some(poisoned.into_inner())
        }
    }
}

// SAFETY: Java owns the Box and holds a lifecycle read lock for the entire
// call. Only nativeRelease, under its exclusive write lock, frees the Box.
unsafe fn handle<'a>(value: jlong) -> Option<&'a Handle> {
    if value == 0 {
        None
    } else {
        (value as *const Handle).as_ref()
    }
}

#[no_mangle]
pub extern "system" fn Java_me_spica27_spicamusic_dsp_NativeDspEngine_nativeCreate(
    _env: JNIEnv,
    _object: JObject,
) -> jlong {
    guarded(0, || {
        Box::into_raw(Box::new(Handle {
            controls: Arc::new(Controls::default()),
            engine: Mutex::new(None),
        })) as jlong
    })
}

#[no_mangle]
pub extern "system" fn Java_me_spica27_spicamusic_dsp_NativeDspEngine_nativeConfigure(
    _env: JNIEnv,
    _object: JObject,
    id: jlong,
    sample_rate: jint,
    channels: jint,
    encoding: jint,
    max_frames: jint,
    channel_mask: jint,
) -> jboolean {
    guarded(JNI_FALSE, || {
        let Some(handle) = (unsafe { handle(id) }) else {
            return JNI_FALSE;
        };
        let Some(encoding) = Encoding::from_media3(encoding) else {
            return JNI_FALSE;
        };
        let Some(mut slot) = try_lock_engine(handle) else {
            return JNI_FALSE;
        };
        // Stop the previous worker before the new producer uses the same ring,
        // but keep its smoothed EQ so a new track does not fade in from flat.
        let eq_gains = slot.take().map(|previous| previous.eq_gains());
        *slot = Engine::with_channel_mask(
            Arc::clone(&handle.controls),
            sample_rate as u32,
            channels as usize,
            encoding,
            max_frames as usize,
            channel_mask,
        );
        if let (Some(engine), Some(gains)) = (slot.as_mut(), eq_gains) {
            engine.restore_eq_gains(gains);
        }
        if slot.is_some() {
            JNI_TRUE
        } else {
            JNI_FALSE
        }
    })
}

#[no_mangle]
pub extern "system" fn Java_me_spica27_spicamusic_dsp_NativeDspEngine_nativeProcess(
    env: JNIEnv,
    _object: JObject,
    id: jlong,
    input: JByteBuffer,
    output: JByteBuffer,
    input_position: jint,
    output_position: jint,
    byte_count: jint,
) -> jint {
    guarded(-1, || {
        let Some(handle) = (unsafe { handle(id) }) else {
            return -1;
        };
        if input_position < 0 || output_position < 0 || byte_count < 0 {
            return -2;
        }
        let (Ok(input_ptr), Ok(output_ptr), Ok(input_size), Ok(output_size)) = (
            env.get_direct_buffer_address(&input),
            env.get_direct_buffer_address(&output),
            env.get_direct_buffer_capacity(&input),
            env.get_direct_buffer_capacity(&output),
        ) else {
            return -2;
        };
        let count = byte_count as usize;
        let from = input_position as usize;
        let to = output_position as usize;
        if from.checked_add(count).is_none_or(|end| end > input_size)
            || to.checked_add(count).is_none_or(|end| end > output_size)
        {
            return -2;
        }
        let input_start = input_ptr as usize + from;
        let output_start = output_ptr as usize + to;
        // Reject aliasing, including duplicate ByteBuffers sharing native memory.
        if input_start < output_start.saturating_add(count)
            && output_start < input_start.saturating_add(count)
        {
            return -2;
        }
        let Some(mut slot) = try_lock_engine(handle) else {
            return -1;
        };
        let Some(engine) = slot.as_mut() else {
            return -1;
        };
        // SAFETY: both ranges were checked against live DirectByteBuffer
        // capacities, are disjoint, and Java retains their owners during JNI.
        let source = unsafe { std::slice::from_raw_parts(input_ptr.add(from), count) };
        let destination = unsafe { std::slice::from_raw_parts_mut(output_ptr.add(to), count) };
        match engine.process(source, destination) {
            Ok(size) => size as jint,
            Err(code) => code,
        }
    })
}

#[no_mangle]
pub extern "system" fn Java_me_spica27_spicamusic_dsp_NativeDspEngine_nativeSetParameters(
    env: JNIEnv,
    _object: JObject,
    id: jlong,
    gains: JFloatArray,
    eq: jboolean,
    loudness: jboolean,
    target: f32,
    fft: jboolean,
) {
    guarded((), || {
        let Some(handle) = (unsafe { handle(id) }) else {
            return;
        };
        let mut values = [0.0_f32; 10];
        if !gains.is_null() {
            let Ok(size) = env.get_array_length(&gains) else {
                return;
            };
            if env
                .get_float_array_region(&gains, 0, &mut values[..(size as usize).min(10)])
                .is_err()
            {
                return;
            }
        }
        handle.controls.publish(Parameters {
            eq: eq != 0,
            loudness: loudness != 0,
            target_lufs: target as f64,
            gains: values.map(f64::from),
        });
        handle.controls.analysis.set_enabled(fft != 0);
    })
}

#[no_mangle]
pub extern "system" fn Java_me_spica27_spicamusic_dsp_NativeDspEngine_nativeSetFftEnabled(
    _env: JNIEnv,
    _object: JObject,
    id: jlong,
    enabled: jboolean,
) {
    guarded((), || {
        if let Some(h) = unsafe { handle(id) } {
            h.controls.analysis.set_enabled(enabled != 0);
        }
    })
}
#[no_mangle]
pub extern "system" fn Java_me_spica27_spicamusic_dsp_NativeDspEngine_nativeSetPlaybackActive(
    _env: JNIEnv,
    _object: JObject,
    id: jlong,
    active: jboolean,
) {
    guarded((), || {
        if let Some(h) = unsafe { handle(id) } {
            h.controls.analysis.set_playing(active != 0);
        }
    })
}
#[no_mangle]
pub extern "system" fn Java_me_spica27_spicamusic_dsp_NativeDspEngine_nativeAwaitBands(
    env: JNIEnv,
    _object: JObject,
    id: jlong,
    after: jlong,
    output: JFloatArray,
    timeout: jint,
) -> jlong {
    guarded(0, || {
        let Some(h) = (unsafe { handle(id) }) else {
            return 0;
        };
        if env.get_array_length(&output).unwrap_or(0) < BAND_COUNT as i32 {
            return 0;
        }
        let mut values = [0.0; BAND_COUNT];
        let sequence = h.controls.analysis.await_bands(
            after as u64,
            &mut values,
            timeout.clamp(1, 1000) as u64,
        );
        if env.set_float_array_region(&output, 0, &values).is_err() {
            return 0;
        }
        sequence as jlong
    })
}
#[no_mangle]
pub extern "system" fn Java_me_spica27_spicamusic_dsp_NativeDspEngine_nativeReset(
    _env: JNIEnv,
    _object: JObject,
    id: jlong,
) {
    guarded((), || {
        if let Some(h) = unsafe { handle(id) } {
            h.controls.reset();
        }
    })
}
#[no_mangle]
pub extern "system" fn Java_me_spica27_spicamusic_dsp_NativeDspEngine_nativeResetFft(
    _env: JNIEnv,
    _object: JObject,
    id: jlong,
) {
    guarded((), || {
        if let Some(h) = unsafe { handle(id) } {
            h.controls.analysis.reset();
        }
    })
}
#[no_mangle]
pub extern "system" fn Java_me_spica27_spicamusic_dsp_NativeDspEngine_nativeRelease(
    _env: JNIEnv,
    _object: JObject,
    id: jlong,
) {
    guarded((), || {
        // SAFETY: exclusive Kotlin lifecycle lock; no reader or audio call
        // can retain the handle. Engine::drop joins its FFT worker.
        if id != 0 {
            drop(unsafe { Box::from_raw(id as *mut Handle) });
        }
    })
}

#[cfg(test)]
mod tests {
    use super::*;
    use std::sync::atomic::Ordering;

    #[test]
    fn poisoned_engine_slot_recovers_instead_of_disabling_dsp() {
        let handle = Handle {
            controls: Arc::new(Controls::default()),
            engine: Mutex::new(None),
        };
        let generation = handle.controls.reset_generation.load(Ordering::Acquire);
        guarded((), || {
            let _slot = handle.engine.lock().unwrap();
            panic!("simulated DSP panic");
        });
        assert!(handle.engine.is_poisoned());
        assert!(try_lock_engine(&handle).is_some());
        assert!(!handle.engine.is_poisoned());
        assert_ne!(
            generation,
            handle.controls.reset_generation.load(Ordering::Acquire)
        );
    }
}
