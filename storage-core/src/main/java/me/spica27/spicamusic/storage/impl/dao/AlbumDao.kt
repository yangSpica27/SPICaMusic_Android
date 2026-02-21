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

    @Query("SELECT * FROM albumentity")
    fun getAllPaging(): PagingSource<Int, AlbumEntity>

    @Query("SELECT * FROM albumentity WHERE title LIKE '%' || :keyword || '%' OR artist LIKE '%' || :keyword || '%'")
    fun getFilteredPaging(keyword: String): PagingSource<Int, AlbumEntity>

    @Query("SELECT * FROM song WHERE albumId = :albumId")
    fun getAlbumSongsFlow(albumId: String): Flow<List<SongEntity>>

}