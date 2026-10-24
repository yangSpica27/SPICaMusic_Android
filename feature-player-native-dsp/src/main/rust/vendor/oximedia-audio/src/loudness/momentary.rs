// Copyright 2024-2026 COOLJAPAN OU (Team Kitasan).
// SPDX-License-Identifier: Apache-2.0
// Derived from loudness/{r128,gate,filter}.rs at 5263510.
// Modified: bounded per-frame power ring, explicit channel weights, K-weighting
// exactly once, no integrated history or peak work in the streaming path.
use super::filter::KWeightFilter;

pub struct MomentaryMeter {
    filters: Vec<KWeightFilter>,
    weights: Vec<f64>,
    powers: Vec<f64>,
    cursor: usize,
    frames_seen: usize,
    sum: f64,
}
impl MomentaryMeter {
    pub fn new(sample_rate: u32, weights: &[f64]) -> Self {
        Self {
            filters: weights
                .iter()
                .map(|_| KWeightFilter::new(sample_rate as f64))
                .collect(),
            weights: weights.to_vec(),
            powers: vec![0.0; (sample_rate as usize * 4 / 10).max(1)],
            cursor: 0,
            frames_seen: 0,
            sum: 0.0,
        }
    }
    pub fn process_frame(&mut self, samples: &[f64]) {
        let mut power = 0.0;
        for ((filter, weight), sample) in self.filters.iter_mut().zip(&self.weights).zip(samples) {
            let filtered = filter.process(*sample);
            power += weight * filtered * filtered;
        }
        self.sum += power - self.powers[self.cursor];
        self.powers[self.cursor] = power;
        self.cursor = (self.cursor + 1) % self.powers.len();
        self.frames_seen = (self.frames_seen + 1).min(self.powers.len());
    }
    pub fn loudness(&self) -> f64 {
        if self.frames_seen < self.powers.len() || self.sum <= 1e-20 {
            f64::NEG_INFINITY
        } else {
            -0.691 + 10.0 * (self.sum.max(0.0) / self.powers.len() as f64).log10()
        }
    }
    pub fn reset(&mut self) {
        self.filters.iter_mut().for_each(KWeightFilter::reset);
        self.powers.fill(0.0);
        self.cursor = 0;
        self.frames_seen = 0;
        self.sum = 0.0;
    }
}
