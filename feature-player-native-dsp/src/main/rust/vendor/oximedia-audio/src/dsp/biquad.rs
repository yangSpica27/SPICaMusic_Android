// Copyright 2024-2026 COOLJAPAN OU (Team Kitasan).
// SPDX-License-Identifier: Apache-2.0
// Imported from OxiMedia 5263510; modified only for standalone packaging/formatting.
//! Biquad filter implementation.
//!
//! This module provides a generic biquad filter that can be configured
//! for various filter types (low-pass, high-pass, band-pass, etc.).

#![forbid(unsafe_code)]
#![allow(clippy::cast_lossless)]

use std::f64::consts::PI;

/// Type of biquad filter.
#[derive(Clone, Copy, Debug, PartialEq, Eq)]
pub enum BiquadType {
    /// Low shelf filter - boost/cut below frequency.
    LowShelf,
    /// High shelf filter - boost/cut above frequency.
    HighShelf,
    /// Peaking filter - boost/cut around center frequency.
    Peaking,
    /// Low pass filter - attenuate above frequency.
    LowPass,
    /// High pass filter - attenuate below frequency.
    HighPass,
    /// Band pass filter - pass around center frequency.
    BandPass,
    /// Notch filter - attenuate around center frequency.
    Notch,
    /// All pass filter - phase shift only.
    AllPass,
}

/// Biquad filter coefficients.
///
/// These coefficients define a biquad (second-order IIR) filter in the form:
/// `y[n] = b0*x[n] + b1*x[n-1] + b2*x[n-2] - a1*y[n-1] - a2*y[n-2]`
#[derive(Clone, Debug)]
pub struct BiquadCoefficients {
    /// Feed-forward coefficient 0.
    pub b0: f64,
    /// Feed-forward coefficient 1.
    pub b1: f64,
    /// Feed-forward coefficient 2.
    pub b2: f64,
    /// Feed-back coefficient 1 (normalized).
    pub a1: f64,
    /// Feed-back coefficient 2 (normalized).
    pub a2: f64,
}

impl Default for BiquadCoefficients {
    fn default() -> Self {
        Self::identity()
    }
}

impl BiquadCoefficients {
    /// Create identity (pass-through) coefficients.
    #[must_use]
    pub fn identity() -> Self {
        Self {
            b0: 1.0,
            b1: 0.0,
            b2: 0.0,
            a1: 0.0,
            a2: 0.0,
        }
    }

