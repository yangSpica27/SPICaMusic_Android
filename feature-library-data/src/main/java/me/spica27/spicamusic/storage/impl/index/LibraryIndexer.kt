package me.spica27.spicamusic.storage.impl.index

import me.spica27.spicamusic.common.entity.ArtistKind
import me.spica27.spicamusic.common.entity.ArtistRole

/** 解释规则。指纹不同于 LibraryState.indexPolicy 时重建关系。 */
data class IndexPolicy(
    val splitArtists: Boolean,
) {
    val fingerprint: String get() = "v$INDEX_VERSION;split=${if (splitArtists) 1 else 0}"

    companion object {
        /** 名称规范化、拆分或分组规则变化时 +1；只需重建关系，不重读文件 */
        const val INDEX_VERSION = 1

        val DEFAULT = IndexPolicy(splitArtists = false)

        /** 从已保存的指纹还原（导入等没有设置可读的场景沿用上次的规则） */
        fun parse(fingerprint: String): IndexPolicy? =
            fingerprint
                .split(';')
                .firstOrNull { it.startsWith("split=") }
                ?.let { IndexPolicy(splitArtists = it.removePrefix("split=") == "1") }
    }
}

/** 索引器输入：Song 表中参与分类的列 */
data class IndexSong(
    val songId: Long,
    val path: String,
    val album: String,
    val artists: List<String>,
    val albumArtists: List<String>,
    val composers: List<String>,
    val lyricists: List<String>,
    val year: Int,
    val isCompilation: Boolean,
)

data class IndexedArtist(
    val key: String,
    val name: String,
    val sortName: String,
    val kind: ArtistKind,
)

data class IndexedAlbum(
    val groupKey: String,
    val title: String,
    val artist: String,
    val sortName: String,
    val year: Int,
    val isCompilation: Boolean,
    /** 专辑艺人名称键，按署名顺序 */
    val artistKeys: List<String>,
)

data class IndexedCredit(
    val role: ArtistRole,
    val artistKey: String,
    val order: Int,
)

/** 目标关系图。artistKey / groupKey 对应 Artist.nameKey / Album.groupKey */
data class LibraryGraph(
    val artists: List<IndexedArtist>,
    val albums: List<IndexedAlbum>,
    /** songId → groupKey；没有专辑的歌曲不在表中 */
    val songAlbums: Map<Long, String>,
    val songCredits: Map<Long, List<IndexedCredit>>,
)

/**
 * 由标签建立艺人与专辑。纯函数：输出只取决于输入内容，与输入顺序无关。
 * - 艺人：按名称键合并写法变体；缺标签时回退专辑艺人，再归入未知艺人
 * - 专辑：同标题键的歌先按专辑艺人分区；没有专辑艺人的歌并入同目录或演唱者重合的分区，
 *   其余按共同演唱者 / 同目录连通成专辑，再推断专辑艺人
 */
