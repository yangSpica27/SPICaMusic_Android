## 柠檬音乐【Compose版】

<p align="center">

<img src="/img/Screenshot_20250728_201015.png" width="20%"/>
<img src="/img/Screenshot_20250730_174948.png" width="20%"/>
<img src="/img/Screenshot_20250621_231303.png" width="20%"/>
<img src="/img/Screenshot_20250820_145443.png" width="20%"/>
</p>

<p align="center">
<img src="/img/Screenshot_20250820_145709.png" width="20%"/>
<img src="/img/Screenshot_20250731_144309.png" width="20%"/>
<img src="/img/Screenshot_20250731_144457.png" width="20%"/>
<img src="/img/Screenshot_20250820_145606.png" width="20%">
</p>

## 项目简介

柠檬音乐是一款基于 EXOPlayer 和 Jetpack Compose 精心打造的现代化安卓音乐播放器。
本项目致力于为用户提供功能丰富、界面美观且高度可定制的音乐播放体验。
通过整合最新的 Android 开发技术，柠檬音乐不仅支持多种常见的音频格式，
还提供了歌词显示、EQ调节、歌单管理等一系列实用功能，旨在成为您安卓设备上的理想音乐伴侣。

## 功能特性

| 功能类别     | 具体特性                                                                  |
|--------------|---------------------------------------------------------------------------|
| 音频格式支持 | vorbis, opus, flac, alac, pcm_mulaw, pcm_alaw, mp3, aac, ac3, eac3, dca, mlp, truehd 等常见格式 |
| 歌单管理     | 新增/编辑/删除歌单                                                        |
| 主题切换     | 亮色/暗色模式切换                                                         |
| 歌词功能     | 歌词搜索/显示                                                             |
| 音效调节     | EQ调节                                                                    |
| 其他特性     | 支持 16kb                |

## 计划修复&未完成功能

- [ ] 歌词组件无法滑动调整播放位置
- [ ] 歌词搜索服务可能超时
- [ ] 目前仅使用MediaStore搜索歌曲，无手动指定目录/全局扫描功能
- [x] ~~ALAC格式不能被正确识别~~
- [ ] 歌单内批量编辑功能

## 项目指南

1. 拉取源代码
2. Android Studio打开
3. just run

## 注意

本仓库遵循MIT协议。然而，WAV、ALAC等格式的软解码依赖于ffmpeg，并需要单独的许可证。有需要请自行编译拓展插件后引入
[decoder_ffmpeg](https://github.com/androidx/media/tree/release/libraries/decoder_ffmpeg)


## 开发环境


| 类型     | 描述 |
| ----------- | ----------- |
| 操作系统      | Windows 10 IoT 企业版 LTSC       |
| Android Studio   | Android Studio Narwhal | 2025.1.1 |
| JDK | JBR 21.0.6 | 

## 库

- [Retrofit](https://github.com/square/retrofit): Android/Java平台类型安全的HTTP请求库
- [OkHTTP](https://github.com/square/okhttp): 一个高效的HTTP和HTTP/2客户端
- [Jetpack Compose](https://developer.android.com/compose): Jetpack Compose 是一个现代化的工具包，用于构建原生
  Android 应用的用户界面。它简化了 UI 开发，使您能够更快地构建应用。
- [Media3](https://github.com/androidx/media): Media3 是一个用于播放音频和视频的库，它提供了一个用于播放媒体的
  API，和一组用于管理媒体会话的类。
- [Amplituda](https://github.com/lincollincol/Amplituda): 一个用于分析音频的库，它提供了一种简单的方法来获取音频的振幅。
- [TarsosDSP](https://github.com/paramsen/noise): TarsosDSP旨在通过提供一套简易接口，让开发者能够轻松访问和运用音乐处理算法。
- [FFMPEG](https://github.com/FFmpeg/FFmpeg):FFmpeg 是用于处理音频、视频、字幕和相关元数据等多媒体内容的库和工具的集合。
- [Koin](https://github.com/InsertKoinIO/koin):Koin 是一个面向 Kotlin 开发人员的实用、轻量级依赖注入框架。
- [AndroidLiquidGlass](https://github.com/Kyant0/AndroidLiquidGlass):Jetpack Compose液态玻璃效果实现。
- [Coil3](https://github.com/coil-kt/coil): 适用于 Android 和 Compose Multiplatform 的图像加载库。
- [Moshi](https://github.com/square/moshi): 一个开源的JSON解析库。

最近要开始找工作了，更新随缘🌈
