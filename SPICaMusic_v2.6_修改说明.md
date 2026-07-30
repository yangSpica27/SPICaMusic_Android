# SPICaMusic v2.6 播放器形变修复

## 本轮目标

本版本以 **v2.5** 为基础，不回退此前已经完成的扫描规则、主题模式、缓存、底栏文字动画和 Performance 构建优化，只针对播放器的展开/收回形变继续修正：

1. 下滑关闭恢复 v2.3 / 原项目的视觉顺序：全屏前景先平滑退出，动态背景和共享封面继续跟随容器，最后缩回底部播放胶囊。
2. 底部播放栏继续使用真实半透明玻璃色，不恢复不透明背景，也不恢复异常阴影。
3. 上滑展开保留 v2.5 已完成的共享封面路径，但消除封面和全屏原封面重叠。
4. 播放胶囊到全屏页面的圆角按当前容器尺寸连续计算，不再在动画前段突然由胶囊切成方形。
5. 播放中的迷你封面继续使用原项目的动态多边形动画。

## 动画流程

### 打开

```text
底部半透明播放胶囊
→ 容器保持真正胶囊轮廓并向上生长
→ 圆角逐渐过渡为圆角矩形
→ 接近全屏时才平滑收为页面方形
```

封面同步执行：

```text
迷你播放器动态封面
→ 单一共享封面沿源位置/目标位置移动并放大
→ 动态多边形平滑交接为圆角封面
→ 到达目标后由全屏真实封面接管
```

### 下滑关闭

```text
全屏播放器前景随手势平滑退出
→ 动态背景和唯一共享封面继续跟手缩小
→ 容器恢复圆角矩形
→ 恢复真正长胶囊
→ 落回底部播放器原位置
```

不会再把整套全屏按钮和文字裁进一个半高的大矩形中。

## 圆角形变分段

- `0% ~ 10%`：按当前容器短边的一半维持真正胶囊。
- `10% ~ 80%`：使用 SmoothStep 曲线逐渐过渡到 `18dp` 圆角矩形。
- `80% ~ 100%`：再由 `18dp` 平滑过渡到全屏页面的 `0dp` 圆角。
- 向下拖动时完全反向复用同一路径，没有单独的关闭跳切动画。

## 修改文件

```text
app/src/main/java/me/spica27/spicamusic/ui/home/player_bar/BottomBar.kt
app/src/main/java/me/spica27/spicamusic/ui/home/player_bar/BottomBarV2.kt
app/src/main/java/me/spica27/spicamusic/ui/player/ExpandedPlayerScreen.kt
app/src/main/java/me/spica27/spicamusic/ui/player/PlayerArtworkMorph.kt
```

## 构建与安装

本轮只修改 Kotlin 文件，正常可直接执行：

```powershell
.\gradlew.bat --stop
.\gradlew.bat installPerformance --no-daemon
```

手机仍表现为旧动画时，再清理增量产物：

```powershell
.\gradlew.bat --stop
.\gradlew.bat clean installPerformance --no-daemon
```

APK 输出位置：

```text
app\build\outputs\apk\performance\app-performance.apk
```

## 检查情况

- 已确认 v2.6 相对 v2.5 只改动上述 4 个 Kotlin 文件。
- 已检查 `BottomBarV2` 唯一调用点与新的 6 参数 `fullScreenPlayer` Lambda 一致。
- 已检查所有 `ExpandedPlayerScreen` 调用点仍与默认参数兼容。
- 已执行 Kotlin 语法级检查；诊断仅为当前容器缺少 Android/Compose 类路径导致的 unresolved reference，没有发现括号、参数数量或语法解析错误。
- 当前容器没有 Android SDK 37，因此完整 AGP 编译与最终真机动画仍需在本机通过 `installPerformance` 验证。
