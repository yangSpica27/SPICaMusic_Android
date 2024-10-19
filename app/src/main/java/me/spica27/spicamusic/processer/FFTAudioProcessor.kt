package me.spica27.spicamusic.processer

import android.media.AudioTrack
import com.google.android.exoplayer2.C
import com.google.android.exoplayer2.Format
import com.google.android.exoplayer2.audio.AudioProcessor
import com.google.android.exoplayer2.util.Assertions
import com.google.android.exoplayer2.util.Util
import com.paramsen.noise.Noise
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.max

/**
 * 通过傅里叶将音频信息转化为
 * 振幅数组用于ui更新绘制对应的动效
 */
class FFTAudioProcessor : AudioProcessor {
  companion object {
    const val SAMPLE_SIZE = 4096

    // From DefaultAudioSink.java:160 'MIN_BUFFER_DURATION_US'
    private const val EXO_MIN_BUFFER_DURATION_US: Long = 250000

    // From DefaultAudioSink.java:164 'MAX_BUFFER_DURATION_US'
    private const val EXO_MAX_BUFFER_DURATION_US: Long = 750000

    // From DefaultAudioSink.java:173 'BUFFER_MULTIPLICATION_FACTOR'
    private const val EXO_BUFFER_MULTIPLICATION_FACTOR = 4

    // Extra size next in addition to the AudioTrack buffer size
    private const val BUFFER_EXTRA_SIZE = SAMPLE_SIZE * 8
  }

  private var noise: Noise = Noise.real(SAMPLE_SIZE)

  private var isActive: Boolean = false

  private var processBuffer: ByteBuffer
  private var fftBuffer: ByteBuffer
  private var outputBuffer: ByteBuffer

  var listener: FFTListener? = null
  private var inputEnded: Boolean = false

  private lateinit var srcBuffer: ByteBuffer
  private var srcBufferPosition = 0
  private val tempByteArray = ByteArray(SAMPLE_SIZE * 2)

  private var audioTrackBufferSize = 0

  private val src = FloatArray(SAMPLE_SIZE)
  private val dst = FloatArray(SAMPLE_SIZE + 2)


  interface FFTListener {
    fun onFFTReady(sampleRateHz: Int, channelCount: Int, fft: FloatArray)
  }

  init {
    processBuffer = AudioProcessor.EMPTY_BUFFER
    fftBuffer = AudioProcessor.EMPTY_BUFFER
    outputBuffer = AudioProcessor.EMPTY_BUFFER
  }

  /**
   * 创建一个和Exoplayer相同大小的缓冲区
   * 进行处理 来匹配真实的音频输出
   * 因为 processor 会提前获取到exoplayer的播放信息
   */
  private fun getDefaultBufferSizeInBytes(audioFormat: AudioProcessor.AudioFormat): Int {
    val outputPcmFrameSize = Util.getPcmFrameSize(audioFormat.encoding, audioFormat.channelCount)
    val minBufferSize =
      AudioTrack.getMinBufferSize(
        audioFormat.sampleRate,
        Util.getAudioTrackChannelConfig(audioFormat.channelCount),
        audioFormat.encoding
      )
    Assertions.checkState(minBufferSize != AudioTrack.ERROR_BAD_VALUE)
    val multipliedBufferSize = minBufferSize * EXO_BUFFER_MULTIPLICATION_FACTOR
    val minAppBufferSize =
      durationUsToFrames(EXO_MIN_BUFFER_DURATION_US).toInt() * outputPcmFrameSize
    val maxAppBufferSize = max(
      minBufferSize.toLong(),
      durationUsToFrames(EXO_MAX_BUFFER_DURATION_US) * outputPcmFrameSize
    ).toInt()
    val bufferSizeInFrames = Util.constrainValue(
      multipliedBufferSize,
      minAppBufferSize,
      maxAppBufferSize
    ) / outputPcmFrameSize
    return bufferSizeInFrames * outputPcmFrameSize
  }

  private fun durationUsToFrames(durationUs: Long): Long {
    return durationUs * inputAudioFormat.sampleRate / C.MICROS_PER_SECOND
  }

  override fun isActive(): Boolean {
    return isActive
  }

  private lateinit var inputAudioFormat: AudioProcessor.AudioFormat