class LibraryIndexer(
    private val text: IndexText,
) {
    /** key 用于匹配；original 是去掉零宽字符后的原文，用于统计显示名 */
    private class Name(
        val key: String,
        val original: String,
    )

    private class Resolved(
        val song: IndexSong,
        val performers: List<Name>,
        val albumArtists: List<Name>,
        val composers: List<Name>,
        val lyricists: List<Name>,
        /** 去掉碟号标记后的标题；没有专辑时为 null */
        val title: String?,
        val titleKey: String?,
        /** 可用于判断"同一张专辑"的目录；平铺目录为 null */
        val directory: String?,
    ) {
        /** 未知艺人不算"共同演唱者"，否则所有缺标签的歌会连成一张专辑 */
        val performerKeys: Set<String> = performers.mapTo(LinkedHashSet()) { it.key } - UNKNOWN_KEY
    }

    private class Group(
        val songs: List<Resolved>,
        val artistKeys: List<String>,
    )

    fun build(
        input: List<IndexSong>,
        policy: IndexPolicy,
    ): LibraryGraph {
        // 名称键 → 各种写法出现次数，用于选显示名
        val spellings = HashMap<String, HashMap<String, Int>>()

        fun resolve(values: List<String>): List<Name> {
            val raw = values.map(String::trim).filter(String::isNotEmpty)
            // 整串就是群星写法（如 "V/A"）时不拆分
            val parts = if (raw.size == 1 && isVarious(raw.single())) raw else ArtistSplitter.split(raw, policy.splitArtists)
            val names = LinkedHashMap<String, Name>()
            for (part in parts) {
                val key = if (isVarious(part)) VARIOUS_KEY else text.key(part)
                if (key.isNotEmpty() && key !in names) names[key] = Name(key, text.clean(part))
            }
            names.values.forEach { name ->
                spellings.getOrPut(name.key) { HashMap() }.merge(name.original, 1, Int::plus)
            }
            return names.values.toList()
        }

        // 按 songId 排序，保证输出与输入顺序无关
        val resolved =
            input.sortedBy { it.songId }.map { song ->
                val albumArtists = resolve(song.albumArtists)
                val title = AlbumTitles.strip(text.clean(song.album)).takeIf { it.isNotBlank() }
                Resolved(
                    song = song,
                    // 没有艺人标签时回退专辑艺人，都没有时归入未知艺人
                    performers = resolve(song.artists).ifEmpty { albumArtists }.ifEmpty { listOf(Name(UNKNOWN_KEY, "")) },
                    albumArtists = albumArtists,
                    composers = resolve(song.composers),
                    lyricists = resolve(song.lyricists),
                    title = title,
                    titleKey = title?.let(text::key)?.takeIf { it.isNotEmpty() },
                    directory = directoryOf(song.path),
                )
            }

        val groups =
            resolved
                .filter { it.titleKey != null }
                .groupBy { requireNotNull(it.titleKey) }
                .values
                .flatMap(::groupBucket)

        /** 显示名取出现最多的写法，次数相同取字典序最小的 */
        fun artistName(key: String): String =
            when (key) {
                UNKNOWN_KEY -> ""
                else ->
                    spellings[key]
                        ?.entries
                        ?.sortedWith(compareByDescending<Map.Entry<String, Int>> { it.value }.thenBy { it.key })
                        ?.firstOrNull()
                        ?.key
                        ?: if (key == VARIOUS_KEY) DEFAULT_VARIOUS_NAME else key
            }

        val albums = assignGroupKeys(groups)
        val indexedAlbums =
            albums.map { (groupKey, group) ->
                val title = mostCommon(group.songs.mapNotNull { it.title })
                IndexedAlbum(
                    groupKey = groupKey,
                    title = title,
                    artist = group.artistKeys.map(::artistName).filter(String::isNotEmpty).joinToString(" / "),
                    sortName = text.sortName(title),
                    year = albumYear(group.songs),
                    isCompilation = group.artistKeys == listOf(VARIOUS_KEY) || group.songs.any { it.song.isCompilation },
                    artistKeys = group.artistKeys,
                )
            }

        val songAlbums = HashMap<Long, String>()
        albums.forEach { (groupKey, group) -> group.songs.forEach { songAlbums[it.song.songId] = groupKey } }

        val songCredits =
            resolved.associate { r ->
                r.song.songId to
                    buildList {
                        r.performers.forEachIndexed { i, name -> add(IndexedCredit(ArtistRole.ARTIST, name.key, i)) }
                        r.composers.forEachIndexed { i, name -> add(IndexedCredit(ArtistRole.COMPOSER, name.key, i)) }
                        r.lyricists.forEachIndexed { i, name -> add(IndexedCredit(ArtistRole.LYRICIST, name.key, i)) }
                    }
            }

        val artistKeys = songCredits.values.flatten().map { it.artistKey } + indexedAlbums.flatMap { it.artistKeys }
        val artists =
            artistKeys.toSortedSet().map { key ->
                val name = artistName(key)
                IndexedArtist(
                    key = key,
                    name = name,
                    sortName = if (key == UNKNOWN_KEY || key == VARIOUS_KEY) "#" else text.sortName(name),
                    kind =
                        when (key) {
                            UNKNOWN_KEY -> ArtistKind.UNKNOWN
                            VARIOUS_KEY -> ArtistKind.VARIOUS
                            else -> ArtistKind.NORMAL
                        },
                )
            }

        return LibraryGraph(
            artists = artists,
            albums = indexedAlbums.sortedBy { it.groupKey },
            songAlbums = songAlbums,
            songCredits = songCredits,
        )
    }

    /** 同一标题键的歌曲：先按专辑艺人分区，再处理没有专辑艺人的歌曲 */
    private fun groupBucket(bucket: List<Resolved>): List<Group> {
        val partitions = LinkedHashMap<String, MutableList<Resolved>>()
        val partitionArtists = HashMap<String, List<String>>()
        bucket.filter { it.albumArtists.isNotEmpty() }.forEach { song ->
            val keys = song.albumArtists.map { it.key }
            val partitionKey = keys.sorted().joinToString("\u0000")
            partitions.getOrPut(partitionKey) { mutableListOf() }.add(song)
            partitionArtists.putIfAbsent(partitionKey, keys)
        }
        // 候选判断只看带专辑艺人的歌曲所在目录，结果不受加入顺序影响；
        // 优先级：同目录 > 与专辑艺人重合多 > 分区键小
        val partitionDirs = partitions.mapValues { (_, songs) -> songs.mapNotNullTo(HashSet()) { it.directory } }

        val unassigned = mutableListOf<Resolved>()
        bucket.filter { it.albumArtists.isEmpty() }.forEach { song ->
            val best =
                partitions.keys
                    .map { key ->
                        val sameDirectory = song.directory != null && song.directory in partitionDirs.getValue(key)
                        val overlap = partitionArtists.getValue(key).count { it in song.performerKeys }
                        Triple(key, sameDirectory, overlap)
                    }.filter { (_, sameDirectory, overlap) -> sameDirectory || overlap > 0 }
                    .sortedWith(
                        compareByDescending<Triple<String, Boolean, Int>> { it.second }
                            .thenByDescending { it.third }
                            .thenBy { it.first },
                    ).firstOrNull()
                    ?.first
            if (best != null) partitions.getValue(best).add(song) else unassigned.add(song)
        }

        val tagged = partitions.map { (key, songs) -> Group(songs.sortedBy { it.song.songId }, partitionArtists.getValue(key)) }
        return tagged + connectUnassigned(unassigned).map { songs -> Group(songs, inferAlbumArtists(songs)) }
    }

    /** 没有专辑艺人的歌曲：有共同演唱者或同一目录就连通，每个连通块是一张专辑 */
    private fun connectUnassigned(songs: List<Resolved>): List<List<Resolved>> {
        val parent = IntArray(songs.size) { it }

        fun find(i: Int): Int {
            var root = i
            while (parent[root] != root) root = parent[root]
            var node = i
            while (parent[node] != root) {
                val next = parent[node]
                parent[node] = root
                node = next
            }
            return root
        }

        fun union(
            a: Int,
            b: Int,
        ) {
            val rootA = find(a)
            val rootB = find(b)
            // 总是挂到较小下标上，结果与遍历顺序无关
            if (rootA != rootB) parent[maxOf(rootA, rootB)] = minOf(rootA, rootB)
        }

        val firstByPerformer = HashMap<String, Int>()
        val firstByDirectory = HashMap<String, Int>()
        songs.forEachIndexed { index, song ->
            song.performerKeys.forEach { key ->
                firstByPerformer[key]?.let { union(it, index) } ?: firstByPerformer.put(key, index)
            }
            song.directory?.let { dir ->
                firstByDirectory[dir]?.let { union(it, index) } ?: firstByDirectory.put(dir, index)
            }
        }
        return songs.indices.groupBy(::find).values.map { indices -> indices.map(songs::get) }
    }

    /** 专辑艺人推断：合辑 → 曲目演唱者交集 → 不超过两位的首署名 → 群星 */
    private fun inferAlbumArtists(songs: List<Resolved>): List<String> {
        if (songs.any { it.song.isCompilation }) return listOf(VARIOUS_KEY)
        val first = songs.first().performers.map { it.key }
        val common = first.filter { key -> songs.all { song -> song.performers.any { it.key == key } } }
        if (common.isNotEmpty()) return common
        val leads = songs.map { it.performers.first().key }.distinct()
        val known = leads.filter { it != UNKNOWN_KEY }.ifEmpty { leads }
        return if (known.size <= 2) known else listOf(VARIOUS_KEY)
    }

    /** groupKey = 标题键|专辑艺人键；极少数撞键时按组内最小 songId 依次加序号 */
    private fun assignGroupKeys(groups: List<Group>): List<Pair<String, Group>> =
        groups
            .groupBy { group -> "${requireNotNull(group.songs.first().titleKey)}|${group.artistKeys.sorted().joinToString(",")}" }
            .flatMap { (baseKey, same) ->
                same.sortedBy { group -> group.songs.minOf { it.song.songId } }.mapIndexed { index, group ->
                    (if (index == 0) baseKey else "$baseKey#${index + 1}") to group
                }
            }

    private fun albumYear(songs: List<Resolved>): Int =
        songs
            .map { it.song.year }
            .filter { it > 0 }
            .groupingBy { it }
            .eachCount()
            .entries
            .sortedWith(compareByDescending<Map.Entry<Int, Int>> { it.value }.thenByDescending { it.key })
            .firstOrNull()
            ?.key ?: 0

    private fun mostCommon(values: List<String>): String =
        values
            .groupingBy { it }
            .eachCount()
            .entries
            .sortedWith(compareByDescending<Map.Entry<String, Int>> { it.value }.thenBy { it.key })
            .first()
            .key

    private fun isVarious(value: String): Boolean = text.key(value) in VARIOUS_ALIASES

    companion object {
        // 以控制字符开头，不会与真实名称的键冲突
        const val UNKNOWN_KEY = "\u0001unknown"
        const val VARIOUS_KEY = "\u0001various"
        const val DEFAULT_VARIOUS_NAME = "Various Artists"

        private val VARIOUS_ALIASES =
            setOf("various artists", "various artist", "various", "v.a.", "v.a", "va", "v/a", "群星", "多位艺术家", "多位艺人")

        private val FLAT_DIRECTORY_NAMES = setOf("music", "download", "downloads")
        private val STORAGE_ROOT = Regex("^/storage/(?:emulated/\\d+|[^/]+)$|^/sdcard$")

        /** 存储根目录和 Music / Download 目录里的文件不能靠目录判断是否同一张专辑 */
        fun directoryOf(path: String): String? {
            val dir = path.substringBeforeLast('/', "").trimEnd('/')
            if (dir.isEmpty() || STORAGE_ROOT.matches(dir)) return null
            if (dir.substringAfterLast('/').lowercase() in FLAT_DIRECTORY_NAMES) return null
            return dir
        }
    }
}
