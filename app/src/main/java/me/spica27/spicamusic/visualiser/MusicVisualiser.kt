package me.spica27.spicamusic.visualiser

import android.view.animation.DecelerateInterpolator
import androidx.media3.common.util.UnstableApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.koin.java.KoinJavaComponent.getKoin
import java.util.concurrent.locks.ReentrantLock
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sqrt

// https://github.com/dzolnai/ExoVisualizer


@UnstableApi
class MusicVisualiser() : FFTAudioProcessor.FFTListener {

  companion object {
    // Taken from: https://en.wikipedia.org/wiki/Preferred_number#Audio_frequencies
    val FREQUENCY_BAND_LIMITS = arrayOf(
      20, 25, 32, 40, 50, 63, 80, 100, 125, 160, 200, 250, 315, 400, 500, 630,
      800, 1000, 1250, 1600, 2000, 2500, 3150, 4000, 5000, 6300, 8000, 10000,
      12500, 16000, 20000
    )
  }

  private val max_const = 14_000 // Reference max value for accum magnitude
  private val bands = FREQUENCY_BAND_LIMITS.size
  private val size = FFTAudioProcessor.SAMPLE_SIZE

  // We average out the values over 3 occurences (plus the current one), so big jumps are smoothed out
  private val smoothing_factor = 2
  private val previous_values = FloatArray(bands * smoothing_factor)

  private val fft2: FloatArray = FloatArray(size)

  private var job = SupervisorJob()

  private var coroutineScope = CoroutineScope(job + Dispatchers.Default)

  private val fftAudioProcessor = getKoin().get<FFTAudioProcessor>()

  fun ready() {
    job = SupervisorJob()
    coroutineScope = CoroutineScope(job + Dispatchers.Default)
    fftAudioProcessor.listeners.add(this)
  }

  fun dispose() {
    fftAudioProcessor.listeners.remove(this)
    job.cancel()
  }

  private var listener: Listener? = null

  fun setListener(listener: Listener?) {
    this.listener = listener
  }

  interface Listener {
    fun getDrawData(list: List<Float>)
  }

  private val res = mutableListOf<Float>()


  private val writeLock = ReentrantLock()

  private var lastTime = System.currentTimeMillis()

  override fun onFFTReady(sampleRateHz: Int, channelCount: Int, fft: FloatArray) {
    val currentTime = System.currentTimeMillis()
    if (currentTime - lastTime < 65) {
      return
    }
    lastTime = currentTime
    if (listener == null) return
    if (writeLock.isLocked) return
    fft.copyInto(fft2)
    coroutineScope.launch {
      writeLock.lock()
      synchronized(fft2) {
        // Set up counters and widgets
        res.clear()
        var currentFftPosition = 0
        var currentFrequencyBandLimitIndex = 0

        // Iterate over the entire FFT result array
        while (currentFftPosition < this@MusicVisualiser.size) {
          var accum = 0f

          // We divide the bands by frequency.
          // Check until which index we need to stop for the current band
          val nextLimitAtPosition =
            floor(FREQUENCY_BAND_LIMITS[currentFrequencyBandLimitIndex] / 20_000.toFloat() * this@MusicVisualiser.size).toInt()

          synchronized(fft) {
            // Here we iterate within this single band
            for (j in 0 until (nextLimitAtPosition - currentFftPosition) step 2) {
              // Convert real and imaginary part to get energy
              val raw = (fft[currentFftPosition + j].toDouble().pow(2.0) +
                  fft[currentFftPosition + j + 1].toDouble().pow(2.0)).toFloat()

              // Hamming window (by frequency band instead of frequency, otherwise it would prefer 10kHz, which is too high)
              // The window mutes down the very high and the very low frequencies, usually not hearable by the human ear
              val m = bands / 2
              val windowed =
                raw * (0.54f - 0.46f * cos(2 * Math.PI * currentFrequencyBandLimitIndex / (m + 1))).toFloat()
              accum += windowed
            }
          }

          // A window might be empty which would result in a 0 division
          if (nextLimitAtPosition - currentFftPosition != 0) {
            accum /= (nextLimitAtPosition - currentFftPosition)
          } else {
            accum = 0.0f
          }
          currentFftPosition = nextLimitAtPosition

          // Here we do the smoothing
          // If you increase the smoothing factor, the high shoots will be toned down, but the
          // 'movement' in general will decrease too
          var smoothedAccum = accum
          for (index in 0 until smoothing_factor) {
            smoothedAccum += previous_values[index * bands + currentFrequencyBandLimitIndex]
            if (index != smoothing_factor - 1) {
              previous_values[index * bands + currentFrequencyBandLimitIndex] =
                previous_values[(index + 1) * bands + currentFrequencyBandLimitIndex]
            } else {
              previous_values[index * bands + currentFrequencyBandLimitIndex] = accum
            }
          }
          smoothedAccum /= (smoothing_factor + 1) // +1 because it also includes the current value

          val value = (smoothedAccum / max_const.toDouble()).toFloat().coerceIn(0f, 1f)

          // 添加增益
          res.add(decelerateInterpolator.getInterpolation(value))

          currentFrequencyBandLimitIndex++
        }
        listener?.getDrawData(res)
      }
      writeLock.unlock()
    }
  }


  private fun getLoudnessAtFrequency(
    fftResult: FloatArray, // 已执行 realForward(src) 后的 src 数组
    targetFrequency: Float,
    N: Int, // FFT size, 即 SAMPLE_SIZE
    sampleRate: Float
  ): Float {
    // 1. 确定目标频率对应的频率区间索引 (Bin Index)
    val kExact = targetFrequency * N / sampleRate
    val k = kExact.roundToInt()

    // 检查 k 是否在有效范围内 (0 到 N/2)
    if (k < 0 || k > N / 2) {
      return 0.0f // 或抛出异常
    }

    val actualFrequency = k * (sampleRate / N)

    // 2. 从 realForward 的输出中提取该频率区间的实部和虚部
    //    采用 JTransforms 的这种打包方式:
    //    a[0] is Re[0]
    //    If n is even, a[n/2] is Re[n/2]
    //    For 0 < i < n/2, a[i] is Re[i] and a[n-i] is Im[i].
    val real: Float
    val imag: Float

    when (k) {
      0 -> { // DC component
        real = fftResult[0]
        imag = 0.0f
      }

      N / 2 -> { // Nyquist frequency (assuming N is even)
        if (N % 2 == 0) {
          real = fftResult[N / 2]
          imag = 0.0f
        } else {
          return 0.0f
        }
      }

      else -> { // Other frequencies (0 < k < N/2)
        if (k >= fftResult.size || (N - k) >= fftResult.size) {
          return 0.0f
        }
        real = fftResult[k]
        imag = fftResult[N - k]
      }
    }

    // 3. 计算幅度 (Amplitude)
    val amplitude = sqrt(real.pow(2) + imag.pow(2))

    return amplitude
  }

  private val decelerateInterpolator = DecelerateInterpolator(3f)

}
