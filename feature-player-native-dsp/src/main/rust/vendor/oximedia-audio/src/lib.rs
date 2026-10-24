// Copyright 2024-2026 COOLJAPAN OU (Team Kitasan).
// SPDX-License-Identifier: Apache-2.0
// Modified for SPICa Music: compile only the audited, standalone DSP modules.
#![forbid(unsafe_code)]

pub mod dsp {
    pub mod biquad;
    pub mod eq;
}
pub mod loudness {
    pub mod filter;
    pub mod momentary;
}
pub mod spectrum {
    pub mod fft;
}
