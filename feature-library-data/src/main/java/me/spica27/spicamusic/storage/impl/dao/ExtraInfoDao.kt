package me.spica27.spicamusic.storage.impl.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow
import me.spica27.spicamusic.storage.impl.entity.ExtraInfoEntity

@Dao
interface ExtraInfoDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertLyric(lyric: ExtraInfoEntity)

    @Transaction
    fun suppressLyrics(mediaId: Long) {
        val existing = getLyricWithMediaId(mediaId)
        insertLyric((existing ?: ExtraInfoEntity(mediaId = mediaId)).copy(lyricsSuppressed = true))
    }

    /** 原子保存歌词状态，自动结果不覆盖用户选择。 */
    @Transaction
    fun saveSelectedLyrics(lyric: ExtraInfoEntity) {
        val existing = getLyricWithMediaId(lyric.mediaId)
        if (!lyric.isManual &&
            existing != null &&
            (existing.lyricsSuppressed || existing.isManual || existing.restoredSnapshot)
        ) {
            return
        }
        insertLyric(
            lyric.copy(
                id = existing?.id ?: 0,
                cover = existing?.cover ?: lyric.cover,
                lyricsSuppressed = false,
                restoredSnapshot = false,
            ),
        )
    }

    @Query("DELETE FROM extra_info WHERE mediaId = :songId")
    fun deleteLyric(songId: Long)

    @Query("SELECT * FROM extra_info WHERE mediaId = :songId LIMIT 1")
    fun getLyricWithMediaId(songId: Long): ExtraInfoEntity?

    @Query("SELECT * FROM extra_info")
    fun getLyrics(): Flow<List<ExtraInfoEntity>>

    @Query("DELETE FROM extra_info")
    fun deleteAll()

    @Query("SELECT delay FROM extra_info WHERE mediaId == :mediaId LIMIT 1")
    fun getDelayFlow(mediaId: Long): Flow<Long?>

    @Query("SELECT lyrics FROM extra_info WHERE mediaId == :mediaId LIMIT 1")
    fun getLyricsFlow(mediaId: Long): Flow<String?>

    @Query("UPDATE extra_info SET delay = :delay WHERE mediaId == :mediaId")
    fun updateDelay(
        mediaId: Long,
        delay: Long?,
    )

    /** 更新已有歌词及来源信息。 */
    @Query(
        """
        UPDATE extra_info SET lyrics = :lyrics, lyricSourceName = :sourceName,
            sourceType = :sourceType, isManual = :isManual, sourceUri = :sourceUri,
            restoredSnapshot = 0
        WHERE mediaId = :mediaId
    """,
    )
    fun updateLyricsAndSource(
        mediaId: Long,
        lyrics: String,
        sourceName: String,
        sourceType: String,
        isManual: Boolean,
        sourceUri: String,
    )

    /** 获取歌词来源名称。 */
    @Query("SELECT lyricSourceName FROM extra_info WHERE mediaId = :mediaId LIMIT 1")
    fun getLyricSourceName(mediaId: Long): String?
}
