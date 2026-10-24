// Copyright 2024-2026 COOLJAPAN OU (Team Kitasan).
// SPDX-License-Identifier: Apache-2.0
// Derived from spectrum/fft.rs at 5263510374761ec8ef02dace5d26d62fb20e405b.
// Modified for SPICa Music: fixed Hamming window, persistent plan and buffers.
use oxifft::api::{Direction, Flags, Plan};
use oxifft::Complex;
use std::f64::consts::PI;

pub struct FftProcessor {
    plan: Plan<f64>,
    window: Vec<f64>,
    input: Vec<Complex<f64>>,
    output: Vec<Complex<f64>>,
}

impl FftProcessor {
    pub fn new(size: usize) -> Option<Self> {
        if size < 2 || !size.is_power_of_two() {
            return None;
        }
        Some(Self {
            plan: Plan::dft_1d(size, Direction::Forward, Flags::ESTIMATE)?,
            window: (0..size)
                .map(|i| 0.54 - 0.46 * (2.0 * PI * i as f64 / (size - 1) as f64).cos())
                .collect(),
            input: vec![Complex::zero(); size],
            output: vec![Complex::zero(); size],
        })
    }
    pub fn process(&mut self, samples: &[f32]) -> &[Complex<f64>] {
        for (i, value) in self.input.iter_mut().enumerate() {
            *value = Complex::new(
                samples.get(i).copied().unwrap_or(0.0) as f64 * self.window[i],
                0.0,
            );
        }
        self.plan.execute(&self.input, &mut self.output);
        &self.output
    }
}

#[cfg(test)]
mod tests {
    use super::*;
    #[test]
    fn detects_bin_and_reuses_plan() {
        let samples: Vec<f32> = (0..4096)
            .map(|i| (2.0 * PI * 128.0 * i as f64 / 4096.0).sin() as f32)
            .collect();
        let mut fft = FftProcessor::new(4096).unwrap();
        for _ in 0..3 {
            let output = fft.process(&samples);
            let bin = (1..2048)
                .max_by(|a, b| output[*a].norm_sqr().total_cmp(&output[*b].norm_sqr()))
                .unwrap();
            assert_eq!(bin, 128);
            assert!((output[bin].norm() - 1105.69).abs() < 1.0);
        }
    }
}