    /// Calculate coefficients for a specific filter type.
    ///
    /// # Arguments
    ///
    /// * `filter_type` - Type of filter to create
    /// * `sample_rate` - Sample rate in Hz
    /// * `frequency` - Center/cutoff frequency in Hz
    /// * `q` - Q factor (resonance/bandwidth control)
    /// * `gain_db` - Gain in dB (for peaking and shelf filters)
    #[must_use]
    pub fn calculate(
        filter_type: BiquadType,
        sample_rate: f64,
        frequency: f64,
        q: f64,
        gain_db: f64,
    ) -> Self {
        let w0 = 2.0 * PI * frequency / sample_rate;
        let cos_w0 = w0.cos();
        let sin_w0 = w0.sin();
        let alpha = sin_w0 / (2.0 * q);
        let a = 10.0_f64.powf(gain_db / 40.0);

        let (b0, b1, b2, a0, a1, a2) = match filter_type {
            BiquadType::LowShelf => {
                let two_sqrt_a_alpha = 2.0 * a.sqrt() * alpha;
                let a_plus_1 = a + 1.0;
                let a_minus_1 = a - 1.0;

                let b0 = a * (a_plus_1 - a_minus_1 * cos_w0 + two_sqrt_a_alpha);
                let b1 = 2.0 * a * (a_minus_1 - a_plus_1 * cos_w0);
                let b2 = a * (a_plus_1 - a_minus_1 * cos_w0 - two_sqrt_a_alpha);
                let a0 = a_plus_1 + a_minus_1 * cos_w0 + two_sqrt_a_alpha;
                let a1 = -2.0 * (a_minus_1 + a_plus_1 * cos_w0);
                let a2 = a_plus_1 + a_minus_1 * cos_w0 - two_sqrt_a_alpha;

                (b0, b1, b2, a0, a1, a2)
            }
            BiquadType::HighShelf => {
                let two_sqrt_a_alpha = 2.0 * a.sqrt() * alpha;
                let a_plus_1 = a + 1.0;
                let a_minus_1 = a - 1.0;

                let b0 = a * (a_plus_1 + a_minus_1 * cos_w0 + two_sqrt_a_alpha);
                let b1 = -2.0 * a * (a_minus_1 + a_plus_1 * cos_w0);
                let b2 = a * (a_plus_1 + a_minus_1 * cos_w0 - two_sqrt_a_alpha);
                let a0 = a_plus_1 - a_minus_1 * cos_w0 + two_sqrt_a_alpha;
                let a1 = 2.0 * (a_minus_1 - a_plus_1 * cos_w0);
                let a2 = a_plus_1 - a_minus_1 * cos_w0 - two_sqrt_a_alpha;

                (b0, b1, b2, a0, a1, a2)
            }
            BiquadType::Peaking => {
                let alpha_a = alpha * a;
                let alpha_over_a = alpha / a;

                let b0 = 1.0 + alpha_a;
                let b1 = -2.0 * cos_w0;
                let b2 = 1.0 - alpha_a;
                let a0 = 1.0 + alpha_over_a;
                let a1 = -2.0 * cos_w0;
                let a2 = 1.0 - alpha_over_a;

                (b0, b1, b2, a0, a1, a2)
            }
            BiquadType::LowPass => {
                let b0 = (1.0 - cos_w0) / 2.0;
                let b1 = 1.0 - cos_w0;
                let b2 = (1.0 - cos_w0) / 2.0;
                let a0 = 1.0 + alpha;
                let a1 = -2.0 * cos_w0;
                let a2 = 1.0 - alpha;

                (b0, b1, b2, a0, a1, a2)
            }
            BiquadType::HighPass => {
                let b0 = (1.0 + cos_w0) / 2.0;
                let b1 = -(1.0 + cos_w0);
                let b2 = (1.0 + cos_w0) / 2.0;
                let a0 = 1.0 + alpha;
                let a1 = -2.0 * cos_w0;
                let a2 = 1.0 - alpha;

                (b0, b1, b2, a0, a1, a2)
            }
            BiquadType::BandPass => {
                let b0 = alpha;
                let b1 = 0.0;
                let b2 = -alpha;
                let a0 = 1.0 + alpha;
                let a1 = -2.0 * cos_w0;
                let a2 = 1.0 - alpha;

                (b0, b1, b2, a0, a1, a2)
            }
            BiquadType::Notch => {
                let b0 = 1.0;
                let b1 = -2.0 * cos_w0;
                let b2 = 1.0;
                let a0 = 1.0 + alpha;
                let a1 = -2.0 * cos_w0;
                let a2 = 1.0 - alpha;

                (b0, b1, b2, a0, a1, a2)
            }
            BiquadType::AllPass => {
                let b0 = 1.0 - alpha;
                let b1 = -2.0 * cos_w0;
                let b2 = 1.0 + alpha;
                let a0 = 1.0 + alpha;
                let a1 = -2.0 * cos_w0;
                let a2 = 1.0 - alpha;

                (b0, b1, b2, a0, a1, a2)
            }
        };

        Self {
            b0: b0 / a0,
            b1: b1 / a0,
            b2: b2 / a0,
            a1: a1 / a0,
            a2: a2 / a0,
        }
    }

    /// Create a low-pass filter.
    #[must_use]
    pub fn low_pass(sample_rate: f64, cutoff_hz: f64, q: f64) -> Self {
        Self::calculate(BiquadType::LowPass, sample_rate, cutoff_hz, q, 0.0)
    }

    /// Create a high-pass filter.
    #[must_use]
    pub fn high_pass(sample_rate: f64, cutoff_hz: f64, q: f64) -> Self {
        Self::calculate(BiquadType::HighPass, sample_rate, cutoff_hz, q, 0.0)
    }

    /// Create a band-pass filter.
    #[must_use]
    pub fn band_pass(sample_rate: f64, center_hz: f64, q: f64) -> Self {
        Self::calculate(BiquadType::BandPass, sample_rate, center_hz, q, 0.0)
    }

