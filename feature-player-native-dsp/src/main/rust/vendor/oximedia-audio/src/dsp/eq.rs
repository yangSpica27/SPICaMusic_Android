// Copyright 2024-2026 COOLJAPAN OU (Team Kitasan).
// SPDX-License-Identifier: Apache-2.0
// Imported from OxiMedia 5263510; modified only for standalone packaging/formatting.
//! Parametric equalizer DSP implementation.
//!
//! This module provides a multi-band parametric equalizer using biquad filters.

#![forbid(unsafe_code)]
#![allow(clippy::cast_lossless)]

use super::biquad::{BiquadCoefficients, BiquadState, BiquadType};

/// Maximum number of EQ bands.
pub const MAX_EQ_BANDS: usize = 32;

/// Configuration for a single EQ band.
#[derive(Clone, Debug)]
pub struct EqBand {
    /// Type of filter for this band.
    pub filter_type: BiquadType,
    /// Center frequency in Hz.
    pub frequency: f64,
    /// Gain in dB (for peaking and shelf filters).
    pub gain_db: f64,
    /// Q factor (bandwidth control).
    pub q: f64,
    /// Whether this band is enabled.
    pub enabled: bool,
}

impl EqBand {
    /// Create a new EQ band.
    #[must_use]
    pub fn new(filter_type: BiquadType, frequency: f64, gain_db: f64, q: f64) -> Self {
        Self {
            filter_type,
            frequency,
            gain_db,
            q,
            enabled: true,
        }
    }

    /// Create a low-shelf band.
    #[must_use]
    pub fn low_shelf(frequency: f64, gain_db: f64) -> Self {
        Self::new(BiquadType::LowShelf, frequency, gain_db, 0.707)
    }

    /// Create a high-shelf band.
    #[must_use]
    pub fn high_shelf(frequency: f64, gain_db: f64) -> Self {
        Self::new(BiquadType::HighShelf, frequency, gain_db, 0.707)
    }

    /// Create a peaking band.
    #[must_use]
    pub fn peaking(frequency: f64, gain_db: f64, q: f64) -> Self {
        Self::new(BiquadType::Peaking, frequency, gain_db, q)
    }

    /// Create a low-pass band.
    #[must_use]
    pub fn low_pass(frequency: f64, q: f64) -> Self {
        Self::new(BiquadType::LowPass, frequency, 0.0, q)
    }

    /// Create a high-pass band.
    #[must_use]
    pub fn high_pass(frequency: f64, q: f64) -> Self {
        Self::new(BiquadType::HighPass, frequency, 0.0, q)
    }

    /// Create a band-pass band.
    #[must_use]
    pub fn band_pass(frequency: f64, q: f64) -> Self {
        Self::new(BiquadType::BandPass, frequency, 0.0, q)
    }

    /// Create a notch filter band.
    #[must_use]
    pub fn notch(frequency: f64, q: f64) -> Self {
        Self::new(BiquadType::Notch, frequency, 0.0, q)
    }

    /// Set whether this band is enabled.
    #[must_use]
    pub fn with_enabled(mut self, enabled: bool) -> Self {
        self.enabled = enabled;
        self
    }
}

impl Default for EqBand {
    fn default() -> Self {
        Self {
            filter_type: BiquadType::Peaking,
            frequency: 1000.0,
            gain_db: 0.0,
            q: 1.0,
            enabled: true,
        }
    }
}

/// Configuration for the parametric equalizer.
#[derive(Clone, Debug, Default)]
pub struct EqualizerConfig {
    /// EQ bands.
    pub bands: Vec<EqBand>,
}

impl EqualizerConfig {
    /// Create a new empty equalizer configuration.
    #[must_use]
    pub fn new() -> Self {
        Self { bands: Vec::new() }
    }

    /// Add a band to the equalizer.
    #[must_use]
    pub fn add_band(mut self, band: EqBand) -> Self {
        if self.bands.len() < MAX_EQ_BANDS {
            self.bands.push(band);
        }
        self
    }

    /// Create a 3-band EQ preset (low shelf, mid peaking, high shelf).
    #[must_use]
    pub fn three_band(low_gain_db: f64, mid_gain_db: f64, high_gain_db: f64) -> Self {
        Self::new()
            .add_band(EqBand::low_shelf(250.0, low_gain_db))
            .add_band(EqBand::peaking(1000.0, mid_gain_db, 1.0))
            .add_band(EqBand::high_shelf(4000.0, high_gain_db))
    }

