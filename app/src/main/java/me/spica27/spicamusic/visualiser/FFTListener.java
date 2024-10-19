package me.spica27.spicamusic.visualiser;

public interface FFTListener {
  void onFFTReady(int sampleRateHz, int channelCount, float[] fft);
}