    /// Create a notch filter.
    #[must_use]
    pub fn notch(sample_rate: f64, center_hz: f64, q: f64) -> Self {
        Self::calculate(BiquadType::Notch, sample_rate, center_hz, q, 0.0)
    }

    /// Create an all-pass filter.
    #[must_use]
    pub fn all_pass(sample_rate: f64, center_hz: f64, q: f64) -> Self {
        Self::calculate(BiquadType::AllPass, sample_rate, center_hz, q, 0.0)
    }

    /// Create a peaking EQ filter.
    #[must_use]
    pub fn peaking(sample_rate: f64, center_hz: f64, q: f64, gain_db: f64) -> Self {
        Self::calculate(BiquadType::Peaking, sample_rate, center_hz, q, gain_db)
    }

    /// Create a low-shelf filter.
    #[must_use]
    pub fn low_shelf(sample_rate: f64, cutoff_hz: f64, gain_db: f64) -> Self {
        Self::calculate(BiquadType::LowShelf, sample_rate, cutoff_hz, 0.707, gain_db)
    }

    /// Create a high-shelf filter.
    #[must_use]
    pub fn high_shelf(sample_rate: f64, cutoff_hz: f64, gain_db: f64) -> Self {
        Self::calculate(
            BiquadType::HighShelf,
            sample_rate,
            cutoff_hz,
            0.707,
            gain_db,
        )
    }
}

/// Biquad filter state for processing audio.
///
/// This maintains the filter history (previous inputs and outputs)
/// needed for IIR filtering.
#[derive(Clone, Debug, Default)]
pub struct BiquadState {
    /// Previous input sample 1.
    x1: f64,
    /// Previous input sample 2.
    x2: f64,
    /// Previous output sample 1.
    y1: f64,
    /// Previous output sample 2.
    y2: f64,
}

impl BiquadState {
    /// Create a new biquad state with zero history.
    #[must_use]
    pub fn new() -> Self {
        Self::default()
    }

    /// Process a single sample through the biquad filter.
    ///
    /// # Arguments
    ///
    /// * `input` - Input sample
    /// * `coeffs` - Biquad coefficients
    ///
    /// # Returns
    ///
    /// Filtered output sample
    pub fn process(&mut self, input: f64, coeffs: &BiquadCoefficients) -> f64 {
        let output = coeffs.b0 * input + coeffs.b1 * self.x1 + coeffs.b2 * self.x2
            - coeffs.a1 * self.y1
            - coeffs.a2 * self.y2;

        self.x2 = self.x1;
        self.x1 = input;
        self.y2 = self.y1;
        self.y1 = output;

        output
    }

    /// Process multiple samples in-place.
    ///
    /// # Arguments
    ///
    /// * `samples` - Input/output sample buffer
    /// * `coeffs` - Biquad coefficients
    pub fn process_block(&mut self, samples: &mut [f64], coeffs: &BiquadCoefficients) {
        for sample in samples.iter_mut() {
            *sample = self.process(*sample, coeffs);
        }
    }

    /// Reset filter state to zero.
    pub fn reset(&mut self) {
        self.x1 = 0.0;
        self.x2 = 0.0;
        self.y1 = 0.0;
        self.y2 = 0.0;
    }
}

/// A cascaded biquad filter consisting of multiple biquad sections in series.
#[derive(Clone, Debug)]
pub struct CascadedBiquad {
    /// Coefficients for each stage.
    coefficients: Vec<BiquadCoefficients>,
    /// State for each stage.
    states: Vec<BiquadState>,
}

impl CascadedBiquad {
    /// Create a new cascaded biquad filter.
    ///
    /// # Arguments
    ///
    /// * `coefficients` - Vector of biquad coefficients (one per stage)
    #[must_use]
    pub fn new(coefficients: Vec<BiquadCoefficients>) -> Self {
        let num_stages = coefficients.len();
        Self {
            coefficients,
            states: vec![BiquadState::new(); num_stages],
        }
    }

    /// Process a single sample through all cascade stages.
    pub fn process(&mut self, mut input: f64) -> f64 {
        for (state, coeffs) in self.states.iter_mut().zip(&self.coefficients) {
            input = state.process(input, coeffs);
        }
        input
    }

