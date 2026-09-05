# Native DSP

`NativeDspEngine` is the format-preserving native audio path used by the
player on `arm64-v8a`:

```
Media3 AudioProcessor (DirectByteBuffer)
  -> JNI
  -> PCM decode (8/16/24/32-bit LE/BE and float)
  -> PFFFT analyzer (4096 samples, asynchronous)
  -> DSPFilters RBJ EQ (10 bands)
  -> libebur128 measurement + native gain/limiter
  -> encode to the original PCM format
```

For 88.2/96/176.4/192 kHz input the analyzer performs analysis-only
decimation to a rate at or below 48 kHz, while the negotiated PCM stream is
passed through at its original rate and encoding. FFT bin mapping uses that
effective analysis rate, so Hi-Res tones remain at their actual frequencies.

The negotiated `AudioFormat` is returned unchanged. If the native library is
unavailable or a block cannot be processed, the adapter returns
`AudioFormat.NOT_SET` or copies the original block so Media3 bypasses
processing without interrupting playback.

The native path supports up to 16 interleaved channels. Unsupported channel
layouts or encodings return `AudioProcessor.AudioFormat.NOT_SET`, allowing
Media3 to bypass the processor and preserve the original stream unchanged.

Third-party sources are vendored under `src/main/cpp/third_party`:

- PFFFT 1.1.0 (permissive BSD-style license),
- DSPFilters (MIT),
- libebur128 1.2.6 (MIT).

See each upstream license file next to its source. Spatial effects are
intentionally outside this first native rollout and can be added later as a
separate processor without changing the PCM/JNI boundary.
