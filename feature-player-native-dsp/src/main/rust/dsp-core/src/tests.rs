use super::*;
use oximedia_audio_dsp::loudness::momentary::MomentaryMeter;
use std::f64::consts::{LN_2, PI};

#[test]
fn every_pcm_encoding_round_trips_integer_extremes_and_low_bits() {
    for encoding in [
        Encoding::U8,
        Encoding::I16,
        Encoding::I16Be,
        Encoding::I24,
        Encoding::I24Be,
        Encoding::I32,
        Encoding::I32Be,
        Encoding::F32,
    ] {
        let mut bytes = [0; 4];
        for value in [-1.0, -0.5, 0.0, 0.5, 0.999999] {
            encoding.encode(value, &mut bytes[..encoding.bytes()]);
            let decoded = encoding.decode(&bytes[..encoding.bytes()]);
            let error = if encoding == Encoding::F32 {
                1e-7
            } else {
                1.0 / 2.0_f64.powi((encoding.bytes() * 8 - 1) as i32)
            };
            assert!(
                (decoded - value).abs() <= error,
                "{encoding:?}: {value} -> {decoded}"
            );
        }
    }
    for encoding in [Encoding::I32, Encoding::I32Be] {
        for value in [i32::MIN, i32::MIN + 1, -1, 0, 1, i32::MAX - 1, i32::MAX] {
            let original = if encoding == Encoding::I32 {
                value.to_le_bytes()
            } else {
                value.to_be_bytes()
            };
            let mut output = [0; 4];
            encoding.encode(encoding.decode(&original), &mut output);
            assert_eq!(original, output);
        }
    }
}

#[test]
fn bypass_and_analysis_only_preserve_every_byte_even_nonfinite_float() {
    let controls = Arc::new(Controls::default());
    let mut engine = Engine::new(Arc::clone(&controls), 192000, 2, Encoding::F32, 128).unwrap();
    let input: Vec<u8> = (0..8192).map(|i| (i * 137) as u8).collect();
    let mut output = vec![0; input.len()];
    for enabled in [false, true] {
        controls.analysis.set_enabled(enabled);
        controls.analysis.set_playing(true);
        assert_eq!(engine.process(&input, &mut output), Ok(input.len()));
        assert_eq!(input, output);
    }
}

#[test]
fn malformed_pcm_is_rejected_without_writing_output() {
    let mut engine =
        Engine::new(Arc::new(Controls::default()), 48000, 2, Encoding::I24, 8).unwrap();
    let mut output = [77; 7];
    assert_eq!(engine.process(&[0; 7], &mut output), Err(-3));
    assert_eq!(output, [77; 7]);
}

#[test]
fn large_blocks_are_chunked_without_dropping_or_reordering_pcm() {
    let controls = Arc::new(Controls::default());
    controls.publish(Parameters {
        eq: true,
        loudness: false,
        target_lufs: -14.0,
        gains: [0.0; 10],
    });
    let mut engine = Engine::new(controls, 96000, 2, Encoding::I32, 31).unwrap();
    let input: Vec<u8> = (0..1024_i32)
        .flat_map(|v| (v * 1048577).to_le_bytes())
        .collect();
    let mut output = vec![0; input.len()];
    assert_eq!(engine.process(&input, &mut output), Ok(input.len()));
    assert_eq!(output, input);
}

#[test]
fn octave_eq_matches_independent_rbj_bandwidth_coefficients() {
    for rate in [8000, 44100, 48000, 96000, 192000] {
        for band in 0..10 {
            let f = effects::EQ_FREQUENCIES[band].min(rate as f64 * 0.45);
            let w = 2.0 * PI * f / rate as f64;
            for gain in [-12.0, 0.0, 12.0] {
                let a = 10.0_f64.powf(gain / 40.0);
                let alpha = w.sin() * (LN_2 / 2.0 * w / w.sin()).sinh();
                let a0 = 1.0 + alpha / a;
                let expected = [
                    (1.0 + alpha * a) / a0,
                    -2.0 * w.cos() / a0,
                    (1.0 - alpha * a) / a0,
                    -2.0 * w.cos() / a0,
                    (1.0 - alpha / a) / a0,
                ];
                let c = effects::eq_coefficients(rate, band, gain);
                for (actual, expected) in [c.b0, c.b1, c.b2, c.a1, c.a2].iter().zip(expected) {
                    assert!((actual - expected).abs() < 1e-12);
                }
            }
        }
    }
}