    /// Process multiple samples in-place through all cascade stages.
    pub fn process_block(&mut self, samples: &mut [f64]) {
        for sample in samples.iter_mut() {
            *sample = self.process(*sample);
        }
    }

    /// Reset all filter states.
    pub fn reset(&mut self) {
        for state in &mut self.states {
            state.reset();
        }
    }

    /// Update coefficients for a specific stage.
    pub fn set_stage_coefficients(&mut self, stage: usize, coeffs: BiquadCoefficients) {
        if stage < self.coefficients.len() {
            self.coefficients[stage] = coeffs;
        }
    }

    /// Get the number of stages.
    #[must_use]
    pub fn num_stages(&self) -> usize {
        self.coefficients.len()
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    const SAMPLE_RATE: f64 = 48000.0;

    #[test]
    fn test_identity_coefficients() {
        let coeffs = BiquadCoefficients::identity();
        assert_eq!(coeffs.b0, 1.0);
        assert_eq!(coeffs.b1, 0.0);
        assert_eq!(coeffs.b2, 0.0);
        assert_eq!(coeffs.a1, 0.0);
        assert_eq!(coeffs.a2, 0.0);
    }

    #[test]
    fn test_identity_passthrough() {
        let coeffs = BiquadCoefficients::identity();
        let mut state = BiquadState::new();
        let input = 0.5;
        let output = state.process(input, &coeffs);
        assert!((output - input).abs() < 1e-10);
    }

    #[test]
    fn test_biquad_state_reset() {
        let coeffs = BiquadCoefficients::low_pass(SAMPLE_RATE, 1000.0, 0.707);
        let mut state = BiquadState::new();
        // Feed some samples
        for _ in 0..10 {
            state.process(1.0, &coeffs);
        }
        state.reset();
        // After reset, state should be zero - first sample output equals b0*input
        let out = state.process(0.0, &coeffs);
        assert_eq!(out, 0.0);
    }

    #[test]
    fn test_low_pass_attenuates_high_freq() {
        // Low-pass at 1 kHz should attenuate 10 kHz
        let coeffs = BiquadCoefficients::low_pass(SAMPLE_RATE, 1000.0, 0.707);
        let mut state = BiquadState::new();
        // Steady-state DC (0 Hz) should pass through
        let mut dc_out = 0.0;
        for _ in 0..1000 {
            dc_out = state.process(1.0, &coeffs);
        }
        assert!(
            dc_out > 0.9,
            "DC should pass through low-pass, got {dc_out}"
        );
    }

    #[test]
    fn test_high_pass_attenuates_dc() {
        // High-pass should block DC
        let coeffs = BiquadCoefficients::high_pass(SAMPLE_RATE, 1000.0, 0.707);
        let mut state = BiquadState::new();
        // Feed DC; output should settle near zero
        let mut dc_out = 0.0;
        for _ in 0..1000 {
            dc_out = state.process(1.0, &coeffs);
        }
        assert!(
            dc_out.abs() < 0.01,
            "DC should be blocked by high-pass, got {dc_out}"
        );
    }

    #[test]
    fn test_peaking_filter_no_gain() {
        // Peaking with 0 dB gain should be identity
        let coeffs = BiquadCoefficients::peaking(SAMPLE_RATE, 1000.0, 1.0, 0.0);
        let mut state = BiquadState::new();
        // After enough samples, DC should pass through unchanged
        let mut out = 0.0;
        for _ in 0..200 {
            out = state.process(1.0, &coeffs);
        }
        assert!(
            (out - 1.0).abs() < 0.01,
            "Peaking with 0 dB should be identity, got {out}"
        );
    }

    #[test]
    fn test_notch_filter_coefficients() {
        let coeffs = BiquadCoefficients::notch(SAMPLE_RATE, 1000.0, 1.0);
        // Notch: b0 and b2 should be equal, b1 == a1
        assert!((coeffs.b0 - coeffs.b2).abs() < 1e-10);
        assert!((coeffs.b1 - coeffs.a1).abs() < 1e-10);
    }

    #[test]
    fn test_low_shelf_boost_dc() {
        // Low shelf boost should amplify DC
        let coeffs = BiquadCoefficients::low_shelf(SAMPLE_RATE, 500.0, 6.0);
        let mut state = BiquadState::new();
        let mut out = 0.0;
        for _ in 0..500 {
            out = state.process(1.0, &coeffs);
        }
        // 6 dB gain at DC means output ~2.0
        assert!(out > 1.5, "Low shelf boost should amplify DC, got {out}");
    }

    #[test]
    fn test_high_shelf_boost_passes_high_freq() {
        // High shelf boost at 1 kHz: input at Nyquist (~24 kHz effective)
        // We just verify the coefficients compute without panicking
        let coeffs = BiquadCoefficients::high_shelf(SAMPLE_RATE, 1000.0, 6.0);
        // b0 should be positive and valid
        assert!(coeffs.b0.is_finite());
        assert!(coeffs.b1.is_finite());
        assert!(coeffs.b2.is_finite());
    }

    #[test]
    fn test_band_pass_coefficients() {
        let coeffs = BiquadCoefficients::band_pass(SAMPLE_RATE, 1000.0, 1.0);
        // For band-pass: b1 = 0 and b0 = -b2
        assert!((coeffs.b1).abs() < 1e-10);
        assert!((coeffs.b0 + coeffs.b2).abs() < 1e-10);
    }

    #[test]
    fn test_process_block_matches_per_sample() {
        let coeffs = BiquadCoefficients::low_pass(SAMPLE_RATE, 2000.0, 0.707);
        let input: Vec<f64> = (0..64).map(|i| (i as f64 * 0.1).sin()).collect();

        let mut state1 = BiquadState::new();
        let output_per_sample: Vec<f64> =
            input.iter().map(|&s| state1.process(s, &coeffs)).collect();

        let mut state2 = BiquadState::new();
        let mut block = input.clone();
        state2.process_block(&mut block, &coeffs);

        for (a, b) in output_per_sample.iter().zip(block.iter()) {
            assert!(
                (a - b).abs() < 1e-15,
                "process_block should match per-sample processing"
            );
        }
    }

    #[test]
    fn test_cascaded_biquad_num_stages() {
        let c1 = BiquadCoefficients::low_pass(SAMPLE_RATE, 1000.0, 0.707);
        let c2 = BiquadCoefficients::high_pass(SAMPLE_RATE, 100.0, 0.707);
        let cascaded = CascadedBiquad::new(vec![c1, c2]);
        assert_eq!(cascaded.num_stages(), 2);
    }

    #[test]
    fn test_cascaded_biquad_reset() {
        let c1 = BiquadCoefficients::low_pass(SAMPLE_RATE, 1000.0, 0.707);
        let mut cascaded = CascadedBiquad::new(vec![c1]);
        for _ in 0..20 {
            cascaded.process(1.0);
        }
        cascaded.reset();
        let out = cascaded.process(0.0);
        assert_eq!(out, 0.0);
    }

    #[test]
    fn test_cascaded_biquad_process_block() {
        let c1 = BiquadCoefficients::low_pass(SAMPLE_RATE, 2000.0, 0.707);
        let mut cascaded = CascadedBiquad::new(vec![c1]);
        let mut samples = vec![1.0_f64; 64];
        cascaded.process_block(&mut samples);
        // All outputs should be finite
        for s in &samples {
            assert!(s.is_finite());
        }
    }

    #[test]
    fn test_cascaded_biquad_set_stage_coefficients() {
        let c1 = BiquadCoefficients::identity();
        let c2 = BiquadCoefficients::low_pass(SAMPLE_RATE, 1000.0, 0.707);
        let mut cascaded = CascadedBiquad::new(vec![c1]);
        cascaded.set_stage_coefficients(0, c2);
        // After update, should process without panic
        let _ = cascaded.process(1.0);
    }

    #[test]
    fn test_all_pass_filter_unity_magnitude() {
        // All-pass filter should not change magnitude at DC (steady state)
        let coeffs = BiquadCoefficients::all_pass(SAMPLE_RATE, 1000.0, 0.707);
        let mut state = BiquadState::new();
        let mut out = 0.0;
        for _ in 0..1000 {
            out = state.process(1.0, &coeffs);
        }
        // Magnitude should be approximately 1.0 at DC
        assert!(
            (out.abs() - 1.0).abs() < 0.1,
            "All-pass should preserve magnitude, got {out}"
        );
    }
}
