# 柠檬音乐 (SPICa Music) - Copilot 指南

## 一句话概览
这是一个多模块 Android 音乐播放器：UI 在 app，核心逻辑在 storage-core/player-core/lyric-core，全部通过接口解耦。

## 架构与数据流（必须先读）
- 模块边界：app 仅依赖接口；功能实现分别在 storage-core、player-core、lyric-core；共用实体在 common。
- 依赖方向：app → 各 core → common（不要反向依赖）。
- 播放器访问统一走 `IMusicPlayer` + `PlayerAction`，见 [player-core/src/main/java/me/spica27/spicamusic/player/api/IMusicPlayer.kt](player-core/src/main/java/me/spica27/spicamusic/player/api/IMusicPlayer.kt) 和 [player-core/src/main/java/me/spica27/spicamusic/player/impl/SpicaPlayer.kt](player-core/src/main/java/me/spica27/spicamusic/player/impl/SpicaPlayer.kt)。
- 数据仓库接口在 [storage-core/src/main/java/me/spica27/spicamusic/storage/api](storage-core/src/main/java/me/spica27/spicamusic/storage/api)，实现位于 storage-core/impl。

## 关键模式与约定
- Koin DI：模块在 App 初始化注册；ViewModel 必须构造注入接口，注册位置在 [app/src/main/java/me/spica27/spicamusic/di/AppModule.kt](app/src/main/java/me/spica27/spicamusic/di/AppModule.kt)。
- 导航采用 Navigation 3：路由在 [app/src/main/java/me/spica27/spicamusic/navigation/Screen.kt](app/src/main/java/me/spica27/spicamusic/navigation/Screen.kt)，图在 [app/src/main/java/me/spica27/spicamusic/navigation/NavGraph.kt](app/src/main/java/me/spica27/spicamusic/navigation/NavGraph.kt)。
- Compose 组织：页面在 app/ui/**/Screen.kt，自定义组件在 [app/src/main/java/me/spica27/spicamusic/widget](app/src/main/java/me/spica27/spicamusic/widget)。
- 播放器动作：UI 只调用 `player.doAction(...)`，示例：`PlayerAction.PlayById`、`PlayerAction.SeekTo`。

## 外部集成与关键实现点
- Media3 + 后台播放：`MediaBrowser`/`PlaybackService` 相关实现集中在 player-core/impl。
- 音频处理链：FFT 相关在 [player-core/src/main/java/me/spica27/spicamusic/player/impl/dsp](player-core/src/main/java/me/spica27/spicamusic/player/impl/dsp)。
- 歌词网络：lyric-core 通过 ApiClient 访问，基础 URL 在 app/di 的网络配置中。
- DataStore 偏好设置封装在 [app/src/main/java/me/spica27/spicamusic/utils/PreferencesManager.kt](app/src/main/java/me/spica27/spicamusic/utils/PreferencesManager.kt)。

## 常用工作流（项目特定）
- Debug 构建：./gradlew assembleDebug
- Release 构建：./gradlew assembleRelease（minSdk 29, targetSdk 36, compileSdk 36）
- KtLint：./gradlew ktlintFormat
- 版本号在 [gradle.properties](gradle.properties) 中，`versionCode = MAJOR*1_000_000 + MINOR*10_000 + BUILD`。

## 变更时的最短路径
- 新数据操作：先改 storage-core/api 接口，再实现到 storage-core/impl，再在 ViewModel 注入使用。
- 新播放能力：在 `PlayerAction` 增加 action → SpicaPlayer 处理 → UI 调用 `doAction`。
- 新路由：先在 `Screen` 加 `@Serializable`，再在 `NavGraph` entryProvider 注册。
