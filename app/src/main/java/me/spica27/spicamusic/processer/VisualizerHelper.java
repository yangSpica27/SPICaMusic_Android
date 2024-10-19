package me.spica27.spicamusic.processer;

import android.os.Handler;
import android.os.Looper;
import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("ALL")
public class VisualizerHelper {
  private static VisualizerHelper helper;
  private List<OnVisualizerEnergyCallBack> onEnergyCallBacks = new ArrayList<>();
  private final FFTListener fftListener = new FFTListener() {
    Handler mainHandler = new Handler(Looper.getMainLooper());
    float[] fft;
    float energy;
    Runnable runnable = new Runnable() {
      @Override
      public void run() {
        for (int i = 0; i < onEnergyCallBacks.size(); i++) {
          if (!onEnergyCallBacks.isEmpty() && i <= onEnergyCallBacks.size() - 1) {
            onEnergyCallBacks.get(i).setWaveData(fft, energy);
          }
        }
      }
    };

    @Override
    public void onFFTReady(int sampleRateHz, int channelCount, float[] fft) {

      float energy = 0f;
      int size = Math.min(fft.length, FFTAudioProcessor.SAMPLE_SIZE);
      float[] newData = new float[size];
      for (int i = 0; i < size; i++) {

        float value = Math.max(0, fft[i]) * FFTAudioProcessor.LAGER_OFFSET;
        energy += value;
        newData[i] = value;
      }
      this.fft = newData;
      this.energy = energy;
      mainHandler.post(runnable);
    }
  };

  public static synchronized VisualizerHelper getInstance() {
    synchronized (VisualizerHelper.class) {
      if (helper == null) {
        helper = new VisualizerHelper();
      }
      return helper;
    }
  }

  public FFTListener getFftListener() {
    return fftListener;
  }

  public void addCallBack(OnVisualizerEnergyCallBack onEnergyCallBack) {
    onEnergyCallBacks.add(onEnergyCallBack);
  }

  public void removeCallBack(OnVisualizerEnergyCallBack onEnergyCallBack) {
    onEnergyCallBacks.remove(onEnergyCallBack);
  }

  public interface OnVisualizerEnergyCallBack {

    void setWaveData(float[] data, float totalEnergy);

  }
}
