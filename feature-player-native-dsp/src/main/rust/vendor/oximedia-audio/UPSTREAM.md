# OxiMedia DSP subset

Source: https://github.com/cool-japan/oximedia

Revision: `5263510374761ec8ef02dace5d26d62fb20e405b` (0.2.1).
Original author: COOLJAPAN OU (Team Kitasan). License: Apache-2.0, see LICENSE.

This is a source subset, not the complete upstream audio crate. Keeping the
audited modules local avoids compiling unused codecs and retaining duplicate
or unbounded metering paths. OxiFFT remains a locked dependency.

Copied paths under `crates/oximedia-audio/src/`:

- `dsp/biquad.rs` and `dsp/eq.rs`: original filter implementations.
- `loudness/filter.rs`: K-weighting cascade. Non-48 kHz coefficient generation
  is calibrated against libebur128 before release.
- `spectrum/fft.rs`: replaced the allocating convenience API with a persistent
  plan and work buffers, retaining upstream windowing and FFT conventions.

SPICa additions:

- `loudness/momentary.rs`: bounded 400 ms meter using upstream K-weighting and
  BS.1770 weighted power / LUFS conversion. Explicit channel weights replace
  the upstream channel-count guess. No planar double K-weighting, true-peak
  work, integrated history, or allocation in the streaming path.

Application gain smoothing, PCM/JNI adaptation and threading live outside this
crate. Codec decoding continues to use the existing Media3 FFmpeg extension.
