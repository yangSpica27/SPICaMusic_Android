# 柠檬音乐 (SPICa Music) - Copilot 指南

## 构建、测试与格式化
- 使用 Gradle Wrapper：`./gradlew`，Windows 下等价命令是 `.\gradlew.bat`。
- 调试构建：`./gradlew :app:assembleDebug`
- Release 构建：`./gradlew :app:assembleRelease`
- Kotlin 格式化与检查：`./gradlew :app:ktlintFormat`、`./gradlew :app:ktlintCheck`
- App 单元测试：`./gradlew :app:testDebugUnitTest`
- 单个单元测试：`./gradlew :app:testDebugUnitTest --tests "me.spica27.spicamusic.ExampleUnitTest"`
- 连接设备/模拟器的仪器测试：`./gradlew :app:connectedDebugAndroidTest`
- 单个仪器测试：`./gradlew :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=me.spica27.spicamusic.ExampleInstrumentedTest`
- 其他模块也有 `:common:testDebugUnitTest`、`:storage-core:testDebugUnitTest`、`:player-core:testDebugUnitTest`、`:lyric-core:testDebugUnitTest` 任务，但当前仓库里实际提交的测试源码只在 `app/src/test` 和 `app/src/androidTest`。
- `app` 模块在 `preBuild` 前自动依赖 `:app:ktlintFormat`，所以构建 app 时会先格式化 Kotlin 源码。
- 版本号来自 `gradle.properties`：`versionCode = MAJOR_VERSION * 1_000_000 + MINOR_VERSION * 10_000 + BUILD_VERSION`；如需递增构建号，仓库里已有 `incrementBuildNumber` 任务。
- README 当前要求 Android Studio Narwhal 2025.1.1+ / JDK 21+；`app` 模块实际配置为 `compileSdk 37`、`minSdk 29`、`targetSdk 36`。

## 高层架构
- 这是一个明确分层的多模块 Android 音乐播放器：`app` 负责 Compose UI、导航、应用级 DI、网络配置、DataStore 设置和 `PlaybackService`；`storage-core`、`player-core`、`lyric-core` 提供能力实现；`common` 放跨模块共享实体。
- 应用入口 `App` 会一次性启动四个 Koin 模块：`storageModule`、`SpicaPlayer.createModule(PlaybackService::class.java)`、`AppModule.appModule`、`extraInfoModule`。要理解依赖从哪里来，先看这里。
- `storage-core` 以 `storage/api` 暴露仓库接口，在 `storage/impl/di/StorageModule.kt` 里绑定 Room 数据库、DAO、仓库实现和 `IMusicScanService`。MediaStore 扫描与数据库更新都在这个模块闭环。
- `player-core` 公开的是 `IMusicPlayer`、`PlayerAction`、`PlayMode` 等 API；实现侧的 `SpicaPlayer` 不是直接持有 ExoPlayer，而是一个懒初始化的 `MediaBrowser` 客户端，连接到 `app` 模块里的 `PlaybackService`。
- 真正的 ExoPlayer、MediaSession、MediaLibraryService 以及音频处理链（FFT -> EQ -> Reverb）都在 `PlaybackService` 中装配；`SpicaPlayer` 负责把这些能力包装成状态流和动作接口给 UI/ViewModel 使用。
- `lyric-core` 只提供歌词 API 客户端和对应 DI；`Retrofit`、`OkHttpClient` 和歌词服务 `baseUrl` 实际定义在 `app/src/main/java/me/spica27/spicamusic/di/AppModule.kt`，所以歌词网络相关改动通常会跨 `app` 与 `lyric-core`。
- `common` 里的实体会跨层传递，甚至直接出现在导航参数里，例如 `Screen.AlbumDetail(val album: Album)`；改动共享实体时要同时考虑导航序列化、数据库映射和 UI 消费方。

## 关键约定
- 保持依赖方向：`app -> {storage-core, player-core, lyric-core} -> common`。不要把 `app` 里的类型或实现反向拉进 core 模块。
- 新的数据能力按这条路径走：先改 `storage-core/api` 接口，再补 `storage-core/impl` 实现与 Koin 绑定，最后在 `AppModule` 注入到对应 ViewModel。
- 新的播放能力按这条路径走：先扩展 `PlayerAction` 或 `IMusicPlayer`，再在 `SpicaPlayer` 处理，UI/ViewModel 侧继续只调用 `player.doAction(...)`，不要直接碰 `MediaBrowser` 或 `ExoPlayer`。
- 导航使用 Navigation 3 的类型安全路由：在 `Screen` 里新增 `@Serializable` 条目后，必须同步在 `AppNavGraph` 的 `entryProvider` 注册页面，否则路由不会生效。
- 详情页 ViewModel 普遍使用 Koin 参数注入，并带稳定 key，例如 `koinViewModel(key = "PlaylistDetailViewModel_$playlistId") { parametersOf(playlistId) }`；同类页面新增参数化 ViewModel 时沿用这个模式，避免实例串用。
- Compose 页面主要在 `app/src/main/java/me/spica27/spicamusic/ui/**`，可复用组件集中在 `app/src/main/java/me/spica27/spicamusic/ui/widget`；不要把业务逻辑塞回 Composable，本仓库默认通过 ViewModel 持有状态与接口依赖。
- 设置持久化统一走 `PreferencesManager` 的 DataStore Flow；像 `AudioEffectsViewModel` 这样的页面会先写入偏好，再通过 Flow 收集结果把 EQ/混响应用到播放器，新增设置时优先复用这个模式。
- `App` 会按应用前后台生命周期启动/停止 `IMusicScanService` 的 MediaStore 监听，这不是某个页面局部逻辑；涉及扫描或库刷新时要考虑生命周期影响。
- `SpicaPlayer.createModule(PlaybackService::class.java)` 是 app 与 player-core 的接缝点；如果播放器能力改动需要依赖服务类型或 DSP 处理链，优先从这里往两边追踪。
