# Rust / OxiMedia 音频处理引擎

公共 PCM 处理链在 `arm64-v8a` 上使用一个 Rust 动态库：

```text
FFmpeg AAR / 系统解码器
  -> SpicaDspAudioSink（在 DefaultAudioSink 转换格式前处理）
  -> Rust JNI / 交错 PCM -> 复用的 f64 缓冲区
  -> OxiMedia 十段 EQ
  -> OxiMedia K 加权的 400 ms 瞬时 LUFS 测量
  -> 平滑增益 + 声道联动的样本峰值保护
  -> 编码回原始 PCM 格式
  -> DefaultAudioSink / AudioTrack
```

## 构建

使用 Rust 1.90.0、`aarch64-linux-android` 编译目标和 Android NDK
28.2.13676358。Gradle 任务通过 NDK API 29 链接器生成 ELF 段按 16 KB 对齐的动态库，
输出目录为 `build/generated/rustJniLibs`。

Cargo 按以下顺序查找：`-PspicaCargo`、`CARGO` 环境变量、标准 rustup 目录、
项目本地工具链目录，最后使用 PATH。Cargo 缓存目录可通过 `-PspicaCargoHome`
或 `CARGO_HOME` 环境变量指定。

Linux x86_64 环境尚未安装 Rust 时，可运行：

```sh
sh feature-player-native-dsp/scripts/bootstrap-rust.sh
```

脚本会将固定版本的工具链下载到项目已忽略的 `.gradle/spica-rust` 目录，
不会修改终端配置。

在项目根目录执行测试和构建：

```sh
./gradlew :feature-player-native-dsp:testRustDsp
./gradlew :feature-player-native-dsp:testDebugUnitTest
./gradlew :app:assembleDebug
./gradlew :app:assembleRelease
```

单元测试会为主机编译真实的 JNI 动态库，验证动态库加载、参数控制、处理和资源释放，
避免动态库不可用时静默旁路而导致测试误判。Robolectric 测试负责验证 Media3
音频输出层的接口约定。

## 开源许可

OxiMedia 和 OxiFFT 使用 Apache-2.0，导入源码已保留来源署名和修改说明。
精简源码以外的项目 Rust 实现沿用本项目的 MIT 许可证。依赖及 Rust 标准库的
完整许可文本随应用打包到 `assets/oss/oximedia-dsp-licenses.txt`，应用的开源许可页面
也已列出新增的库。