    /// Create a 5-band EQ preset.
    #[must_use]
    pub fn five_band(
        low_gain_db: f64,
        low_mid_gain_db: f64,
        mid_gain_db: f64,
        high_mid_gain_db: f64,
        high_gain_db: f64,
    ) -> Self {
        Self::new()
            .add_band(EqBand::low_shelf(100.0, low_gain_db))
            .add_band(EqBand::peaking(250.0, low_mid_gain_db, 1.0))
            .add_band(EqBand::peaking(1000.0, mid_gain_db, 1.0))
            .add_band(EqBand::peaking(4000.0, high_mid_gain_db, 1.0))
            .add_band(EqBand::high_shelf(8000.0, high_gain_db))
    }

    /// Create a 10-band graphic EQ preset.
    #[must_use]
    #[allow(clippy::too_many_arguments)]
    pub fn graphic_10_band(
        g31: f64,
        g62: f64,
        g125: f64,
        g250: f64,
        g500: f64,
        g1k: f64,
        g2k: f64,
        g4k: f64,
        g8k: f64,
        g16k: f64,
    ) -> Self {
        let q = 1.414; // ~1 octave bandwidth
        Self::new()
            .add_band(EqBand::peaking(31.0, g31, q))
            .add_band(EqBand::peaking(62.0, g62, q))
            .add_band(EqBand::peaking(125.0, g125, q))
            .add_band(EqBand::peaking(250.0, g250, q))
            .add_band(EqBand::peaking(500.0, g500, q))
            .add_band(EqBand::peaking(1000.0, g1k, q))
            .add_band(EqBand::peaking(2000.0, g2k, q))
            .add_band(EqBand::peaking(4000.0, g4k, q))
            .add_band(EqBand::peaking(8000.0, g8k, q))
            .add_band(EqBand::peaking(16000.0, g16k, q))
    }
}

/// Parametric equalizer processor.
///
/// Processes audio through multiple biquad filter bands in series.
pub struct Equalizer {
    /// Filter coefficients for each band.
    coefficients: Vec<BiquadCoefficients>,
    /// Filter states for each band (per channel).
    states: Vec<Vec<BiquadState>>,
    /// Current configuration.
    config: EqualizerConfig,
    /// Sample rate.
    sample_rate: f64,
    /// Number of channels.
    channels: usize,
}

impl Equalizer {
    /// Create a new equalizer.
    ///
    /// # Arguments
    ///
    /// * `config` - Equalizer configuration
    /// * `sample_rate` - Sample rate in Hz
    /// * `channels` - Number of audio channels
    #[must_use]
    pub fn new(config: EqualizerConfig, sample_rate: f64, channels: usize) -> Self {
        let mut eq = Self {
            coefficients: Vec::new(),
            states: vec![Vec::new(); channels],
            config: config.clone(),
            sample_rate,
            channels,
        };
        eq.update_coefficients(&config);
        eq
    }

    /// Update coefficients based on configuration.
    fn update_coefficients(&mut self, config: &EqualizerConfig) {
        self.coefficients.clear();

        for band in &config.bands {
            let coeffs = BiquadCoefficients::calculate(
                band.filter_type,
                self.sample_rate,
                band.frequency,
                band.q,
                band.gain_db,
            );
            self.coefficients.push(coeffs);
        }

        // Resize state arrays
        for channel_states in &mut self.states {
            channel_states.resize(self.coefficients.len(), BiquadState::new());
        }
    }

    /// Set the equalizer configuration.
    pub fn set_config(&mut self, config: EqualizerConfig) {
        self.config = config.clone();
        self.update_coefficients(&config);
    }

    /// Get the current configuration.
    #[must_use]
    pub fn config(&self) -> &EqualizerConfig {
        &self.config
    }

    /// Process a single channel of samples.
    ///
    /// # Arguments
    ///
    /// * `channel` - Channel index
    /// * `samples` - Input/output sample buffer
    pub fn process_channel(&mut self, channel: usize, samples: &mut [f64]) {
        if channel >= self.channels {
            return;
        }

        for sample in samples.iter_mut() {
            let mut value = *sample;

            for (band_idx, band) in self.config.bands.iter().enumerate() {
                if !band.enabled {
                    continue;
                }

                if band_idx < self.coefficients.len() && band_idx < self.states[channel].len() {
                    value =
                        self.states[channel][band_idx].process(value, &self.coefficients[band_idx]);
                }
            }

            *sample = value;
        }
    }

