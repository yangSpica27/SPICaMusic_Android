package me.spica27.spicamusic.storage.impl.scanner

import androidx.room.withTransaction
import me.spica27.spicamusic.storage.impl.dao.LibraryDao
import me.spica27.spicamusic.storage.impl.db.AppDatabase
import me.spica27.spicamusic.storage.impl.entity.AlbumArtistEntity
import me.spica27.spicamusic.storage.impl.entity.AlbumEntity
import me.spica27.spicamusic.storage.impl.entity.ArtistEntity
import me.spica27.spicamusic.storage.impl.entity.LibraryStateEntity
import me.spica27.spicamusic.storage.impl.entity.SongArtistEntity
import me.spica27.spicamusic.storage.impl.entity.SongEntity
import me.spica27.spicamusic.storage.impl.index.IndexPolicy
import me.spica27.spicamusic.storage.impl.index.IndexSong
import me.spica27.spicamusic.storage.impl.index.IndexText
import me.spica27.spicamusic.storage.impl.index.LibraryIndexer
import me.spica27.spicamusic.storage.impl.index.TagJson

/**
 * 曲库唯一的写入入口：歌曲 upsert / 删除、关系重建、v22 重建收尾。
 * 扫描与导入都经由这里，避免两套逻辑交替覆盖。
 */
