# 歌曲排序和筛选 API 使用指南

## 概述

在 `storage-core` 模块中新增了歌曲排序和筛选功能，支持灵活的查询需求。

## 核心类型

### 1. SongSortOrder (排序枚举)

位置：`common/entity/SongSortOrder.kt`

```kotlin
enum class SongSortOrder {
    DISPLAY_NAME_ASC,      // 按歌名升序
    DISPLAY_NAME_DESC,     // 按歌名降序
    ARTIST_ASC,            // 按艺术家升序
    ARTIST_DESC,           // 按艺术家降序
    DURATION_ASC,          // 按时长升序
    DURATION_DESC,         // 按时长降序
    SIZE_ASC,              // 按大小升序
    SIZE_DESC,             // 按大小降序
    DATE_ADDED_ASC,        // 按添加时间升序
    DATE_ADDED_DESC,       // 按添加时间降序
    PLAY_COUNT_DESC,       // 按播放次数降序
    RANDOM,                // 随机排序
    DEFAULT                // 默认顺序
}
```

### 2. SongFilter (筛选条件)

位置：`common/entity/SongFilter.kt`

```kotlin
data class SongFilter(
    val keyword: String? = null,              // 搜索关键词（歌名/艺术家）
    val minDuration: Long? = null,            // 最小时长（毫秒）
    val maxDuration: Long? = null,            // 最大时长（毫秒）
    val minSize: Long? = null,                // 最小文件大小（字节）
    val maxSize: Long? = null,                // 最大文件大小（字节）
    val artists: List<String>? = null,        // 指定艺术家列表
    val albums: List<Long>? = null,           // 指定专辑ID列表
    val mimeTypes: List<String>? = null,      // 指定MIME类型列表
    val onlyLiked: Boolean = false,           // 仅显示喜欢的歌曲
    val excludeIgnored: Boolean = true        // 排除已忽略的歌曲
)
```

## API 方法

### ISongRepository 新增方法

```kotlin
interface ISongRepository {
    /**
     * 获取歌曲列表（支持排序和筛选）
     */
    fun getSongsFlow(
        sortOrder: SongSortOrder = SongSortOrder.DEFAULT,
        filter: SongFilter = SongFilter.EMPTY
    ): Flow<List<Song>>

    /**
     * 获取歌曲列表（同步）
     */
    suspend fun getSongs(
        sortOrder: SongSortOrder = SongSortOrder.DEFAULT,
        filter: SongFilter = SongFilter.EMPTY
    ): List<Song>

    /**
     * 搜索歌曲
     */
    fun searchSongsFlow(
        keyword: String,
        sortOrder: SongSortOrder = SongSortOrder.DEFAULT
    ): Flow<List<Song>>
}
```

## 使用示例

### 1. 基础排序

```kotlin
// 按歌名升序
val songs = songRepository.getSongsFlow(sortOrder = SongSortOrder.DISPLAY_NAME_ASC)

// 按时长降序
val songs = songRepository.getSongsFlow(sortOrder = SongSortOrder.DURATION_DESC)
```

### 2. 关键词搜索

```kotlin
// 搜索包含"love"的歌曲，按艺术家排序
val results = songRepository.searchSongsFlow(
    keyword = "love",
    sortOrder = SongSortOrder.ARTIST_ASC
)
```

### 3. 复合筛选

```kotlin
// 筛选时长3-5分钟的喜欢的歌曲
val filter = SongFilter(
    minDuration = 3 * 60 * 1000L,      // 3分钟
    maxDuration = 5 * 60 * 1000L,      // 5分钟
    onlyLiked = true
)
val songs = songRepository.getSongsFlow(
    sortOrder = SongSortOrder.DURATION_ASC,
    filter = filter
)
```

### 4. 指定艺术家

```kotlin
// 获取指定艺术家的歌曲
val filter = SongFilter(
    artists = listOf("Taylor Swift", "Ed Sheeran")
)
val songs = songRepository.getSongsFlow(filter = filter)
```

### 5. 文件大小筛选

```kotlin
// 筛选大于10MB的无损音乐
val filter = SongFilter(
    minSize = 10 * 1024 * 1024L,       // 10MB
    mimeTypes = listOf("audio/flac", "audio/x-flac")
)
val songs = songRepository.getSongsFlow(filter = filter)
```

### 6. 在 ViewModel 中使用

```kotlin
class HomeViewModel(private val songRepository: ISongRepository) : ViewModel() {
    
    private val _sortOrder = MutableStateFlow(SongSortOrder.DEFAULT)
    private val _filter = MutableStateFlow(SongFilter.EMPTY)
    
    val songs: StateFlow<List<Song>> = combine(_sortOrder, _filter) { sort, filter ->
        songRepository.getSongsFlow(sort, filter)
    }.flattenConcat().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )
    
    fun updateSortOrder(order: SongSortOrder) {
        _sortOrder.value = order
    }
    
    fun updateFilter(filter: SongFilter) {
        _filter.value = filter
    }
}
```

### 7. UI 中使用

```kotlin
@Composable
fun SongListScreen(viewModel: HomeViewModel = koinViewModel()) {
    val songs by viewModel.allSongs.collectAsState()
    val sortOrder by viewModel.sortOrder.collectAsState()
    
    Column {
        // 排序按钮
        Row {
            TextButton(onClick = { 
                viewModel.updateSortOrder(SongSortOrder.DISPLAY_NAME_ASC) 
            }) {
                Text("按名称")
            }
            TextButton(onClick = { 
                viewModel.updateSortOrder(SongSortOrder.DURATION_DESC) 
            }) {
                Text("按时长")
            }
        }
        
        // 筛选按钮
        TextButton(onClick = {
            viewModel.updateFilter(SongFilter(onlyLiked = true))
        }) {
            Text("仅喜欢的")
        }
        
        // 歌曲列表
        LazyColumn {
            items(songs) { song ->
                SongItem(song)
            }
        }
    }
}
```

## 性能注意事项

1. **排序在应用层完成**：排序逻辑在 Kotlin 代码中执行，对于大量数据可能有性能影响
2. **筛选优先使用数据库**：关键词搜索、喜欢状态等通过 SQL 筛选，效率较高
3. **复合条件性能**：多个筛选条件组合时，建议使用 `Flow` 避免阻塞主线程
4. **缓存策略**：使用 `stateIn` 配合 `WhileSubscribed` 实现自动缓存和取消

## 扩展建议

### 未来可能的增强

1. **播放次数排序**：需要关联 `PlayHistory` 表实现
2. **专辑分组**：添加 `groupByAlbum()` 方法
3. **艺术家分组**：添加 `groupByArtist()` 方法
4. **模糊匹配增强**：支持拼音首字母、编辑距离等
5. **数据库层排序**：将常用排序迁移到 SQL 层提升性能

### 自定义排序

```kotlin
// 扩展函数实现自定义排序
fun List<Song>.customSort(): List<Song> {
    return this.sortedWith(
        compareByDescending<Song> { it.like }       // 喜欢的在前
            .thenByDescending { it.playCount }       // 播放次数高的在前
            .thenBy { it.displayName }               // 最后按名称
    )
}
```
