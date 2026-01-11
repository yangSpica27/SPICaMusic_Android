# 柠檬音乐 (SPICa Music) - Copilot 开发指南

## 项目概述

现代化 Android 音乐播放器，采用 Jetpack Compose + Media3 ExoPlayer + Koin DI 架构。支持多种音频格式 (FLAC/ALAC/Opus等)，提供 EQ 调节、歌词显示、歌单管理等功能。

**🎯 模块化重构中**: 项目正在进行模块化改造，将播放器和存储逻辑抽离为独立模块。详见 [MODULARIZATION.md](../MODULARIZATION.md) 和 [MIGRATION_GUIDE.md](../MIGRATION_GUIDE.md)。

## 核心架构

### 🏗️ 模块化架构（新）

项目采用多模块架构，清晰分离关注点：

```
SPICaMusic_Android/
├── app/                  # UI层 - Compose界面、导航、主题
├── common/               # 共享模块 - 通用实体类和工具
├── storage-core/         # 存储模块 - Room数据库、Repository接口
└── player-core/          # 播放器模块 - Media3封装、播放控制
```

**依赖原则**:
- **面向接口编程**: app 层通过接口（`ISongRepository`、`IMusicPlayer`）依赖功能模块
- **单向依赖**: app → storage-core/player-core → common
- **职责分离**: 核心逻辑（存储/播放）与UI完全解耦

**接口定义位置**:
- `storage-core/api/`: 数据仓库接口 (`ISongRepository`, `IPlaylistRepository`, etc.)
- `player-core/api/`: 播放器接口 (`IMusicPlayer`, `PlayerAction`, `PlayMode`)

### 依赖注入 (Koin)

**模块化 DI 架构**:
```kotlin
// App.kt 初始化
startKoin {
    modules(
        storageModule,    // storage-core 提供 (ISongRepository等)
        playerModule,     // player-core 提供 (IMusicPlayer)
        networkModule,    // app 模块 - 网络请求
        viewModelModule,  // app 模块 - ViewModels
    )

**接口定义**: [player-core/api/IMusicPlayer.kt](player-core/src/main/java/me/spica27/spicamusic/player/api/IMusicPlayer.kt)

- 通过 `IMusicPlayer` 接口访问播放功能
- 所有操作通过 `doAction(PlayerAction)` 派发
  ```kotlin
  player.doAction(PlayerAction.Play)
  player.doAction(PlayerAction.PlayById("mediaId"))
  player.doAction(PlayerAction.SetPlayMode(PlayMode.SHUFFLE))
  ```
- 状态通过 StateFlow 暴露: `isPlaying`, `currentMediaItem`, `playMode`, etc.
- 实现层使用 Media3 `MediaBrowser` + `PlaybackService` 后台播放
- 启动流程: `MainActivity.onCreate()` → `doOnMainThreadIdle` → `p
class SongViewModel(
    private val songRepo: ISongRepository,  // 接口
    private val player: IMusicPlayer        // 接口
) : ViewModel()
```

- 所有接口实现在各自模块的 `impl/di/` 目录定义
- ViewModels 在 `app/module/InjectModules.viewModelModule` 注册
- 使用 `viewModel { }` DSL 定义，`koinViewModel<>()` 获取实例

### 导航系统
- 路由定义在 [route/Routes.kt](app/src/main/java/me/spica27/spicamusic/route/Routes.kt) 使用 `@Serializable` + Kotlin 序列化
- [ui/AppMain.kt](app/src/main/java/me/spica27/spicamusic/ui/AppMain.kt) 的 NavHost 配置所有路由，使用 Material SharedAxis 转场动画
- 通过 `LocalNavController.current` 访问全局 NavController

### 播放器核心
- [media/SpicaPlayer.kt](app/src/main/java/me/spica27/spicamusic/media/SpicaPlayer.kt) 是播放控制的核心类

**接口定义**: [storage-core/api/](storage-core/src/main/java/me/spica27/spicamusic/storage/api/)

- 通过仓库接口访问数据:
  - `ISongRepository`: 歌曲数据 CRUD
  - `IPlaylistRepository`: 歌单管理
  - `ILyricRepository`: 歌词存储
  - `IPlayHistoryRepository`: 播放历史
  
- 实现层 (storage-core/impl):
  - Room 数据库版本 17
  - 5 个实体: `SongEntity`, `PlaylistEntity`, `PlaylistSongCrossRefEntity`, `LyricEntity`, `PlayHistoryEntity`
  - Mapper 负责 Entity ↔ Common Entity 转换
  - 数据库迁移: MIGRATION_12_13/13_14/16_17

- DataStore 用于键值存储 (主题设置等)
- 启动流程: `MainActivity.onCreate()` → `doOnMainThreadIdle` → `SpicaPlayer.init()`

### 数据持久化
- Room 数据库版本 17，定义在 [db/AppDatabase.kt](app/src/main/java/me/spica27/spicamusic/db/AppDatabase.kt)
- 包含 5 个实体: `Song`、`Playlist`、`PlaylistSongCrossRef`、`Lyric`、`PlayHistory`
- 数据库迁移使用 Migration 对象 (MIGRATION_12_13/13_14/16_17)
- DataStore 用于键值存储 (主题设置等)，封装在 `utils/DataStoreUtil.kt`

## 开发规范

### Compose UI 规范
- 所有 UI 使用 Jetpack Compose，无 XML 布局
- 屏幕级 Composable 放在 `ui/<feature_name>/` 目录，以 `Screen.kt` 结尾
- 使用 Material3 组件，主题在 [theme/](app/src/main/java/me/spica27/spicamusic/theme/) 目录
- 自定义组件放在 [widget/](app/src/main/java/me/spica27/spicamusic/widget/) 目录

### 音频处理
- 自定义 DSP 处理器在 [dsp/](app/src/main/java/me/spica27/spicamusic/dsp/) 目录
  - `EqualizerAudioProcessor`: 10段 EQ 均衡器
  - `ReplayGainAudioProcessor`: ReplayGain 增益控制
  - `FFTAudioProcessor`: 频谱分析 (可视化用)
- FFMPEG 解码器 (`libs/media3-decode-ffmpeg-1.8.0.aar`) 支持 ALAC/WAV 等格式
- TagLib (`libs/taglib_1.0.2.aar`) 用于读取音频元数据

### 网络请求
- Retrofit + OkHttp + Moshi + Sandwich (ApiResponse 封装)
- 基础 URL: `http://api.spica27.site/api/v1/lyrics/`
- 超时配置: 3000ms (connect/read/write/call)
- 网络模块在 `InjectModules.networkModule` 中配置