    /// Process multiple channels (interleaved samples).
    ///
    /// # Arguments
    ///
    /// * `samples` - Interleaved input/output sample buffer
    /// * `num_samples` - Number of samples per channel
    pub fn process_interleaved(&mut self, samples: &mut [f64], num_samples: usize) {
        for i in 0..num_samples {
            for ch in 0..self.channels {
                let idx = i * self.channels + ch;
                if idx >= samples.len() {
                    break;
                }

                let mut value = samples[idx];

                for (band_idx, band) in self.config.bands.iter().enumerate() {
                    if !band.enabled {
                        continue;
                    }

                    if band_idx < self.coefficients.len()
                        && ch < self.states.len()
                        && band_idx < self.states[ch].len()
                    {
                        value =
                            self.states[ch][band_idx].process(value, &self.coefficients[band_idx]);
                    }
                }

                samples[idx] = value;
            }
        }
    }

    /// Process multiple channels (planar samples).
    ///
    /// # Arguments
    ///
    /// * `channels` - Slice of channel buffers
    pub fn process_planar(&mut self, channels: &mut [Vec<f64>]) {
        for (ch, channel_samples) in channels.iter_mut().enumerate() {
            self.process_channel(ch, channel_samples);
        }
    }

    /// Reset all filter states.
    pub fn reset(&mut self) {
        for channel_states in &mut self.states {
            for state in channel_states {
                state.reset();
            }
        }
    }

    /// Update a specific band's parameters.
    pub fn update_band(&mut self, band_index: usize, band: EqBand) {
        if band_index < self.config.bands.len() {
            self.config.bands[band_index] = band;

            if band_index < self.coefficients.len() {
                let coeffs = BiquadCoefficients::calculate(
                    self.config.bands[band_index].filter_type,
                    self.sample_rate,
                    self.config.bands[band_index].frequency,
                    self.config.bands[band_index].q,
                    self.config.bands[band_index].gain_db,
                );
                self.coefficients[band_index] = coeffs;
            }
        }
    }

    /// Enable or disable a specific band.
    pub fn set_band_enabled(&mut self, band_index: usize, enabled: bool) {
        if band_index < self.config.bands.len() {
            self.config.bands[band_index].enabled = enabled;
        }
    }
}

#[cfg(test)]
mod tests {
    use super::*;
    use crate::dsp::biquad::BiquadType;

    const SAMPLE_RATE: f64 = 48000.0;

    #[test]
    fn test_eq_band_default() {
        let band = EqBand::default();
        assert_eq!(band.filter_type, BiquadType::Peaking);
        assert_eq!(band.frequency, 1000.0);
        assert_eq!(band.gain_db, 0.0);
        assert!(band.enabled);
    }

    #[test]
    fn test_eq_band_low_shelf() {
        let band = EqBand::low_shelf(200.0, 3.0);
        assert_eq!(band.filter_type, BiquadType::LowShelf);
        assert_eq!(band.frequency, 200.0);
        assert_eq!(band.gain_db, 3.0);
    }

    #[test]
    fn test_eq_band_high_shelf() {
        let band = EqBand::high_shelf(8000.0, -3.0);
        assert_eq!(band.filter_type, BiquadType::HighShelf);
        assert_eq!(band.frequency, 8000.0);
        assert_eq!(band.gain_db, -3.0);
    }

    #[test]
    fn test_eq_band_peaking() {
        let band = EqBand::peaking(1000.0, 6.0, 1.5);
        assert_eq!(band.filter_type, BiquadType::Peaking);
        assert_eq!(band.q, 1.5);
        assert_eq!(band.gain_db, 6.0);
    }

    #[test]
    fn test_eq_band_notch() {
        let band = EqBand::notch(500.0, 2.0);
        assert_eq!(band.filter_type, BiquadType::Notch);
        assert_eq!(band.frequency, 500.0);
    }

    #[test]
    fn test_eq_band_enabled_toggle() {
        let band = EqBand::peaking(1000.0, 3.0, 1.0).with_enabled(false);
        assert!(!band.enabled);
        let band_on = band.with_enabled(true);
        assert!(band_on.enabled);
    }

    #[test]
    fn test_equalizer_config_new() {
        let config = EqualizerConfig::new();
        assert!(config.bands.is_empty());
    }

    #[test]
    fn test_equalizer_config_add_band() {
        let config = EqualizerConfig::new()
            .add_band(EqBand::peaking(1000.0, 3.0, 1.0))
            .add_band(EqBand::high_shelf(8000.0, -2.0));
        assert_eq!(config.bands.len(), 2);
    }

    #[test]
    fn test_equalizer_config_three_band() {
        let config = EqualizerConfig::three_band(3.0, 0.0, -2.0);
        assert_eq!(config.bands.len(), 3);
    }

