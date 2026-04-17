package me.spica27.spicamusic.storage.impl.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import me.spica27.spicamusic.storage.impl.entity.ScanFolderEntity

@Dao
interface ScanFolderDao {

    @Query("SELECT * FROM ScanFolder WHERE folderType = :type ORDER BY addedAt DESC")
    fun getByType(type: Int): Flow<List<ScanFolderEntity>>

    @Query("SELECT * FROM ScanFolder WHERE folderType = :type ORDER BY addedAt DESC")
    suspend fun getByTypeSync(type: Int): List<ScanFolderEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: ScanFolderEntity)

    @Query("DELETE FROM ScanFolder WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("UPDATE ScanFolder SET isAccessible = 0 WHERE id = :id")
    suspend fun markInaccessible(id: Long)

    @Query("UPDATE ScanFolder SET uriString = :uriString, isAccessible = 1 WHERE id = :id")
    suspend fun reAuthorize(id: Long, uriString: String)
}