## 关键工作流

### 构建与运行
```bash
# 标准构建
./gradlew assembleDebug

# Release 构建 (需 key.jks)
./gradlew assembleRelease


### 添加新的数据操作
1. 在 `storage-core/api/I*Repository.kt` 接口添加方法声明
2. 在 `storage-core/impl/repository/*RepositoryImpl.kt` 实现方法
3. 在 ViewModel 中通过接口调用: `songRepo.yourNewMethod()`

### 添加新的播放器功能
1. 在 `player-core/api/PlayerAction.kt` 添加新的 Action 类型
2. 在 `player-core/impl/` 的播放器实现中处理新 Action
3. 在 UI 中调用: `player.doAction(YourNewAction())`
# KtLint 代码格式化 (构建前自动执行)
./gradlew ktlintFormat
```

### 版本管理，通过构造函数注入接口依赖
   ```kotlin
   class MyViewModel(
       private val songRepo: ISongRepository,  // 接口依赖
       private val player: IMusicPlayer
   )  `common/entity/` 中的通用实体类
2. 修改 `storage-core/impl/entity/` 中的 Room Entity
3. 更新 Mapper 转换逻辑
4. 递增 `storage-core/impl/db/AppDatabase` 版本号
5. 创建 Migration 对象处理升级
6  ```kotlin
   viewModel { MyViewModel(get(), get()) }
   ```
3. 在 Composable 中获取: `koinViewModel<MyViewModel>()`

### NDK/Native 支持
- 仅构建 `arm64-v8a` ABI (minSdk 24)
- JNI 库放在 `app/libs/` 目录
- TarsosDSP (`TarsosDSP-Android-latest.jar`) 用于音频分析

### ProGuard 规则
- Release 构建启用混淆 + 资源压缩
- 保留规则: Koin 注解、网络 DTO (`network/**`)、Sandwich ApiResponse
- 配置在 [a在 `player-core/api/PlayerAction.kt` 添加新 Action
- 音频处理: 在 `player-core/impl/dsp/` 实现 `AudioProcessor` 接口
- 播放模式: 修改 `player-core/api/PlayMode.kt

### 添加新路由
1. 在 `Routes` 对象添加 `@Serializable` data class/object
2. 在 `AppMain.kt` 的 `NavHost` 添加 `composable<Routes.YourRoute>`
3. 使用 `navController.navigate(Routes.YourRoute())` 导航
架构**: 核心逻辑与UI分离，面向接口编程
- **自定义 DSP**: 完全控制音频处理链 (非依赖第三方 EQ 库)
- **离线优先**: 主要使用 MediaStore，无依赖云端音乐 API

## 模块化重构状态

**已完成**:
- ✅ 创建 common、storage-core、player-core 模块
- ✅ 定义所有接口和通用实体类
- ✅ 配置 Gradle 依赖和构建脚本
- ✅ 创建 Koin 依赖注入配置示例

**进行中**:
- 🔄 迁移 Room 数据库代码到 storage-core/impl
- 🔄 迁移播放器代码到 player-core/impl
- 🔄 更新 app 模块使用接口而非实现

**详细指南**: 参考 [MODULARIZATION.md](../MODULARIZATION.md) 和 [MIGRATION_GUIDE.md](../MIGRATION_GUIDE.md)
2. 在 `InjectModules.viewModelModule` 使用 `viewModel { YourViewModel(get()) }` 注册
3. 在 Composable 中使用 `koinViewModel<YourViewModel>()` 获取实例

### 添加数据库字段
1. 修改实体类 (`db/entity/`)
2. 递增 `AppDatabase` 版本号
3. 创建 Migration 对象处理升级逻辑
4. 在 `AppDatabase` 的 `addMigrations()` 添加新 Migration

### 修改播放器行为
- 播放控制: 创建新 `Action` 子类，在 `SpicaPlayer.doAction()` 处理
- 音频处理: 实现 `AudioProcessor` 接口，在 ExoPlayer 链中注册
- 播放模式: 修改 `media/common/PlayMode.kt` 和 `PlayerKVUtils`

## 项目特点

- **纯 Kotlin 实现**: 无 Java 代码，使用协程处理异步
- **类型安全导航**: Kotlin 序列化 + 编译时类型检查
- **模块化 DI**: 业务逻辑通过 Koin 模块解耦
- **自定义 DSP**: 完全控制音频处理链 (非依赖第三方 EQ 库)
- **离线优先**: 主要使用 MediaStore，无依赖云端音乐 API

## 已知限制

- 歌曲扫描仅通过 MediaStore，无自定义目录支持
- 歌词搜索服务可能超时 (3s 限制)
- 歌词组件滑动调整播放位置功能待实现
- 歌单批量编辑功能缺失
