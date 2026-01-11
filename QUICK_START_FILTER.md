# 歌曲排序筛选 - 快速开始

## 功能概览

已在 `storage-core` 模块实现了歌曲排序和筛选功能，支持：

✅ **14种排序方式** - 名称、艺术家、时长、大小、添加时间、播放次数、随机等  
✅ **多维度筛选** - 关键词搜索、时长范围、文件大小、艺术家、专辑、MIME类型  
✅ **响应式设计** - Flow + StateFlow 自动更新UI  
✅ **性能优化** - 数据库层搜索 + 应用层排序筛选

## 快速使用

### 1. ViewModel中使用（推荐）

`HomeViewModel` 已集成排序筛选功能：

```kotlin
@Composable
fun MyScreen() {
    val viewModel: HomeViewModel = koinViewModel()
    val songs by viewModel.allSongs.collectAsState()
    
    // 切换排序
    Button(onClick = { viewModel.updateSortOrder(SongSortOrder.DURATION_DESC) }) {
        Text("按时长排序")
    }
    
    // 筛选喜欢的歌曲
    Button(onClick = { 
        viewModel.updateFilter(SongFilter(onlyLiked = true)) 
    }) {
        Text("只看喜欢的")
    }
    
    // 歌曲列表
    LazyColumn {
        items(songs) { song ->
            Text(song.displayName)
        }
    }
}
```

### 2. 直接使用Repository

```kotlin
class MyViewModel(private val songRepo: ISongRepository) : ViewModel() {
    
    // 获取按名称排序的歌曲
    val songsByName = songRepo.getSongsFlow(
        sortOrder = SongSortOrder.DISPLAY_NAME_ASC
    )
    
    // 搜索"love"相关歌曲
    val searchResults = songRepo.searchSongsFlow(
        keyword = "love",
        sortOrder = SongSortOrder.ARTIST_ASC
    )
    
    // 筛选3-5分钟的歌曲
    val mediumLengthSongs = songRepo.getSongsFlow(
        filter = SongFilter(
            minDuration = 180_000L, // 3分钟
            maxDuration = 300_000L  // 5分钟
        )
    )
}
```

## 常用场景

### 搜索功能
```kotlin
fun onSearchQueryChanged(query: String) {
    viewModel.updateFilter(
        SongFilter(keyword = query)
    )
}
```

### 分类浏览
```kotlin
// 仅显示FLAC格式
viewModel.updateFilter(
    SongFilter(mimeTypes = listOf("audio/flac"))
)

// 指定艺术家
viewModel.updateFilter(
    SongFilter(artists = listOf("Taylor Swift"))
)
```

### 排序切换
```kotlin
enum class SortOption {
    NAME, ARTIST, DURATION, DATE
}

fun onSortSelected(option: SortOption) {
    val order = when(option) {
        SortOption.NAME -> SongSortOrder.DISPLAY_NAME_ASC
        SortOption.ARTIST -> SongSortOrder.ARTIST_ASC
        SortOption.DURATION -> SongSortOrder.DURATION_DESC
        SortOption.DATE -> SongSortOrder.DATE_ADDED_DESC
    }
    viewModel.updateSortOrder(order)
}
```

## API文档

详细文档请参考：[SONG_FILTER_API.md](SONG_FILTER_API.md)

## 文件结构

```
common/
├── entity/
│   ├── SongSortOrder.kt    # 排序枚举
│   └── SongFilter.kt       # 筛选条件

storage-core/
├── api/
│   └── ISongRepository.kt  # 新增3个API方法
└── impl/
    ├── dao/SongDao.kt      # 新增搜索查询
    └── repository/
        └── SongRepositoryImpl.kt  # 实现排序筛选逻辑

app/
└── ui/home/
    └── HomeViewModel.kt    # 使用示例
```

## 性能说明

- **关键词搜索**: SQL LIKE查询，效率高
- **排序**: 内存排序，数据量<1000时性能优异
- **筛选**: Kotlin集合操作，延迟执行
- **响应式**: 仅在数据或条件变化时重新计算

## 后续扩展

可根据需要添加：
- 拼音搜索支持
- 播放次数排序（需关联播放历史）
- 专辑/艺术家分组
- 保存筛选预设