    #[test]
    fn test_equalizer_config_five_band() {
        let config = EqualizerConfig::five_band(1.0, -1.0, 2.0, -2.0, 3.0);
        assert_eq!(config.bands.len(), 5);
    }

    #[test]
    fn test_equalizer_config_ten_band() {
        let config =
            EqualizerConfig::graphic_10_band(0.0, 1.0, 2.0, 3.0, -1.0, -2.0, -3.0, 0.0, 1.0, 2.0);
        assert_eq!(config.bands.len(), 10);
    }

    #[test]
    fn test_equalizer_new_and_passthrough() {
        // An EQ with zero-gain peaking filter should pass DC through roughly
        let config = EqualizerConfig::new().add_band(EqBand::peaking(1000.0, 0.0, 1.0));
        let mut eq = Equalizer::new(config, SAMPLE_RATE, 1);
        let mut samples = vec![1.0_f64; 200];
        eq.process_channel(0, &mut samples);
        // After settling, should be near 1.0
        assert!((samples[199] - 1.0).abs() < 0.05);
    }

    #[test]
    fn test_equalizer_process_interleaved() {
        let config = EqualizerConfig::three_band(0.0, 0.0, 0.0);
        let mut eq = Equalizer::new(config, SAMPLE_RATE, 2);
        let mut samples = vec![0.5_f64; 40]; // 20 frames, 2 channels
        eq.process_interleaved(&mut samples, 20);
        for s in &samples {
            assert!(s.is_finite());
        }
    }

    #[test]
    fn test_equalizer_process_planar() {
        let config = EqualizerConfig::five_band(1.0, -1.0, 0.0, 1.0, -1.0);
        let mut eq = Equalizer::new(config, SAMPLE_RATE, 2);
        let mut channels = vec![vec![0.3_f64; 256]; 2];
        eq.process_planar(&mut channels);
        for ch in &channels {
            for s in ch {
                assert!(s.is_finite());
            }
        }
    }

    #[test]
    fn test_equalizer_reset() {
        let config = EqualizerConfig::three_band(3.0, 0.0, -3.0);
        let mut eq = Equalizer::new(config, SAMPLE_RATE, 1);
        let mut samples = vec![1.0_f64; 100];
        eq.process_channel(0, &mut samples);
        eq.reset();
        // After reset, re-process should produce same output as fresh start
        let mut samples2 = vec![1.0_f64; 100];
        let config2 = EqualizerConfig::three_band(3.0, 0.0, -3.0);
        let mut eq2 = Equalizer::new(config2, SAMPLE_RATE, 1);
        eq2.process_channel(0, &mut samples2);
        let mut samples3 = vec![1.0_f64; 100];
        eq.process_channel(0, &mut samples3);
        assert!((samples2[99] - samples3[99]).abs() < 1e-10);
    }

    #[test]
    fn test_equalizer_update_band() {
        let config = EqualizerConfig::new().add_band(EqBand::peaking(1000.0, 0.0, 1.0));
        let mut eq = Equalizer::new(config, SAMPLE_RATE, 1);
        eq.update_band(0, EqBand::peaking(2000.0, 6.0, 1.0));
        assert_eq!(eq.config().bands[0].frequency, 2000.0);
        assert_eq!(eq.config().bands[0].gain_db, 6.0);
    }

    #[test]
    fn test_equalizer_set_band_enabled() {
        let config = EqualizerConfig::new().add_band(EqBand::peaking(1000.0, 12.0, 1.0));
        let mut eq = Equalizer::new(config, SAMPLE_RATE, 1);
        eq.set_band_enabled(0, false);
        assert!(!eq.config().bands[0].enabled);
        // Disabled band: output should not be modified
        let mut s = vec![1.0_f64; 100];
        eq.process_channel(0, &mut s);
        // With disabled band, output should be close to input
        assert!((s[99] - 1.0).abs() < 1e-10);
    }

    #[test]
    fn test_equalizer_max_bands_limit() {
        let mut config = EqualizerConfig::new();
        for _ in 0..MAX_EQ_BANDS + 5 {
            config = config.add_band(EqBand::peaking(1000.0, 0.0, 1.0));
        }
        assert_eq!(config.bands.len(), MAX_EQ_BANDS);
    }

    #[test]
    fn test_equalizer_set_config() {
        let config1 = EqualizerConfig::three_band(3.0, 0.0, -3.0);
        let mut eq = Equalizer::new(config1, SAMPLE_RATE, 1);
        let config2 = EqualizerConfig::five_band(0.0, 0.0, 0.0, 0.0, 0.0);
        eq.set_config(config2);
        assert_eq!(eq.config().bands.len(), 5);
    }
}
