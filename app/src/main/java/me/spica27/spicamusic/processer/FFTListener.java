package me.spica27.spicamusic.processer;

public interface FFTListener {
  void onFFTReady(int sampleRateHz, int channelCount, float[] fft);
}
