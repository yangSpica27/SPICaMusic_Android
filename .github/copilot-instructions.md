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
- 其他模块也有 `:common:testDebugUnitTest`、`:feature-library-data:testDebugUnitTest`、`:feature-player-data:testDebugUnitTest`、`:feature-lyrics-data:testDebugUnitTest` 任务，但当前仓库里实际提交的测试源码只在 `app/src/test` 和 `app/src/androidTest`。
- `app` 模块在 `preBuild` 前自动依赖 `:app:ktlintFormat`，所以构建 app 时会先格式化 Kotlin 源码。
- 版本号来自 `gradle.properties`：`versionCode = MAJOR_VERSION * 1_000_000 + MINOR_VERSION * 10_000 + BUILD_VERSION`；如需递增构建号，仓库里已有 `incrementBuildNumber` 任务。
- README 当前要求 Android Studio Narwhal 2025.1.1+ / JDK 21+；`app` 模块实际配置为 `compileSdk 37`、`minSdk 29`、`targetSdk 37`。
- CI 的 release workflow 在 `.github/workflows/android.yml` 中执行 `assembleRelease`，当前使用 Temurin 11。

## 高层架构
- 这是一个明确分层的多模块 Android 音乐播放器：`app` 负责 Compose UI、导航、应用级 DI、网络配置和 `PlaybackService`；`feature-library-data`、`feature-player-data`、`feature-lyrics-data` 提供数据与能力实现；对应的 `feature-*-domain` 提供 facade / use case；`common` 放跨模块共享实体。
- 应用入口 `App` 会一次性启动全部 Koin 模块：`storageModule`、`SpicaPlayer.createModule(PlaybackService::class.java)`、`libraryDomainModule`、`playerDomainModule`、`settingsDomainModule`、`lyricsDomainModule`、`AppModule.appModule`、`extraInfoModule`。要理解依赖从哪里来，先看这里。
- `feature-library-data` 以 `storage/api` 暴露仓库接口，在 `storage/impl/di/StorageModule.kt` 里绑定 Room 数据库、DAO、仓库实现和 `IMusicScanService`。MediaStore 扫描与数据库更新都在这个模块闭环。
- `feature-player-data` 公开的是 `IMusicPlayer`、`PlayerAction`、`PlayMode` 等 API；实现侧的 `SpicaPlayer` 不是直接持有 ExoPlayer，而是一个懒初始化的 `MediaBrowser` 客户端，连接到 `app` 模块里的 `PlaybackService`。
- 真正的 ExoPlayer、MediaSession、MediaLibraryService 以及音频处理链（FFT -> EQ -> Reverb）都在 `PlaybackService` 中装配；`SpicaPlayer` 负责把这些能力包装成状态流和动作接口给 UI/ViewModel 使用。
- 播放控制的主链路是：`UI/Scene -> 页面 ViewModel 或 PlayerViewModel -> PlayerUseCases -> IMusicPlayer.doAction(PlayerAction)`；随后由 `SpicaPlayer` 通过 `MediaBrowser` 驱动 `PlaybackService` 中的 ExoPlayer。改播放行为时，优先沿这条链路排查，而不是直接在 UI 层碰 Media3 对象。
- `feature-lyrics-data` 只提供歌词 API 客户端和对应 DI；`Retrofit`、`OkHttpClient` 和歌词服务 `baseUrl` 实际定义在 `app/src/main/java/me/spica27/spicamusic/di/AppModule.kt`，所以歌词网络相关改动通常会跨 `app` 与 `feature-lyrics-data`。
- `common` 里的实体（`Song`、`Album`、`Artist`、`Playlist` 等）跨层传递，直接作为 `Scene` 构造参数传入导航；改动共享实体时要同时考虑数据库映射、仓库实现和 UI 消费方。
- 首页 `HomeScene` 由 `HomePage` 枚举驱动 Finder / Music / Library 三个 tab 页，页面实现在 `ui/home/page/**`；其中依赖 `BottomBarScrollConnection` 的 Composable 必须在 Scene 的 content lambda 内调用。

## 导航系统（navkit）
本项目的运行时导航使用仓库内自研的 `navkit` 模块；`app/build.gradle` 虽然仍声明了部分 Navigation 3 依赖，但当前页面流转代码实际基于 `Scene` / `NavigationPath`。核心概念：

- **`NavigationPath`**：导航栈状态持有者，提供 `push(scene)` / `popTop()` / `pop(scene)`。在 Composable 内通过 `LocalNavigationPath.current` 取得。
- **`StackScene`**：全屏页面的基类，内置滑入/滑出动画。新建全屏页面继承此类，实现 `Content()`。
- **`DialogScene`**：底部弹窗或对话框的基类，内置遮罩 + 缩放动画。子类实现 `DialogContent()`；遮罩点击自动关闭；在对话框内部手动关闭使用 `path.pop(LocalScene.current)`。
- **`GeometryTransition`**：共享元素飞行动画驱动器。在列表 item 上用 `Modifier.geometrySource(transition)` 标记源位置，在 `Scene` 中重写 `geometryTransition` 和 `FloatingContent()`，在目标封面上用 `Modifier.geometryTarget(transition)`，效果参考 `AlbumDetailScene`。
- 根场景在 `AppScaffold` 中通过 `NavigationStack(initialScene = { HomeScene() })` 声明，无需注册表或路由表。
- 新增全屏详情页的完整路径：① 创建 `XxxDetailScene : StackScene()`，在 `Content()` 中调用 Screen Composable；② 在触发点取得 `path = LocalNavigationPath.current`，执行 `path.push(XxxDetailScene(entity))`。

