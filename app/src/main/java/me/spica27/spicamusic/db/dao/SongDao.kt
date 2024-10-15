package me.spica27.spicamusic.db.dao

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import me.spica27.spicamusic.db.entity.Song


@Dao
interface SongDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(song: Song)


    // 切换是否喜欢
    @Query("UPDATE song SET `like` = (CASE WHEN`LIKE` == 1 THEN 0 ELSE 1 END)  WHERE( songId == :id)")
    suspend fun toggleLike(id: Long)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(songs: List<Song>)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insertSync(songs: List<Song>)


    @Query("DELETE FROM song WHERE mediaStoreId NOT IN (:mediaIds)")
    suspend fun deleteSongsNotInList(mediaIds: List<Long>)

    @Query(
        "SELECT * FROM song WHERE displayName LIKE '%' ||:name|| '%'" +
            "OR artist LIKE '%' ||:name|| '%'"
    )
    fun getsSongsFromName(name: String): Flow<List<Song>>


    @Query(
        "SELECT * FROM song WHERE displayName LIKE '%' ||:name|| '%'" +
            "OR artist LIKE '%' ||:name|| '%'"
    )
    fun getsSongsFromNameSync(name: String): List<Song>

    @Query("SELECT * FROM song WHERE songId == :id")
    fun getSongWithId(id: Long): Song

//    @Query("SELECT * FROM song WHERE mediaStoreId == :id")
//    fun getSongWithMediaStoreId(id: Long): Song?

    @Query("SELECT * FROM song WHERE songId == :id")
    fun getSongFlowWithId(id: Long): Flow<Song?>

    @Query("SELECT `like` FROM song WHERE songId == :id")
    fun getSongIsLikeFlowWithId(id: Long): Flow<Int>

    @Query("SELECT * FROM song")
    fun getAll(): Flow<List<Song>>

    @Query("SELECT * FROM song")
    fun getAllSync(): List<Song>

    @Query("SELECT * FROM song WHERE `like` == 1")
    fun getAllLikeSong(): Flow<List<Song>>


    @Query("DELETE FROM song")
    suspend fun deleteAll()

    @Query("DELETE FROM song")
    fun deleteAllSync()

    @Delete
    suspend fun delete(song: Song)

    @Delete
    suspend fun delete(song: List<Song>)

    @Transaction
    suspend fun updateSongs(songs: List<Song>) {
        val songIds = songs.map { it.mediaStoreId }
        deleteSongsNotInList(songIds)
        insert(songs)
    }


}