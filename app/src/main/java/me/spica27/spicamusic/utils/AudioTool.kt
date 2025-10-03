package me.spica27.spicamusic.utils

import android.content.Context
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.media.MediaExtractor
import android.media.MediaFormat
import android.os.ParcelFileDescriptor
import android.provider.MediaStore
import androidx.annotation.OptIn
import androidx.core.database.getIntOrNull
import androidx.core.database.getLongOrNull
import androidx.core.database.getStringOrNull
import androidx.media3.common.MimeTypes
import androidx.media3.common.util.UnstableApi
import me.spica27.spicamusic.App
import me.spica27.spicamusic.db.dao.LyricDao
import me.spica27.spicamusic.db.entity.Lyric
import me.spica27.spicamusic.db.entity.Song
import me.spica27.spicamusic.wrapper.TaglibUtils
import timber.log.Timber

// 查询字段
private val LocalAudioColumns =
    arrayOf(
        MediaStore.Audio.AudioColumns._ID, // 音频id
        MediaStore.Audio.AudioColumns.DATA, // 音频路径
        MediaStore.Audio.AudioColumns.SIZE, // 音频字节大小
        MediaStore.Audio.AudioColumns.DISPLAY_NAME, // 音频名称 xxx.amr
        MediaStore.Audio.AudioColumns.TITLE, // 音频标题
        MediaStore.Audio.AudioColumns.DATE_ADDED, // 音频添加到MediaProvider的时间
        MediaStore.Audio.AudioColumns.DATE_MODIFIED, // 上次修改时间，该列用于内部MediaScanner扫描，外部不要修改
        MediaStore.Audio.AudioColumns.MIME_TYPE, // 音频类型 audio/3gp
        MediaStore.Audio.AudioColumns.DURATION, // 音频时长
        MediaStore.Audio.AudioColumns.BOOKMARK, // 上次音频的回放位置
        MediaStore.Audio.AudioColumns.ARTIST_ID, // 艺人id
        MediaStore.Audio.AudioColumns.ARTIST, // 艺人名称
        MediaStore.Audio.AudioColumns.ALBUM_ID, // 艺人专辑id
        MediaStore.Audio.AudioColumns.ALBUM, // 艺人专辑名称
        MediaStore.Audio.AudioColumns.TRACK,
        MediaStore.Audio.AudioColumns.YEAR, // 录制音频的年份
        MediaStore.Audio.AudioColumns.IS_MUSIC, // 是否为音乐音频
        MediaStore.Audio.AudioColumns.IS_PODCAST,
        MediaStore.Audio.AudioColumns.IS_RINGTONE, // 是否为警告音频
        MediaStore.Audio.AudioColumns.IS_ALARM, // 是否为闹钟音频
        MediaStore.Audio.AudioColumns.IS_NOTIFICATION, // 是否为通知音频
    )

object AudioTool {
    @OptIn(UnstableApi::class)
    fun getSongsFromPhone(
        context: Context,
        lyricDao: LyricDao,
        itemListener: (Song) -> Unit = {},
    ): List<Song> {
        val cursor =
            context.contentResolverSafe.safeQuery(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                LocalAudioColumns,
                null,
                null,
            )

        val types = hashSetOf<String>()
        if (!cursor.moveToFirst()) return listOf()

        val songs = mutableListOf<Song>()
        Timber.d("开始扫描音频文件")

//        val hanyuPinyinOutputFormat =
//            HanyuPinyinOutputFormat().apply {
//                caseType = HanyuPinyinCaseType.LOWERCASE
//                toneType = HanyuPinyinToneType.WITHOUT_TONE
//            }

        do {
            val isMusic =
                cursor.getIntOrNull(cursor.getColumnIndexOrThrow(MediaStore.Audio.AudioColumns.IS_MUSIC))
            if (isMusic != 1) continue
            val mimeType =
                MimeTypes.normalizeMimeType(
                    cursor.getStringOrNull(cursor.getColumnIndexOrThrow(MediaStore.Audio.AudioColumns.MIME_TYPE))
                        ?: "",
                )

            val isSupportMineType = true
            if (!isSupportMineType) continue
            // 过滤掉时长小于5秒的音频文件
            val duration =
                cursor.getLongOrNull(cursor.getColumnIndexOrThrow(MediaStore.Audio.AudioColumns.DURATION))
            if (duration == null || duration < 5000) continue

            val song =
                Song(
                    mediaStoreId = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Audio.AudioColumns._ID)),
                    path =
                        cursor.getStringOrNull(cursor.getColumnIndexOrThrow(MediaStore.Audio.AudioColumns.DATA))
                            ?: "",
                    size =
                        cursor.getLongOrNull(cursor.getColumnIndexOrThrow(MediaStore.Audio.AudioColumns.SIZE))
                            ?: 0,
                    displayName =
                        cursor.getStringOrNull(cursor.getColumnIndexOrThrow(MediaStore.Audio.AudioColumns.DISPLAY_NAME))
                            ?: MediaStore.UNKNOWN_STRING,
                    artist =
                        cursor.getStringOrNull(cursor.getColumnIndexOrThrow(MediaStore.Audio.AudioColumns.ARTIST))
                            ?: "",
                    like = false,
                    sort = 0,
                    duration =
                        cursor.getLongOrNull(cursor.getColumnIndexOrThrow(MediaStore.Audio.AudioColumns.DURATION))
                            ?: 1,
                    mimeType = mimeType,
                    albumId =
                        cursor.getLongOrNull(cursor.getColumnIndexOrThrow(MediaStore.Audio.AudioColumns.ALBUM_ID))
                            ?: 0,
                    sampleRate = 0,
                    bitRate = 0,
                    channels = 0,
                    digit = 0,
                    isIgnore = false,
                )

//      val pinying = PinyinHelper.toHanYuPinyinString(
//        song.displayName,
//        hanyuPinyinOutputFormat,
//        "", false
//      )
//
//      Timber.tag("PinYin").d("PinYin: $pinying")