## 关键约定
- 保持依赖方向：`app -> feature-*-domain -> feature-*-data -> common/core-*`，`app` 只在应用组装处直接触达 data 模块。不要把 `app` 里的类型或实现反向拉进 data/domain 模块。
- 新的数据能力按这条路径走：先改 `feature-library-data` 中的 `storage/api` 接口，再补 `storage/impl` 实现与 Koin 绑定，最后在 domain facade 和 `AppModule` 注入到对应 ViewModel。完整参考样例是扫描目录能力：`IScanFolderRepository`（api）→ `ScanFolderRepositoryImpl` + `ScanFolderDao`/`ScanFolderEntity`（impl）→ `ScanFolderUseCases` 并在 `feature-library-domain/ScanModels.kt` 做 `toDomain()` 模型映射 → `ScanFoldersScene`（DialogScene）消费。
- 改动 Room 表结构必须在 `AppDatabase` 中补充 Migration（参考 v9 → v10 新增 `ScanFolder` 表），不要依赖破坏性重建。
- 仓库层批量写操作走单事务、只触发一次数据失效（参考 `IPlaylistRepository.removeSongsFromPlaylist(playlistId, mediaIds)`）；新增批量能力时沿用该模式，不要在调用侧循环单条接口。
- 新的播放能力按这条路径走：先扩展 `PlayerAction` 或 `IMusicPlayer`，再在 `SpicaPlayer` 处理，UI/ViewModel 侧继续只调用 `player.doAction(...)`，不要直接碰 `MediaBrowser` 或 `ExoPlayer`。
- 详情页 ViewModel 普遍使用 Koin 参数注入，并带稳定 key，例如 `koinViewModel(key = "PlaylistDetailViewModel_$playlistId") { parametersOf(playlistId) }`；同类页面新增参数化 ViewModel 时沿用这个模式，避免实例串用。同时在 `AppModule.kt` 内用 `viewModel { parameters -> ... }` 注册对应绑定。
- Compose 页面主要在 `app/src/main/java/me/spica27/spicamusic/ui/**`，可复用组件集中在 `app/src/main/java/me/spica27/spicamusic/ui/widget`；不要把业务逻辑塞回 Composable，本仓库默认通过 ViewModel 持有状态与接口依赖。
- 播放控制共享同一个 Activity 级 `PlayerViewModel`：它在 `AppScaffold` 中通过 `koinActivityViewModel()` 创建，并通过 `LocalPlayerViewModel` 下发给整棵 Compose 树。需要播放器状态或播放操作时，优先复用这个实例，不要在子页面重新创建一个播放器 ViewModel。
- 播放器里的 `mediaId` 约定上就是 `song.mediaStoreId.toString()`；无论是 `PlayerAction.PlayById`、`AddToNext` 还是 `UpdateList(mediaIds, mediaId)`，都沿用这个映射。新增播放入口时不要引入另一套 ID 规则。
- 使用 FFT 可视化的页面不要只收集 `fftDrawData`；按现有模式，需要在页面可见时调用 `subscribeFFTDrawData()`，离开时调用 `unsubscribeFFTDrawData()`，否则会额外保留插值计算开销。
- 设置持久化统一走 `PreferencesManager` 的 DataStore Flow；像 `AudioEffectsViewModel` 这样的页面会先写入偏好，再通过 Flow 收集结果把 EQ/混响应用到播放器，新增设置时优先复用这个模式。
- `App` 会按应用前后台生命周期启动/停止 `IMusicScanService` 的 MediaStore 监听，这不是某个页面局部逻辑；涉及扫描或库刷新时要考虑生命周期影响。额外/忽略扫描目录持久化在 Room 的 `ScanFolder` 表中，`IMusicScanService.scanFolders(folderPaths)` 已废弃，改用 `scanExtraFolders()`。
- `SpicaPlayer.createModule(PlaybackService::class.java)` 是 app 与 `feature-player-data` 的接缝点；如果播放器能力改动需要依赖服务类型或 DSP 处理链，优先从这里往两边追踪。
- 歌曲筛选统一使用 `SongFilter` 数据类（`common` 模块），支持 `artists`、`albums`、`keyword`、`onlyLiked` 等字段，通过 `ISongRepository.getSongsFlow(filter = ...)` 消费。
- 带搜索的分页列表遵循统一 Paging 模式（参考 `SearchViewModel`、`PlaylistDetailViewModel`）：浏览主列表与搜索结果是两条独立的 Pager 流（主列表不受输入影响、滚动位置不跳变）；关键字经 300ms `debounce` + `distinctUntilChanged`；空关键字发射带 `LoadState.Loading` 的 `PagingData.empty(sourceLoadStates = ...)` 驱动骨架屏。搜索命中高亮用 `ui/widget/KeywordHighlight.kt`；DAO 关键字匹配覆盖歌名/歌手/专辑，需与搜索框提示文案一致。

## Jetpack Compose
For Compose/Android UI work, follow the skill instructions in
`skills/compose-expert/SKILL.md`. Consult reference files in
`skills/compose-expert/references/` for patterns, pitfalls,
and source-code-backed guidance.