  override fun configure(inputAudioFormat: AudioProcessor.AudioFormat): AudioProcessor.AudioFormat {
    if (inputAudioFormat.encoding != C.ENCODING_PCM_16BIT) {
      throw AudioProcessor.UnhandledAudioFormatException(
        inputAudioFormat
      )
    }
    this.inputAudioFormat = inputAudioFormat
    isActive = true
    noise = Noise.real(SAMPLE_SIZE)
    audioTrackBufferSize = getDefaultBufferSizeInBytes(inputAudioFormat)
    srcBuffer = ByteBuffer.allocate(audioTrackBufferSize + BUFFER_EXTRA_SIZE)
    return inputAudioFormat
  }

  override fun queueInput(inputBuffer: ByteBuffer) {
    var position = inputBuffer.position()
    val limit = inputBuffer.limit()
    val frameCount = (limit - position) / (2 * inputAudioFormat.channelCount)
    val singleChannelOutputSize = frameCount * 2
    val outputSize = frameCount * inputAudioFormat.channelCount * 2
    if (processBuffer.capacity() < outputSize) {
      processBuffer = ByteBuffer.allocateDirect(outputSize).order(ByteOrder.nativeOrder())
    } else {
      processBuffer.clear()
    }
    if (fftBuffer.capacity() < singleChannelOutputSize) {
      fftBuffer =
        ByteBuffer.allocateDirect(singleChannelOutputSize).order(ByteOrder.nativeOrder())
    } else {
      fftBuffer.clear()
    }
    while (position < limit) {
      var summedUp = 0
      for (channelIndex in 0 until inputAudioFormat.channelCount) {
        val current = inputBuffer.getShort(position + 2 * channelIndex)
        processBuffer.putShort(current)
        summedUp += current
      }
      // 使用所有通道的 currentAverage
      fftBuffer.putShort((summedUp / inputAudioFormat.channelCount).toShort())
      position += inputAudioFormat.channelCount * 2
    }
    inputBuffer.position(limit)
    processFFT(this.fftBuffer)
    processBuffer.flip()
    outputBuffer = this.processBuffer
  }

  private fun processFFT(buffer: ByteBuffer) {
    if (listener == null) {
      return
    }
    srcBuffer.put(buffer.array())
    srcBufferPosition += buffer.array().size
    // PCM 16位，每个样本将是2字节。
    // 获取两倍以获取到最终的大小
    val bytesToProcess = SAMPLE_SIZE * 2
    var currentByte: Byte? = null
    while (srcBufferPosition > audioTrackBufferSize) {
      srcBuffer.position(0)
      srcBuffer.get(tempByteArray, 0, bytesToProcess)

      tempByteArray.forEachIndexed { index, byte ->
        if (currentByte == null) {
          currentByte = byte
        } else {
          src[index / 2] =
            (currentByte!!.toFloat() * Byte.MAX_VALUE + byte) / (Byte.MAX_VALUE * Byte.MAX_VALUE)
          dst[index / 2] = 0f
          currentByte = null
        }

      }
      srcBuffer.position(bytesToProcess)
      srcBuffer.compact()
      srcBufferPosition -= bytesToProcess
      srcBuffer.position(srcBufferPosition)
      val fft = noise.fft(src, dst)
      listener?.onFFTReady(inputAudioFormat.sampleRate, inputAudioFormat.channelCount, fft)
    }
  }

  override fun queueEndOfStream() {
    inputEnded = true
    processBuffer = AudioProcessor.EMPTY_BUFFER
  }

  override fun getOutput(): ByteBuffer {
    val outputBuffer = this.outputBuffer
    this.outputBuffer = AudioProcessor.EMPTY_BUFFER
    return outputBuffer
  }

  override fun isEnded(): Boolean {
    return inputEnded && processBuffer === AudioProcessor.EMPTY_BUFFER
  }

  override fun flush() {
    outputBuffer = AudioProcessor.EMPTY_BUFFER
    inputEnded = false
  }

  override fun reset() {
    flush()
    processBuffer = AudioProcessor.EMPTY_BUFFER
    inputAudioFormat = AudioProcessor.AudioFormat(Format.NO_VALUE, Format.NO_VALUE, Format.NO_VALUE)
  }
}