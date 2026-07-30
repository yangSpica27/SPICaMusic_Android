# SPICaMusic 第二轮界面与性能修复

本轮针对录屏中出现的标题重影、底栏文字竖排、播放器展开/关闭顿帧、底栏尺寸/透明感不一致和 APK 体积增大继续处理。

## 1. 「发现」标题滚动变形

- 删除滚动过程中的 `scaleX/scaleY` 位图缩放。
- 大标题只随列表自然滚动并淡出。
- 固定标题在大标题完全淡出后再淡入，二者不同时叠加。

主要文件：

- `app/src/main/java/me/spica27/spicamusic/ui/home/page/FinderPage.kt`

## 2. 底栏展开时「发现」短暂竖排

- 底栏模式只使用 `BottomBarScrollConnection.isInline` 作为唯一状态源，避免两个状态不同步。
- 紧凑栏展开时，先完成胶囊横向生长，再延迟显示文字。
- 所有导航文字强制 `maxLines = 1`、`softWrap = false`，窄宽度下只裁切，不换成竖排。

## 3. 正在播放改为胶囊到全屏形变

- 打开：先由 60dp 长胶囊连续铺满全屏，再淡入播放器内容。
- 关闭：先关闭 FFT/TextureView/波形等重效果并淡出内容，再由全屏收回成长胶囊。
- 几何动画期间只绘制轻量容器，不让封面解码、Pager、动态背景线程参与每一帧。
- 首次组合完成后保留播放器组合树，避免关闭动画最后一段突然销毁、闪回。

主要文件：

- `BottomBarV2.kt`
- `VerticalDragGestureHandler.kt`
- `ExpandedPlayerScreen.kt`

## 4. 底部栏统一尺寸与玻璃感

- 展开导航胶囊、紧凑播放胶囊统一为 60dp。
- 两侧圆形按钮统一为 58dp。
- 横向边距统一为 16dp，间距统一为 10dp。
- 导航、迷你播放器和按钮改为半透明 Surface，并保留描边和 6dp 悬浮阴影。
- 形变容器从半透明胶囊颜色平滑过渡到不透明全屏页面底色，避免中途闪白/闪黑。

## 5. APK 体积优化

- 删除项目内 `feature-library-data/libs/icu4j-78.1.jar`（原文件约 15.15MB）。
- 改用 Android 29+ 系统自带的 `android.icu.text.Transliterator`，保留中文拼音、平假名和片假名首字母排序。
- 转换器改为线程本地缓存，扫描时不再为每首歌曲重复创建规则对象。
- 删除没有代码使用的 `pinyin4j` 依赖和许可证条目。
- 新增 `performance` 构建类型，启用与 Release 相同的 R8/资源压缩，用于真机性能与体积测试。

> 32.9MB 很可能是 Debug APK。Debug 不执行 R8/资源压缩，并包含 Compose 调试工具，不能与原作者的 Release APK 直接比较。本轮另外从依赖层删除了大型 ICU4J JAR，因此 Debug 和 Release 都会减小；实际减少值以本机生成 APK 为准。

## 推荐构建

依赖已发生删除，第一次请执行一次 clean：

```powershell
.\gradlew.bat --stop
.\gradlew.bat clean assembleRelease
```

Release APK：

```text
app\build\outputs\apk\release\app-release.apk
```

也可以构建性能测试版：

```powershell
.\gradlew.bat clean assemblePerformance
```

Performance APK：

```text
app\build\outputs\apk\performance\app-performance.apk
```

查看 APK 大小：

```powershell
Get-Item .\app\build\outputs\apk\release\app-release.apk |
  Select-Object Name, @{Name="SizeMB"; Expression={[math]::Round($_.Length / 1MB, 2)}}
```

## 构建兼容修复

完整源码同时保留此前构建修复：

- Gradle Wrapper 使用官方 Gradle 9.5.0 地址。
- 所有 Android 模块统一 `compileSdk = 37`。
- 补齐各 Library 模块声明但缺失的 `consumer-rules.pro` / `proguard-rules.pro`。

当前执行环境没有 Android SDK，因此这里完成的是源码静态检查和结构检查；最终 Kotlin/AGP 编译请以你的 Windows + Android Studio 环境结果为准。