class LibraryWriter(
    private val database: AppDatabase,
    text: IndexText = AndroidIndexText.instance,
) {
    private val indexer = LibraryIndexer(text)
    private val songDao = database.songDao()
    private val dao = database.libraryDao()

    /** 一个事务：写入歌曲 → 回填暂存标记 → 删除消失的歌曲 → 按需重建关系 / 重建收尾 */
    suspend fun commit(
        upserts: List<SongEntity>,
        removedMediaStoreIds: Collection<Long> = emptyList(),
        policy: IndexPolicy,
        reindex: Boolean,
        finalizeRebuild: Boolean = false,
    ) = database.withTransaction {
        if (upserts.isNotEmpty()) {
            songDao.upsertSongs(upserts)
            applyCarryOver()
        }
        // 删除用 IN 参数，SQLite 上限 999，分批执行
        removedMediaStoreIds.chunked(500).forEach { songDao.deleteSongsByMediaStoreIds(it) }
        if (reindex || finalizeRebuild) reindexInTransaction(policy)
        if (finalizeRebuild) finalizeRebuildInTransaction()
    }

    suspend fun reindex(policy: IndexPolicy) = database.withTransaction { reindexInTransaction(policy) }

    /** 回填 v22 迁移暂存的喜欢 / 导入标记；须在事务内调用，幂等 */
    suspend fun applyCarryOver() {
        dao.applyCarriedLikes()
        dao.applyCarriedImports()
    }

    /** 按当前标签重建艺人、专辑与署名，只写差异。须在事务内调用 */
    suspend fun reindexInTransaction(policy: IndexPolicy) {
        val rows = dao.indexRows()
        val graph = indexer.build(rows.map { it.toIndexSong() }, policy)

        val artistIds = syncArtists(graph.artists.map { ArtistEntity(0, it.key, it.name, it.sortName, it.kind) })
        val albumIds =
            syncAlbums(
                graph.albums.map { AlbumEntity(0, it.groupKey, it.title, it.artist, it.sortName, it.year, it.isCompilation) },
            )

        // 多余的艺人 / 专辑等关系同步完再按孤儿删除，避免先删被引用的行
        rows.forEach { row ->
            val target = graph.songAlbums[row.songId]?.let(albumIds::getValue)
            if (row.albumId != target) dao.setSongAlbum(row.songId, target)
        }

        val songCredits =
            graph.songCredits.flatMap { (songId, credits) ->
                credits.map { SongArtistEntity(songId, it.role, artistIds.getValue(it.artistKey), it.order) }
            }
        syncRows(dao.songArtists(), songCredits, dao::deleteSongArtists, dao::upsertSongArtists)

        val albumCredits =
            graph.albums.flatMap { album ->
                album.artistKeys.mapIndexed { index, key ->
                    AlbumArtistEntity(albumIds.getValue(album.groupKey), artistIds.getValue(key), index)
                }
            }
        syncRows(dao.albumArtists(), albumCredits, dao::deleteAlbumArtists, dao::upsertAlbumArtists)

        dao.deleteOrphanAlbums()
        dao.deleteOrphanArtists()
        if (dao.updateIndexPolicy(policy.fingerprint) == 0) {
            dao.insertState(LibraryStateEntity(indexPolicy = policy.fingerprint))
        }
    }

    /** 首次完整重扫成功后：删除找不到歌曲的歌单条目，清空暂存，结束重建。须在事务内调用 */
    suspend fun finalizeRebuildInTransaction() {
        dao.markPlaylistsWithMissingSongs()
        dao.deleteMissingPlaylistEntries()
        dao.clearCarryOver()
        dao.clearPendingRebuild()
    }

    /** 按 nameKey upsert，返回 nameKey → artistId；已有艺人只更新显示名等字段，ID 不变 */
    private suspend fun syncArtists(targets: List<ArtistEntity>): Map<String, Long> {
        val existing = dao.artists().associateBy { it.nameKey }
        val ids = HashMap<String, Long>()
        val updates = mutableListOf<ArtistEntity>()
        val inserts = mutableListOf<ArtistEntity>()
        targets.forEach { target ->
            val current = existing[target.nameKey]
            if (current == null) {
                inserts.add(target)
            } else {
                ids[target.nameKey] = current.artistId
                val merged = target.copy(artistId = current.artistId)
                if (merged != current) updates.add(merged)
            }
        }
        if (updates.isNotEmpty()) dao.updateArtists(updates)
        if (inserts.isNotEmpty()) dao.insertArtists(inserts).forEachIndexed { i, id -> ids[inserts[i].nameKey] = id }
        return ids
    }

    /** 按 groupKey upsert，返回 groupKey → albumId；已有专辑 ID 不变 */
    private suspend fun syncAlbums(targets: List<AlbumEntity>): Map<String, Long> {
        val existing = dao.albums().associateBy { it.groupKey }
        val ids = HashMap<String, Long>()
        val updates = mutableListOf<AlbumEntity>()
        val inserts = mutableListOf<AlbumEntity>()
        targets.forEach { target ->
            val current = existing[target.groupKey]
            if (current == null) {
                inserts.add(target)
            } else {
                ids[target.groupKey] = current.albumId
                val merged = target.copy(albumId = current.albumId)
                if (merged != current) updates.add(merged)
            }
        }
        if (updates.isNotEmpty()) dao.updateAlbums(updates)
        if (inserts.isNotEmpty()) dao.insertAlbums(inserts).forEachIndexed { i, id -> ids[inserts[i].groupKey] = id }
        return ids
    }

    /** 关系行整行比较：多余的删除，缺少或顺序变化的 upsert */
    private suspend fun <T> syncRows(
        current: List<T>,
        target: List<T>,
        delete: suspend (List<T>) -> Unit,
        upsert: suspend (List<T>) -> Unit,
    ) {
        val targetSet = target.toHashSet()
        val currentSet = current.toHashSet()
        val stale = current.filter { it !in targetSet }
        val missing = target.filter { it !in currentSet }
        stale.chunked(500).forEach { delete(it) }
        missing.chunked(500).forEach { upsert(it) }
    }

    private fun LibraryDao.IndexRow.toIndexSong() =
        IndexSong(
            songId = songId,
            path = path,
            album = album,
            artists = TagJson.decode(artistTags),
            albumArtists = TagJson.decode(albumArtistTags),
            composers = TagJson.decode(composerTags),
            lyricists = TagJson.decode(lyricistTags),
            year = year,
            isCompilation = isCompilation,
        )
}
