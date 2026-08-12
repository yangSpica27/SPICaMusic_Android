package me.spica27.spicamusic.storage.impl.dao

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow
import me.spica27.spicamusic.storage.impl.entity.AlbumEntity
import me.spica27.spicamusic.storage.impl.entity.SongEntity


@Dao
interface AlbumDao {

    @Query("DELETE FROM albumentity")
    fun deleteAll()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAll(list: List<AlbumEntity>)

    @Transaction
    fun replaceAll(list: List<AlbumEntity>) {
        deleteAll()
        insertAll(list)
    }

    @Query("DELETE FROM albumentity WHERE id IN (:albumIds)")
    fun deleteByIds(albumIds: List<String>)

    @Transaction
    fun replaceByIds(
        albumIds: List<String>,
        list: List<AlbumEntity>,
    ) {
        if (albumIds.isNotEmpty()) {
            deleteByIds(albumIds)
        }
        if (list.isNotEmpty()) {
            insertAll(list)
        }
    }

    @Query("SELECT * FROM albumentity")
    fun getAllPaging(): PagingSource<Int, AlbumEntity>

    @Query("SELECT * FROM albumentity WHERE title LIKE '%' || :keyword || '%' OR artist LIKE '%' || :keyword || '%'")
    fun getFilteredPaging(keyword: String): PagingSource<Int, AlbumEntity>

    // 同一歌手的专辑
    @Query(
        "SELECT * FROM albumentity WHERE artist = :artist " +
            "ORDER BY year DESC, title ASC",
    )
    fun getAlbumsByArtistFlow(artist: String): Flow<List<AlbumEntity>>

    // 专辑内按曲目序号排序：未知序号(<=0)置于末尾，其余按 trackNumber 升序，最后以名称兜底
    @Query(
        "SELECT * FROM song WHERE albumId = :albumId " +
            "ORDER BY CASE WHEN trackNumber <= 0 THEN 1 ELSE 0 END, trackNumber ASC, displayName ASC",
    )
    fun getAlbumSongsFlow(albumId: String): Flow<List<SongEntity>>

}