            val fd: ParcelFileDescriptor? =
                App.getInstance().contentResolverSafe.openFileDescriptor(song.getSongUri(), "r")

            fd?.use {
                val metadata = TaglibUtils.retrieveMetadataWithFD(fd.detachFd())
                if (metadata.title.isNotEmpty()) {
                    song.displayName = metadata.title
                    song.artist = metadata.artist
                    if (metadata.lyricist.isNotEmpty()) {
                        lyricDao.deleteLyric(song.mediaStoreId)
                        lyricDao.insertLyric(
                            lyric =
                                Lyric(
                                    mediaId = song.mediaStoreId,
                                    lyrics = metadata.lyricist,
                                ),
                        )
                    }
                }
            }

            // ALAC使用的mp4容器 需要进一步分析
            if (true) {
                try {
                    val extractor = MediaExtractor()
                    extractor.setDataSource(song.path)
                    val mf: MediaFormat = extractor.getTrackFormat(0)
                    val sampleRate = mf.getInteger(MediaFormat.KEY_SAMPLE_RATE)
                    val channelCount = mf.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
                    song.sampleRate = sampleRate
                    song.channels = channelCount
                    if (extractor.trackCount != 0) {
                        val format = extractor.getTrackFormat(0)
                        val mine = format.getString(MediaFormat.KEY_MIME) ?: mimeType
                        song.mimeType = MimeTypes.normalizeMimeType(mine)
                        Timber.d("音频类型：${song.mimeType}")
                        types.add(song.mimeType)
                    }
                    extractor.release()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            itemListener.invoke(song)
            songs.add(song)
        } while (cursor.moveToNext())
        Timber.d("扫描音频文件完成，共扫描到${songs.size}个音频文件")
        types.forEach {
            Timber.d("音频类型：$it")
        }
        cursor.close()
        return songs
    }

    /**
     * 音频外放
     */
    @Suppress("DEPRECATION")
    fun changeToSpeaker(context: Context) {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        // 注意此处，蓝牙未断开时使用MODE_IN_COMMUNICATION而不是MODE_NORMAL
        audioManager.mode = AudioManager.MODE_IN_COMMUNICATION
        audioManager.stopBluetoothSco()
        audioManager.isBluetoothScoOn = false
        audioManager.isSpeakerphoneOn = true
    }

    /**
     * 切换到蓝牙音箱
     */
    @Suppress("DEPRECATION")
    fun changeToHeadset(context: Context) {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        audioManager.mode = AudioManager.MODE_IN_COMMUNICATION
        audioManager.startBluetoothSco()
        audioManager.isBluetoothScoOn = true
        audioManager.isSpeakerphoneOn = false
    }

    /**
     * 切换到听筒
     */
    @Suppress("DEPRECATION")
    fun changeToReceiver(context: Context) {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        audioManager.isSpeakerphoneOn = false
        audioManager.mode = AudioManager.MODE_IN_COMMUNICATION
    }

    fun getModel(context: Context): Int {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        return audioManager.mode
    }

    fun getDevices(context: Context): Array<AudioDeviceInfo> {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        return audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
    }

    private fun isHeadsetOn(context: Context): Boolean {
        val am = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val devices = am.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
        for (device in devices) {
            if (device.type == AudioDeviceInfo.TYPE_WIRED_HEADSET ||
                device.type == AudioDeviceInfo.TYPE_WIRED_HEADPHONES ||
                device.type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP ||
                device.type == AudioDeviceInfo.TYPE_BLUETOOTH_SCO
            ) {
                return true
            }
        }
        return false
    }

    fun isSpeaker(context: Context): Boolean {
        val am = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val devices = am.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
        for (device in devices) {
            if (device.type == AudioDeviceInfo.TYPE_BUILTIN_SPEAKER) {
                return true
            }
        }
        return false
    }
}
