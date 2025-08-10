/*

MIT License

Copyright (c) 2019 Dániel Zolnai

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.

 */

package me.spica27.spicamusic.visualiser

import android.media.AudioTrack
import android.media.AudioTrack.ERROR_BAD_VALUE
import androidx.media3.common.C
import androidx.media3.common.Format
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.util.Assertions
import androidx.media3.common.util.UnstableApi
import androidx.media3.common.util.Util
import be.tarsos.dsp.util.fft.FFT
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.max

/**
 * An audio processor which forwards the input to the output,
 * but also takes the input and executes a Fast-Fourier Transformation (FFT) on it.
 * The results of this transformation is a 'list' of frequencies with their amplitudes,
 * which will be forwarded to the listener
 */
@UnstableApi
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



  private var FFT: FFT? = null

  private var isActive: Boolean = false

  // 初始化用于存储处理后的音频数据的缓冲区。最初为空。
  private var processBuffer: ByteBuffer

  // 初始化用于存储 FFT 输入数据的缓冲区。最初为空。
  private var fftBuffer: ByteBuffer
  private var outputBuffer: ByteBuffer

  var listeners: MutableList<FFTListener> = mutableListOf()
  private var inputEnded: Boolean = false

  private lateinit var srcBuffer: ByteBuffer
  private var srcBufferPosition = 0
  private val tempByteArray = ByteArray(SAMPLE_SIZE * 2)

  private var audioTrackBufferSize = 0

  private val src = FloatArray(SAMPLE_SIZE)


  interface FFTListener {
    fun onFFTReady(sampleRateHz: Int, channelCount: Int, fft: FloatArray)
  }

  init {
    processBuffer = AudioProcessor.EMPTY_BUFFER
    fftBuffer = AudioProcessor.EMPTY_BUFFER
    outputBuffer = AudioProcessor.EMPTY_BUFFER
  }

  /**
   * 计算一个与 ExoPlayer 内部 AudioTrack 缓冲区大小相匹配的缓冲区大小。
   * 这是为了延迟 FFT 处理，使其与实际音频输出同步，
   * 避免 FFT 结果超前于听到的声音。
   * •getDefaultBufferSizeInBytes 方法的逻辑与 ExoPlayer DefaultAudioSink 中的实现类似
   * ，它考虑了最小和最大缓冲区持续时间以及一个乘法因子
   */
  private fun getDefaultBufferSizeInBytes(audioFormat: AudioProcessor.AudioFormat): Int {
    val outputPcmFrameSize = Util.getPcmFrameSize(audioFormat.encoding, audioFormat.channelCount)
    val minBufferSize =
      AudioTrack.getMinBufferSize(
        audioFormat.sampleRate,
        Util.getAudioTrackChannelConfig(audioFormat.channelCount),
        audioFormat.encoding
      )
    Assertions.checkState(minBufferSize != ERROR_BAD_VALUE)
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

  // 初始化配置
  override fun configure(inputAudioFormat: AudioProcessor.AudioFormat): AudioProcessor.AudioFormat {
    if (inputAudioFormat.encoding != C.ENCODING_PCM_16BIT) {
      throw AudioProcessor.UnhandledAudioFormatException(
        inputAudioFormat
      )
    }
    this.inputAudioFormat = inputAudioFormat
    isActive = true
    FFT = FFT(SAMPLE_SIZE)
    audioTrackBufferSize = getDefaultBufferSizeInBytes(inputAudioFormat)

    // 分配一个字节缓冲区 (srcBuffer)，用于累积足够的数据进行 FFT 处理。
    // 其大小等于计算出的 audioTrackBufferSize
    // 加上一些额外的空间 (BUFFER_EXTRA_SIZE)，以防止在数据到达速度快于处理速度时发生溢出。
    srcBuffer = ByteBuffer.allocate(audioTrackBufferSize + BUFFER_EXTRA_SIZE)

    return inputAudioFormat
  }

  override fun queueInput(inputBuffer: ByteBuffer) {
    //  获取输入缓冲区当前读取位置
    var position = inputBuffer.position()
    // 获取输入缓冲区数据结束位置
    val limit = inputBuffer.limit()
    // 计算输入缓冲区中的音频帧数。PCM 16 位意味着每个样本 2 个字节。
    val frameCount = (limit - position) / (2 * inputAudioFormat.channelCount)
    val singleChannelOutputSize = frameCount * 2
    // 计算所有声道输出所需的总大小（字节
    val outputSize = frameCount * inputAudioFormat.channelCount * 2

    // 检查 processBuffer 和 fftBuffer 的容量是否足够容纳当前输入的数据。
    // 如果不够，则重新分配具有足够大小的直接字节缓冲区
    // (ByteBuffer.allocateDirect) 并设置为本地字节序 (ByteOrder.nativeOrder())。
    // 如果容量足够，则清空缓冲区 (clear())
    // 以备写入新数据。直接字节缓冲区通常用于与 JNI
    // 或其他需要直接内存访问的库进行交互，可以提高性能。
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
      //  初始化一个变量来累加所有声道的值。•for (channelIndex in 0 until inputAudioFormat.channelCount): 遍历每个声道
      var summedUp = 0
      for (channelIndex in 0 until inputAudioFormat.channelCount) {
        val current = inputBuffer.getShort(position + 2 * channelIndex)
        processBuffer.putShort(current)
        summedUp += current
      }
      // For the FFT, we use an currentAverage of all the channels
      fftBuffer.putShort((summedUp / inputAudioFormat.channelCount).toShort())
      position += inputAudioFormat.channelCount * 2
    }

    // •inputBuffer.position(limit): 将输入缓冲区的读取位置设置为其限制，表示所有数据都已被消耗。
    inputBuffer.position(limit)

    // 调用 processFFT 方法，传入包含混合后单声道数据的 fftBuffer
    processFFT(this.fftBuffer)

    // •processBuffer.flip(): 翻转 processBuffer，使其从写入模式切换到读取模式，准备好被 getOutput 方法消耗
    processBuffer.flip()
    // 将处理后的 processBuffer 赋值给 outputBuffer
    outputBuffer = this.processBuffer
  }

  private fun processFFT(buffer: ByteBuffer) {
    // 如果没有注册 FFT 监听器，则直接返回，不执行 FFT 计算，以节省资源。
    if (listeners.isEmpty()) {
      return
    }
    // 将传入的 buffer (即 fftBuffer 中的数据) 的内容复制到 srcBuffer 的当前位置
    srcBuffer.put(buffer.array())
    // 更新 srcBuffer 中有效数据的字节数。
    srcBufferPosition += buffer.array().size
    //  FFT 处理的窗口大小（SAMPLE_SIZE 个样本），每个样本 2 字节。
    val bytesToProcess = SAMPLE_SIZE * 2
    var currentByte: Byte? = null
    // while (srcBufferPosition > audioTrackBufferSize): 这是一个关键的同步机制。
    // 只有当 srcBuffer 中累积的数据量超过了 audioTrackBufferSize（模拟 AudioTrack 缓冲区的延迟）时，
    // 才开始处理 FFT 数据。这确保了 FFT 分析的音频数据与用户实际听到的音频大致同步
    while (srcBufferPosition > audioTrackBufferSize) {
      // 将 srcBuffer 的读取位置重置到开头
      srcBuffer.position(0)
      // 从 srcBuffer 的开头读取 bytesToProcess 数量的字节到 tempByteArray
      srcBuffer.get(tempByteArray, 0, bytesToProcess)

      // •遍历 tempByteArray，每两个字节（一个 short，代表一个 PCM 16 位样本）合并成一个浮点数，
      // 并归一化到 [-1.0, 1.0] 的范围
      // （尽管这里的实现 (currentByte.toFloat() * Byte.MAX_VALUE + byte) /
      // (Byte.MAX_VALUE * Byte.MAX_VALUE) 看起来有点不寻常，通常的做法是将 short 值直接除以
      // Short.MAX_VALUE 或 32768.0f）。转换后的浮点数存入 src 数组，该数组是 FFT 函数的输入
      tempByteArray.forEachIndexed { index, byte ->
        if (currentByte == null) {
          currentByte = byte
        } else {
          src[index / 2] =
            (currentByte.toFloat() * Byte.MAX_VALUE + byte) / (Byte.MAX_VALUE * Byte.MAX_VALUE)
          currentByte = null
        }
      }
      // 将 srcBuffer 的读取位置移动到已处理数据的末尾。
      srcBuffer.position(bytesToProcess)
      // 丢弃已读取的数据（0 到 bytesToProcess），并将剩余的数据（从 bytesToProcess 到 srcBufferPosition）移动到缓冲区的开头。
      srcBuffer.compact()
      // 更新 srcBuffer 中有效数据的字节数。
      srcBufferPosition -= bytesToProcess
      // 将 srcBuffer 的写入位置设置到有效数据的末尾。
      srcBuffer.position(srcBufferPosition)

      FFT?.forwardTransform(src)
      for (listener in listeners) {
        listener.onFFTReady(inputAudioFormat.sampleRate, inputAudioFormat.channelCount, src)
      }
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
    // A new stream is incoming.
  }


  override fun reset() {
    flush()
    processBuffer = AudioProcessor.EMPTY_BUFFER
    inputAudioFormat = AudioProcessor.AudioFormat(Format.NO_VALUE, Format.NO_VALUE, Format.NO_VALUE)
  }
}