// Independent reference: libebur128 1.2.6, mono amplitude 0.1 sine, 2 s
// processed, final 400 ms measurement. Generated from the previous vendored
// library with ebur128_add_frames_double / ebur128_loudness_momentary.
#[test]
fn lufs_matches_libebur128_reference_at_four_sample_rates() {
    let references = [
        (44100, [-24.8309712294, -23.0078791982, -19.7284270319]),
        (48000, [-24.8347980499, -23.0106603973, -19.7333030126]),
        (96000, [-24.8564424819, -23.0280885797, -19.7592805883]),
        (192000, [-24.8672697031, -23.0378875127, -19.7712093791]),
    ];
    for (rate, expected) in references {
        for (frequency, expected) in [100.0, 997.0, 4000.0].iter().zip(expected) {
            let mut meter = MomentaryMeter::new(rate, &[1.0]);
            for i in 0..rate * 2 {
                meter.process_frame(&[0.1 * (2.0 * PI * frequency * i as f64 / rate as f64).sin()]);
            }
            let actual = meter.loudness();
            assert!(
                (actual - expected).abs() < 0.1,
                "{rate} Hz / {frequency} Hz: {actual} vs {expected}"
            );
        }
    }
}

#[test]
fn loudness_channel_weights_exclude_lfe_and_include_center_and_surround() {
    let weights = effects::channel_weights(6).unwrap();
    let mut lfe = MomentaryMeter::new(48000, &weights);
    let mut center = MomentaryMeter::new(48000, &weights);
    let mut surround = MomentaryMeter::new(48000, &weights);
    for i in 0..48000 {
        let x = 0.1 * (2.0 * PI * 997.0 * i as f64 / 48000.0).sin();
        lfe.process_frame(&[0.0, 0.0, 0.0, x, 0.0, 0.0]);
        center.process_frame(&[0.0, 0.0, x, 0.0, 0.0, 0.0]);
        surround.process_frame(&[0.0, 0.0, 0.0, 0.0, x, 0.0]);
    }
    assert_eq!(lfe.loudness(), f64::NEG_INFINITY);
    assert!((surround.loudness() - center.loudness() - 10.0 * 1.41_f64.log10()).abs() < 1e-8);
    assert_eq!(effects::channel_weights(3).unwrap(), vec![1.0, 1.0, 1.0]);
    assert!(effects::channel_weights(16).is_none());
}

#[test]
fn silence_and_reset_do_not_leave_meter_history() {
    let mut meter = MomentaryMeter::new(44100, &[1.0, 1.0]);
    for _ in 0..44100 {
        meter.process_frame(&[0.25, 0.25]);
    }
    meter.reset();
    for _ in 0..44100 {
        meter.process_frame(&[0.0, 0.0]);
    }
    assert_eq!(meter.loudness(), f64::NEG_INFINITY);
}

#[test]
fn limiter_links_stereo_and_stays_within_sample_ceiling() {
    let mut effects = effects::Effects::new(48000, 2);
    effects.set_parameters(false, true, -14.0, [0.0; 10]);
    let mut input = vec![0.0; 2048];
    for frame in input.chunks_exact_mut(2) {
        frame[0] = 2.0;
        frame[1] = 1.0;
    }
    effects.process(&mut input);
    for frame in input.chunks_exact(2) {
        assert!(frame[0].abs() <= 0.98000001);
        assert!((frame[0] - 2.0 * frame[1]).abs() < 1e-12);
    }
}

#[test]
fn invalid_configuration_is_rejected() {
    for (rate, channels, frames) in [
        (0, 2, 1024),
        (48000, 0, 1024),
        (48000, 17, 1024),
        (48000, 2, 0),
    ] {
        assert!(Engine::new(
            Arc::new(Controls::default()),
            rate,
            channels,
            Encoding::I16,
            frames
        )
        .is_none());
    }
}

#[test]
fn explicit_channel_masks_distinguish_three_front_channels_from_lfe() {
    assert_eq!(
        effects::weights_for_mask(3, 4 | 8 | 16).unwrap(),
        vec![1.0, 1.0, 1.0]
    );
    assert_eq!(
        effects::weights_for_mask(3, 4 | 8 | 32).unwrap(),
        vec![1.0, 1.0, 0.0]
    );
    assert_eq!(
        effects::weights_for_mask(3, -1),
        effects::channel_weights(3)
    );
    assert_eq!(
        effects::weights_for_mask(6, -1),
        effects::channel_weights(6)
    );
    assert!(effects::weights_for_mask(16, -1).is_none());
    // Multichannel masks that disagree with the channel count stay ambiguous.
    assert!(effects::weights_for_mask(3, 4 | 8).is_none());
    assert!(effects::weights_for_mask(3, 1 | 2 | 4).is_none());
}

