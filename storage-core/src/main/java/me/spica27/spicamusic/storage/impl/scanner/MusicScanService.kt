package me.spica27.spicamusic.storage.impl.scanner

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.core.net.toUri
import com.ibm.icu.text.Transliterator
import com.kyant.taglib.TagLib
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import me.spica27.spicamusic.storage.api.IMusicScanService
import me.spica27.spicamusic.storage.api.ScanProgress
import me.spica27.spicamusic.storage.api.ScanResult
import me.spica27.spicamusic.storage.impl.dao.SongDao
import me.spica27.spicamusic.storage.impl.entity.SongEntity
import timber.log.Timber

/**
 * 音乐扫描服务实现 - 基于 MediaStore 和文件系统扫描
 */
class MusicScanService(
  private val context: Context,
  private val songDao: SongDao,
) : IMusicScanService {

  private val _scanProgress = MutableStateFlow<ScanProgress?>(null)
  private val _isScanning = MutableStateFlow(false)
  private var isCancelled = false

  companion object {
    private const val TAG = "MusicScanService"

    // 支持的音频格式
    private val SUPPORTED_MIME_TYPES = setOf(
      "audio/mpeg",      // MP3
      "audio/mp4",       // M4A
      "audio/flac",      // FLAC
      "audio/ogg",       // OGG
      "audio/wav",       // WAV
      "audio/x-wav",     // WAV
      "audio/x-flac",    // FLAC
      "audio/aac",       // AAC
      "audio/opus",      // OPUS
    )
  }

  override fun getScanProgress(): Flow<ScanProgress?> = _scanProgress.asStateFlow()

  override fun isScanning(): Flow<Boolean> = _isScanning.asStateFlow()

  override fun cancelScan() {
    isCancelled = true
  }

  override suspend fun scanMediaStore(): ScanResult = withContext(Dispatchers.IO) {
    if (_isScanning.value) {
      Timber.tag(TAG).w("扫描已在进行中")
      return@withContext ScanResult(0, 0, 0, 0)
    }

    _isScanning.value = true
    isCancelled = false
    var totalScanned = 0
    var newAdded = 0
    var updated = 0

    try {
      val scannedSongs = mutableListOf<SongEntity>()
      val contentResolver = context.contentResolver

      // 查询 MediaStore
      val projection = arrayOf(
        MediaStore.Audio.Media._ID,
        MediaStore.Audio.Media.DISPLAY_NAME,
        MediaStore.Audio.Media.ARTIST,
        MediaStore.Audio.Media.SIZE,
        MediaStore.Audio.Media.DURATION,
        MediaStore.Audio.Media.MIME_TYPE,
        MediaStore.Audio.Media.ALBUM_ID,
        MediaStore.Audio.Media.DATA,
      )

      val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0"
      val sortOrder = "${MediaStore.Audio.Media.DISPLAY_NAME} ASC"

      val uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
      } else {
        MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
      }

      contentResolver.query(
        uri,
        projection,
        selection,
        null,
        sortOrder
      )?.use { cursor ->
        val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
        val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME)
        val artistColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
        val sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.SIZE)
        val durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
        val mimeTypeColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.MIME_TYPE)
        val albumIdColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
        val dataColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)

        val totalCount = cursor.count
        Timber.tag(TAG).d("开始扫描 MediaStore，共 $totalCount 个音频文件")

        while (cursor.moveToNext() && !isCancelled) {
          val mediaStoreId = cursor.getLong(idColumn)
          val displayName = cursor.getString(nameColumn) ?: "Unknown"
          val artist = cursor.getString(artistColumn) ?: "Unknown Artist"
          val size = cursor.getLong(sizeColumn)
          val duration = cursor.getLong(durationColumn)
          val mimeType = cursor.getString(mimeTypeColumn) ?: ""
          val albumId = cursor.getLong(albumIdColumn)
          val path = cursor.getString(dataColumn) ?: ""

          // 过滤不支持的格式
          if (!SUPPORTED_MIME_TYPES.contains(mimeType)) {
            continue
          }

          // 过滤太短的音频（小于 10 秒）
          if (duration < 10000) {
            continue
          }

          totalScanned++

          // 更新进度
          _scanProgress.value = ScanProgress(
            current = totalScanned,
            total = totalCount,
            currentFile = displayName
          )

          // 使用 Taglib 读取音频详细信息（采样率、比特率等）
          val audioInfo = extractAudioInfoWithTaglib(
            contentResolver = contentResolver,
            mediaStoreId = mediaStoreId,
            fallbackDuration = duration
          )

          // 使用 Taglib 读取的元数据优先，回退到 MediaStore 数据
          val finalDisplayName = audioInfo.title ?: displayName
          val finalArtist = audioInfo.artist ?: artist

          // 生成排序名称
          val sortName = generateSortName(finalDisplayName)

          val song = SongEntity(
            songId = null, // 让 Room 自动生成
            mediaStoreId = mediaStoreId,
            path = path,
            displayName = finalDisplayName,
            artist = finalArtist,
            size = size,
            like = false, // 新歌曲默认不喜欢
            duration = audioInfo.duration.takeIf { it > 0 } ?: duration,
            sort = 0,
            mimeType = mimeType,
            albumId = albumId,
            sampleRate = audioInfo.sampleRate,
            bitRate = audioInfo.bitRate,
            channels = audioInfo.channels,
            digit = audioInfo.digit,
            isIgnore = false,
            sortName = sortName
          )

          scannedSongs.add(song)
        }
      }

      if (isCancelled) {
        Timber.tag(TAG).w("扫描已取消")
        return@withContext ScanResult(totalScanned, 0, 0, 0)
      }

      // 获取数据库中现有的歌曲
      val existingSongs = songDao.getAllSync()
      val existingMediaStoreIds = existingSongs.map { it.mediaStoreId }.toSet()
      val scannedMediaStoreIds = scannedSongs.map { it.mediaStoreId }.toSet()

      // 计算新增和更新
      scannedSongs.forEach { song ->
        if (song.mediaStoreId in existingMediaStoreIds) {
          // 保留已有歌曲的 like 和 isIgnore 状态
          val existingSong = existingSongs.find { it.mediaStoreId == song.mediaStoreId }
          if (existingSong != null) {
            song.like = existingSong.like
            song.isIgnore = existingSong.isIgnore
            song.sort = existingSong.sort
          }
          updated++
        } else {
          newAdded++
        }
      }

      // 更新数据库（会自动删除不在列表中的歌曲）
      songDao.updateSongs(scannedSongs)

      // 计算删除的数量
      val removed = existingSongs.size - (scannedSongs.size - newAdded)

      Timber.tag(TAG).i(
        "扫描完成: 总计=$totalScanned, 新增=$newAdded, 更新=$updated, 删除=$removed"
      )

      ScanResult(
        totalScanned = totalScanned,
        newAdded = newAdded,
        updated = updated,
        removed = removed.coerceAtLeast(0)
      )
    } catch (e: Exception) {
      Timber.tag(TAG).e(e, "扫描失败")
      ScanResult(0, 0, 0, 0)
    } finally {
      _isScanning.value = false
      _scanProgress.value = null
    }
  }

  override suspend fun scanFolder(folderPath: String): ScanResult {
    // TODO: 实现文件夹扫描（预留接口）
    Timber.tag(TAG).w("文件夹扫描功能尚未实现: $folderPath")
    return ScanResult(0, 0, 0, 0)
  }

  override suspend fun scanFolders(folderPaths: List<String>): ScanResult {
    // TODO: 实现多文件夹扫描（预留接口）
    Timber.tag(TAG).w("多文件夹扫描功能尚未实现")
    return ScanResult(0, 0, 0, 0)
  }

  /**
   * 使用 Taglib 提取音频详细信息（优先使用）
   */
  private fun extractAudioInfoWithTaglib(
    contentResolver: ContentResolver,
    mediaStoreId: Long,
    fallbackDuration: Long,
  ): AudioInfo {
    var title: String? = null
    var artist: String? = null
    var duration = 0L
    var sampleRate = 0
    var bitRate = 0
    var channels = 0
    var digit = 0

    try {
      val uri = "content://media/external/audio/media/$mediaStoreId".toUri()

      // 打开文件描述符
      contentResolver.openFileDescriptor(uri, "r")?.use { pfd ->
        try {

          val fd = pfd.detachFd()

          // 读取元数据（标题和艺术家）
          val metadata = TagLib.getMetadata(
            fd = fd,
            readPictures = false // 扫描时不读取图片以提高速度
          )

          if (metadata != null) {
            title = metadata.propertyMap["TITLE"]?.firstOrNull()
            artist = metadata.propertyMap["ARTIST"]?.firstOrNull()
          }

        } catch (e: Exception) {
          Timber.tag(TAG).w(e, "Taglib 读取失败，回退到默认值")
        }
      }


      contentResolver.openFileDescriptor(uri, "r")?.use { pfd ->
        try {

          TagLib.getAudioProperties(
            pfd.detachFd()
          )?.let {
            duration = (it.length).toLong()
            sampleRate = it.sampleRate
            bitRate = it.bitrate
            channels = it.channels
          }

        } catch (e: Exception) {
          Timber.tag(TAG).w(e, "Taglib 读取失败，回退到默认值")
        }
      }

      // 如果 Taglib 未能获取 duration，使用回退值
      if (duration == 0L) {
        duration = fallbackDuration
      }

      // 从 MediaStore 获取 bitrate（作为补充）
      if (bitRate == 0 && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        contentResolver.query(
          Uri.withAppendedPath(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            mediaStoreId.toString()
          ),
          arrayOf(MediaStore.Audio.Media.BITRATE),
          null,
          null,
          null
        )?.use { cursor ->
          if (cursor.moveToFirst()) {
            val bitRateColumn = cursor.getColumnIndex(MediaStore.Audio.Media.BITRATE)
            if (bitRateColumn != -1) {
              // MediaStore 返回的是 bps，转换为 kbps
              bitRate = cursor.getInt(bitRateColumn) / 1000
            }
          }
        }
      }

      // 使用默认值填充缺失的信息
      if (sampleRate == 0) sampleRate = 44100 // 默认 44.1kHz
      if (channels == 0) channels = 2 // 默认立体声
      if (digit == 0) digit = 16 // 默认 16-bit

    } catch (e: Exception) {
      Timber.tag(TAG).w(e, "提取音频信息失败: $mediaStoreId")
      // 返回默认值
      duration = fallbackDuration
      sampleRate = 44100
      channels = 2
      digit = 16
    }

    return AudioInfo(
      title = title,
      artist = artist,
      duration = duration,
      sampleRate = sampleRate,
      bitRate = bitRate,
      channels = channels,
      digit = digit
    )
  }

  /**
   * 根据歌曲名称生成排序用的首字符
   * - 英文：直接使用首字符（大写）
   * - 中文：转换为拼音首字母
   * - 日文：转换为罗马音首字母
   * - 其他：返回 "#"
   */
  private fun generateSortName(displayName: String): String {
    if (displayName.isEmpty()) return "#"

    val firstChar = displayName.first()

    return try {
      when {
        // 英文字母（A-Z, a-z）
        firstChar.isLetter() && firstChar.code in 0x41..0x7A -> {
          firstChar.uppercaseChar().toString()
        }

        // 中文（CJK 统一表意文字）
        firstChar.code in 0x4E00..0x9FFF -> {
          val transliterator = Transliterator.getInstance("Han-Latin; Latin-ASCII")
          val pinyin = transliterator.transliterate(firstChar.toString())
          pinyin.firstOrNull()?.uppercaseChar()?.toString() ?: "#"
        }

        // 日文平假名（ひらがな）
        firstChar.code in 0x3040..0x309F -> {
          val transliterator = Transliterator.getInstance("Hiragana-Latin")
          val romaji = transliterator.transliterate(firstChar.toString())
          romaji.firstOrNull()?.uppercaseChar()?.toString() ?: "#"
        }

        // 日文片假名（カタカナ）
        firstChar.code in 0x30A0..0x30FF -> {
          val transliterator = Transliterator.getInstance("Katakana-Latin")
          val romaji = transliterator.transliterate(firstChar.toString())
          romaji.firstOrNull()?.uppercaseChar()?.toString() ?: "#"
        }

        // 数字
        firstChar.isDigit() -> "#"

        // 其他字符
        else -> "#"
      }
    } catch (e: Exception) {
      Timber.tag(TAG).w(e, "生成排序名称失败: $displayName")
      "#"
    }
  }

  /**
   * 音频信息数据类（扩展版本，包含元数据）
   */
  private data class AudioInfo(
    val title: String? = null,
    val artist: String? = null,
    val duration: Long,
    val sampleRate: Int,
    val bitRate: Int,
    val channels: Int,
    val digit: Int,
  )
}
