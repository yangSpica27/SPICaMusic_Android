# 柠檬音乐 (SPICa Music) - Copilot 开发指南

## 项目概述

现代化 Android 音乐播放器，采用 Jetpack Compose + Media3 ExoPlayer + Koin DI 架构。支持多种音频格式 (FLAC/ALAC/Opus等)，提供 EQ 调节、歌词显示、歌单管理等功能。

**✅ 模块化架构已完成**: 核心逻辑已成功解耦到独立模块，应用层仅依赖接口。

## 核心架构

### 🏗️ 模块化架构

项目采用多模块架构，清晰分离关注点：

```
SPICaMusic_Android/
├── app/                  # UI层 - Compose界面、导航、主题、ViewModels
├── common/               # 共享模块 - 通用实体类和工具 (LrcParser等)
├── storage-core/         # 存储模块 - Room数据库、Repository接口与实现
├── player-core/          # 播放器模块 - Media3封装、播放控制、音频处理
└── lyric-core/           # 歌词模块 - 网络API、歌词搜索服务
```

**依赖原则**:
- **面向接口编程**: app 层通过接口（`ISongRepository`、`IMusicPlayer`）依赖功能模块
- **单向依赖**: app → storage-core/player-core/lyric-core → common
- **职责分离**: 核心逻辑（存储/播放/歌词）与UI完全解耦

**接口定义位置**:
- `storage-core/api/`: 数据仓库接口 (`ISongRepository`, `IPlaylistRepository`, `IMusicScanService`)
- `player-core/api/`: 播放器接口 (`IMusicPlayer`, `PlayerAction`, `PlayMode`, `IFFTProcessor`)

### 依赖注入 (Koin)

**模块化 DI 架构**:
```kotlin
// App.kt 初始化
startKoin {
    modules(
        storageModule,           // storage-core 提供 (ISongRepository等)
        SpicaPlayer.createModule(PlaybackService::class.java),  // player-core
        AppModule.appModule,     // app 模块 - 网络、ViewModels
        extraInfoModule,         // lyric-core - 歌词API
    )
}
```

- 所有接口实现在各自模块的 `impl/di/` 目录定义
- ViewModels 在 `app/di/AppModule.appModule` 注册
- 使用 `viewModel { }` DSL 定义，`koinViewModel<>()` 获取实例
- ViewModel 通过构造函数注入接口依赖:
  ```kotlin
  class SongViewModel(
      private val songRepo: ISongRepository,  // 接口
      private val player: IMusicPlayer        // 接口
  ) : ViewModel()
  ```

### 导航系统 (Navigation 3)
- 路由定义在 [navigation/Screen.kt](app/src/main/java/me/spica27/spicamusic/navigation/Screen.kt) 使用 `@Serializable` + `NavKey` 接口
- [navigation/NavGraph.kt](app/src/main/java/me/spica27/spicamusic/navigation/NavGraph.kt) 的 `NavDisplay` + `entryProvider` 配置所有路由
- 通过 `LocalNavBackStack.current` 访问全局 NavBackStack
- 导航调用: `backStack.push(Screen.YourScreen)` / `backStack.pop()`

### 播放器核心

**接口定义**: [player-core/api/IMusicPlayer.kt](player-core/src/main/java/me/spica27/spicamusic/player/api/IMusicPlayer.kt)

- 实现类: [player-core/impl/SpicaPlayer.kt](player-core/src/main/java/me/spica27/spicamusic/player/impl/SpicaPlayer.kt)
- 通过 `IMusicPlayer` 接口访问播放功能，所有操作通过 `doAction(PlayerAction)` 派发:
  ```kotlin
  player.doAction(PlayerAction.Play)
  player.doAction(PlayerAction.PlayById("mediaId"))
  player.doAction(PlayerAction.SetPlayMode(PlayMode.SHUFFLE))
  player.doAction(PlayerAction.SeekTo(positionMs))
  ```
- 状态通过 StateFlow 暴露: `isPlaying`, `currentMediaItem`, `playMode`, `currentDuration`, etc.
- 实现层使用 Media3 `MediaBrowser` + `PlaybackService` 后台播放
- 音频处理器: FFT 分析器通过 `fftProcessor` 和 `fftAudioProcessor` 属性访问

### 数据持久化

**接口定义**: [storage-core/api/](storage-core/src/main/java/me/spica27/spicamusic/storage/api/)

- 通过仓库接口访问数据:
  - `ISongRepository`: 歌曲数据 CRUD
  - `IPlaylistRepository`: 歌单管理
  - `IPlayHistoryRepository`: 播放历史
  - `IMusicScanService`: 音乐扫描服务
  
- 实现层 (storage-core/impl):
  - Room 数据库版本 3，定义在 [storage-core/impl/db/AppDatabase.kt](storage-core/src/main/java/me/spica27/spicamusic/storage/impl/db/AppDatabase.kt)
  - 5 个实体: `SongEntity`, `PlaylistEntity`, `PlaylistSongCrossRefEntity`, `ExtraInfoEntity`(歌词), `PlayHistoryEntity`
  - Mapper 负责 Entity ↔ Common Entity 转换 (在 `storage-core/impl/mapper/` 目录)
  - DAO 接口在 `storage-core/impl/dao/` 目录