#[test]
fn nonstandard_mono_and_stereo_masks_keep_loudness_enabled() {
    // Legacy CHANNEL_CONFIGURATION_MONO / _STEREO pass Media3's bit-count check.
    assert_eq!(effects::weights_for_mask(1, 2), Some(vec![1.0]));
    assert_eq!(effects::weights_for_mask(2, 3), Some(vec![1.0, 1.0]));
    assert_eq!(
        effects::weights_for_mask(2, 1 << 20 | 1 << 21),
        Some(vec![1.0, 1.0])
    );
    let mut effects = effects::Effects::with_channel_mask(48000, 2, 3);
    effects.set_parameters(false, true, -14.0, [0.0; 10]);
    assert!(effects.active());
}

#[test]
fn eq_skips_bands_that_settle_at_zero_db() {
    let mut effects = effects::Effects::new(48000, 2);
    let mut gains = [0.0; 10];
    gains[5] = 6.0;
    effects.set_parameters(true, false, -14.0, gains);
    let mut block = vec![0.1; 960 * 2];
    effects.process(&mut block);
    assert_eq!(effects.processed_band_count(), 1);
    // Moving the band back to flat runs it until the smoothed gain reaches 0 dB.
    effects.set_parameters(true, false, -14.0, [0.0; 10]);
    effects.process(&mut block);
    assert_eq!(effects.processed_band_count(), 1);
    for _ in 0..50 {
        effects.process(&mut block);
    }
    assert_eq!(effects.processed_band_count(), 0);
    let mut flat = vec![0.25; 960 * 2];
    effects.process(&mut flat);
    assert!(flat.iter().all(|v| *v == 0.25));
}

#[test]
fn reconfigure_keeps_smoothed_eq_gains_instead_of_fading_in_from_flat() {
    let controls = Arc::new(Controls::default());
    let mut gains = [0.0; 10];
    gains[1] = 12.0;
    controls.publish(Parameters {
        eq: true,
        loudness: false,
        target_lufs: -14.0,
        gains,
    });
    // 20 ms of 48 kHz stereo 16-bit PCM, matching the sink's chunk size.
    let input = vec![0; 960 * 2 * 2];
    let mut output = vec![0; input.len()];
    let mut previous = Engine::new(Arc::clone(&controls), 48000, 2, Encoding::I16, 960).unwrap();
    for _ in 0..50 {
        previous.process(&input, &mut output).unwrap();
    }
    let carried = previous.eq_gains();
    drop(previous);
    let mut next = Engine::new(Arc::clone(&controls), 48000, 2, Encoding::I16, 960).unwrap();
    next.restore_eq_gains(carried);
    next.process(&input, &mut output).unwrap();
    assert!((next.eq_gains()[1] - 12.0).abs() < 1e-3);
}

#[test]
fn asynchronous_fft_uses_effective_high_resolution_rate_and_reset_discards_results() {
    for rate in [48000, 96000, 192000] {
        let controls = Arc::new(Controls::default());
        controls.analysis.set_enabled(true);
        controls.analysis.set_playing(true);
        let mut engine = Engine::new(Arc::clone(&controls), rate, 1, Encoding::F32, 32768).unwrap();
        let mut bands = [0.0; analysis::BAND_COUNT];
        let after = controls.analysis.await_bands(0, &mut bands, 1);
        let factor = (rate as usize).div_ceil(48000);
        let input: Vec<u8> = (0..analysis::FFT_SIZE * factor)
            .flat_map(|i| ((2.0 * PI * 1000.0 * i as f64 / rate as f64).sin() as f32).to_le_bytes())
            .collect();
        let mut output = vec![0; input.len()];
        engine.process(&input, &mut output).unwrap();
        let sequence = controls.analysis.await_bands(after, &mut bands, 1000);
        assert_ne!(sequence, after);
        let peak = (0..bands.len())
            .max_by(|a, b| bands[*a].total_cmp(&bands[*b]))
            .unwrap();
        assert_eq!(peak, 17, "rate {rate}: {bands:?}");
        assert_eq!(input, output);
        controls.analysis.set_enabled(false);
        controls.analysis.await_bands(sequence, &mut bands, 1);
        assert_eq!(bands, [0.0; analysis::BAND_COUNT]);
    }
}
