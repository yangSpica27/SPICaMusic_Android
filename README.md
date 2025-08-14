## 柠檬音乐【Compose版】

<p align="center">

<img src="/img/Screenshot_20250728_201015.png" width="20%"/>
<img src="/img/Screenshot_20250730_174948.png" width="20%"/>
<img src="/img/Screenshot_20250621_231303.png" width="20%"/>
<img src="/img/Screenshot_20250731_143921.png" width="20%"/>
</p>

<p align="center">
<img src="/img/Screenshot_20250731_144222.png" width="20%"/>
<img src="/img/Screenshot_20250731_144309.png" width="20%"/>
<img src="/img/Screenshot_20250731_144457.png" width="20%"/>
<img src="/img/Screenshot_20250731_144531.png" width="20%"/>
</p>

## 项目简介

EXOPlayer+Jetpack Compose实现的音乐播放器

&#10004; 支持 vorbis opus flac alac pcm_mulaw pcm_alaw mp3 aac ac3 eac3 dca mlp truehd 等常见格式

&#10004; 支持 16kb

&#10004; 支持 新增/编辑/删除歌单

&#10004; 支持 亮色/暗色模式切换

&#10004; 支持 歌词搜索/显示

&#10004; 支持 16kb

&#10004; 支持 EQ调节

## 计划修复&未完成功能

- [ ] 歌词组件无法滑动调整播放位置
- [ ] 歌词搜索服务可能超时
- [ ] 目前仅使用MediaStore搜索歌曲，无手动指定目录/全局扫描功能
- [ ] ALAC格式不能被正确识别
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

- [Retrofit](https://github.com/square/retrofit)
- [OkHTTP](https://github.com/square/okhttp)
- [Jetpack Compose](https://developer.android.com/compose): Jetpack Compose 是一个现代化的工具包，用于构建原生
  Android 应用的用户界面。它简化了 UI 开发，使您能够更快地构建应用。
- [Media3](https://github.com/androidx/media): Media3 是一个用于播放音频和视频的库，它提供了一个用于播放媒体的
  API。
  和一组用于管理媒体会话的类。
- [Amplituda](https://github.com/lincollincol/Amplituda): 一个用于分析音频的库，它提供了一种简单的方法来获取音频的振幅。
- [Noise](https://github.com/paramsen/noise): 一个通用的FFT计算库，用于计算音频的频谱。
- [FFMPEG](https://github.com/FFmpeg/FFmpeg):FFmpeg 是用于处理音频、视频、字幕和相关元数据等多媒体内容的库和工具的集合。
- [Koin](https://github.com/InsertKoinIO/koin):Koin 是一个面向 Kotlin 开发人员的实用、轻量级依赖注入框架。
- [AndroidLiquidGlass](https://github.com/Kyant0/AndroidLiquidGlass):Jetpack Compose液态玻璃效果实现。

最近要开始找工作了，更新随缘🌈