- DataStore 用于键值存储 (主题设置等)，封装在 `app/utils/PreferencesManager.kt`

## 开发规范

### Compose UI 规范
- 所有 UI 使用 Jetpack Compose，无 XML 布局
- 屏幕级 Composable 放在 `ui/<feature_name>/` 目录，以 `Screen.kt` 结尾
- 使用 Material3 组件，主题在 [theme/](app/src/main/java/me/spica27/spicamusic/theme/) 目录
- 自定义组件放在 [widget/](app/src/main/java/me/spica27/spicamusic/widget/) 目录

### 音频处理
- 自定义 DSP 处理器在 [player-core/impl/dsp/](player-core/src/main/java/me/spica27/spicamusic/player/impl/dsp/) 目录
  - `FFTAudioProcessor`: FFT 频谱分析 (可视化用)
  - `FFTAudioProcessorWrapper`: 将 FFT 处理器包装为 Media3 `AudioProcessor`
  - 注意: EQ、ReplayGain 等其他处理器可能在其他目录
- FFMPEG 解码器 (`app/libs/media3-decode-ffmpeg-1.9.0.aar`) 支持 ALAC/WAV/Opus 等格式
- TarsosDSP 用于音频分析，使用 Amplituda 库获取音频振幅

### 网络请求
- Retrofit + OkHttp + Moshi + Sandwich (ApiResponse 封装)
- 歌词API基础 URL: `http://api.spica27.site/api/v1/lyrics/`
- 超时配置: 3000ms (connect/read/write/call)
- 网络模块在 `app/di/AppModule.appModule` 中配置
- 歌词服务在 `lyric-core/` 模块，通过 `ApiClient` 访问

## 关键工作流

### 构建与运行
```bash
# 标准构建
./gradlew assembleDebug

# Release 构建 (minSdk 29, targetSdk 36, compileSdk 36)
./gradlew assembleRelease

# KtLint 代码格式化 (构建前自动执行)
./gradlew ktlintFormat
```

### 版本管理
- 版本号在 [gradle.properties](gradle.properties) 中配置:
  - `MAJOR_VERSION`: 主版本号
  - `MINOR_VERSION`: 次版本号
  - `BUILD_VERSION`: 构建版本号
- versionCode 计算: `MAJOR * 1,000,000 + MINOR * 10,000 + BUILD`
- versionName 格式: `MAJOR.MINOR.BUILD BETA`

### 添加新的数据操作
1. 在 `storage-core/api/I*Repository.kt` 接口添加方法声明
2. 在 `storage-core/impl/repository/*RepositoryImpl.kt` 实现方法
3. 在 ViewModel 中通过接口调用: `songRepo.yourNewMethod()`

### 添加新的播放器功能
1. 在 `player-core/api/PlayerAction.kt` 添加新的 Action 类型
2. 在 `player-core/impl/` 的播放器实现中处理新 Action
3. 在 UI 中调用: `player.doAction(YourNewAction())`

### 添加新 ViewModel
1. 创建 ViewModel 类，通过构造函数注入接口依赖
   ```kotlin
   class MyViewModel(
       private val songRepo: ISongRepository,  // 接口依赖
       private val player: IMusicPlayer
   ) : ViewModel()
   ```
2. 在 `app/di/AppModule.appModule` 中注册:
   ```kotlin
   viewModel { MyViewModel(get(), get()) }
   ```
3. 在 Composable 中获取: `koinViewModel<MyViewModel>()`

### 添加数据库字段
1. 修改 `common/entity/` 中的通用实体类
2. 修改 `storage-core/impl/entity/` 中的 Room Entity
3. 更新 Mapper 转换逻辑 (`storage-core/impl/mapper/`)
4. 递增 `storage-core/impl/db/AppDatabase` 版本号
5. 创建 Migration 对象处理升级逻辑

### 添加新路由
1. 在 `navigation/Screen.kt` 添加新的 `@Serializable` sealed interface 实现
2. 在 `navigation/NavGraph.kt` 的 `entryProvider` 中添加 `entry<Screen.YourScreen> { YourScreen() }`
3. 使用 `backStack.push(Screen.YourScreen)` 导航

### NDK/Native 支持
- 仅构建 `arm64-v8a` ABI (minSdk 24)
- JNI 库放在 `app/libs/` 目录
- TarsosDSP (`TarsosDSP-Android-latest.jar`) 用于音频分析

### ProGuard 规则
- Release 构建启用混淆 + 资源压缩
- 保留规则: Koin 注解、网络 DTO (`network/**`)、Sandwich ApiResponse
- 配置在 [app/proguard-rules.pro](app/proguard-rules.pro)

## 项目特点

- **纯 Kotlin 实现**: 无 Java 代码，使用协程处理异步
- **类型安全导航**: Kotlin 序列化 + Navigation 3 + 编译时类型检查
- **模块化架构**: 核心逻辑与UI分离，面向接口编程
- **自定义 DSP**: 完全控制音频处理链 (非依赖第三方 EQ 库)
- **离线优先**: 主要使用 MediaStore，无依赖云端音乐 API

## 已知限制

- 歌曲扫描仅通过 MediaStore，无自定义目录支持
- 歌词搜索服务可能超时 (3s 限制)
- 歌词组件滑动调整播放位置功能待实现
- 歌单批量编辑功能缺失
