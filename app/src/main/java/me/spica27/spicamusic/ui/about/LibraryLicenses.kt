package me.spica27.spicamusic.ui.about

import androidx.compose.runtime.Immutable

/**
 * 第三方依赖库的许可证声明条目。
 *
 * 数据来源为 `gradle/libs.versions.toml` 中声明的全部依赖（含传递依赖的核心来源），
 * 在「开源许可证」页面展示。新增/移除依赖时请同步维护此列表。
 */
@Immutable
data class OssLibrary(
    val name: String,
    /** Maven 坐标 group:artifact，用于辨识 */
    val artifact: String,
    /** 许可证名称，如 Apache-2.0 / MIT */
    val license: String,
    /** 项目主页，点击可在浏览器打开 */
    val url: String,
)

/**
 * 全部第三方依赖声明。按字母序大致归类，便于查阅。
 */
val ossLibraries: List<OssLibrary> =
    listOf(
        // ---------- AndroidX / Jetpack ----------
        OssLibrary(
            "AndroidX Core KTX",
            "androidx.core:core-ktx",
            "Apache-2.0",
            "https://developer.android.com/jetpack/androidx/releases/core",
        ),
        OssLibrary(
            "Jetpack Compose",
            "androidx.compose:compose-bom",
            "Apache-2.0",
            "https://developer.android.com/jetpack/compose",
        ),
        OssLibrary(
            "Compose Material 3",
            "androidx.compose.material3:material3",
            "Apache-2.0",
            "https://developer.android.com/jetpack/androidx/releases/compose-material3",
        ),
        OssLibrary(
            "Compose Material 3 Adaptive",
            "androidx.compose.material3.adaptive:adaptive",
            "Apache-2.0",
            "https://developer.android.com/jetpack/androidx/releases/compose-material3-adaptive",
        ),
        OssLibrary(
            "Compose Material Icons",
            "androidx.compose.material:material-icons-extended",
            "Apache-2.0",
            "https://developer.android.com/jetpack/androidx/releases/compose-material",
        ),
        OssLibrary(
            "Activity Compose",
            "androidx.activity:activity-compose",
            "Apache-2.0",
            "https://developer.android.com/jetpack/androidx/releases/activity",
        ),
        OssLibrary(
            "Lifecycle (Runtime / ViewModel / Process)",
            "androidx.lifecycle:lifecycle-runtime-ktx",
            "Apache-2.0",
            "https://developer.android.com/jetpack/androidx/releases/lifecycle",
        ),
        OssLibrary(
            "DataStore Preferences",
            "androidx.datastore:datastore-preferences",
            "Apache-2.0",
            "https://developer.android.com/jetpack/androidx/releases/datastore",
        ),
        OssLibrary(
            "Room",
            "androidx.room:room-runtime",
            "Apache-2.0",
            "https://developer.android.com/jetpack/androidx/releases/room",
        ),
        OssLibrary(
            "Paging",
            "androidx.paging:paging-runtime",
            "Apache-2.0",
            "https://developer.android.com/jetpack/androidx/releases/paging",
        ),
        OssLibrary(
            "ConstraintLayout Compose",
            "androidx.constraintlayout:constraintlayout-compose",
            "Apache-2.0",
            "https://developer.android.com/jetpack/androidx/releases/constraintlayout",
        ),
        OssLibrary(
            "DynamicAnimation KTX",
            "androidx.dynamicanimation:dynamicanimation-ktx",
            "Apache-2.0",
            "https://developer.android.com/jetpack/androidx/releases/dynamicanimation",
        ),
        OssLibrary(
            "Graphics (Core / Path / Shapes)",
            "androidx.graphics:graphics-shapes",
            "Apache-2.0",
            "https://developer.android.com/jetpack/androidx/releases/graphics",
        ),
        OssLibrary(
            "Palette KTX",
            "androidx.palette:palette-ktx",
            "Apache-2.0",
            "https://developer.android.com/jetpack/androidx/releases/palette",
        ),
        OssLibrary(
            "Navigation 3",
            "androidx.navigation3:navigation3-runtime",
            "Apache-2.0",
            "https://developer.android.com/jetpack/androidx/releases/navigation3",
        ),
        OssLibrary(
            "Media",
            "androidx.media:media",
            "Apache-2.0",
            "https://developer.android.com/jetpack/androidx/releases/media",
        ),
        OssLibrary(
            "Benchmark Macro JUnit4",
            "androidx.benchmark:benchmark-macro-junit4",
            "Apache-2.0",
            "https://developer.android.com/jetpack/androidx/releases/benchmark",
        ),
        OssLibrary(
            "UI Automator",
            "androidx.test.uiautomator:uiautomator",
            "Apache-2.0",
            "https://developer.android.com/jetpack/androidx/releases/test",
        ),
        // ---------- Media / Audio ----------
        OssLibrary(
            "Media3 ExoPlayer / Session",
            "androidx.media3:media3-exoplayer",
            "Apache-2.0",
            "https://github.com/androidx/media",
        ),
        OssLibrary(
            "Jellyfin Media3 FFmpeg Decoder",
            "org.jellyfin.media3:media3-ffmpeg-decoder",
            "Apache-2.0 / LGPL (FFmpeg)",
            "https://github.com/jellyfin/jellyfin-androidx-media",
        ),
        OssLibrary(
            "Amplituda",
            "com.github.lincollincol:amplituda",
            "Apache-2.0",
            "https://github.com/lincollincol/Amplituda",
        ),
        OssLibrary(
            "TagLib (Kyant)",
            "io.github.kyant0:taglib",
            "LGPL-2.1",
            "https://github.com/Kyant0/taglib",
        ),
        // ---------- DI ----------
        OssLibrary(
            "Koin",
            "io.insert-koin:koin-androidx-compose",
            "Apache-2.0",
            "https://github.com/InsertKoinIO/koin",
        ),
        // ---------- Network / JSON ----------
        OssLibrary(
            "Retrofit",
            "com.squareup.retrofit2:retrofit",
            "Apache-2.0",
            "https://github.com/square/retrofit",
        ),
        OssLibrary(
            "OkHttp",
            "com.squareup.okhttp3:okhttp",
            "Apache-2.0",
            "https://github.com/square/okhttp",
        ),
        OssLibrary(
            "Moshi",
            "com.squareup.moshi:moshi-kotlin",
            "Apache-2.0",
            "https://github.com/square/moshi",
        ),
        OssLibrary(
            "Sandwich (Retrofit)",
            "com.github.skydoves:sandwich-retrofit",
            "Apache-2.0",
            "https://github.com/skydoves/sandwich",
        ),
        OssLibrary(
            "Kotlinx Serialization",
            "org.jetbrains.kotlinx:kotlinx-serialization-core",
            "Apache-2.0",
            "https://github.com/Kotlin/kotlinx.serialization",
        ),
        OssLibrary(
            "Kotlinx Coroutines (Guava)",
            "org.jetbrains.kotlinx:kotlinx-coroutines-guava",
            "Apache-2.0",
            "https://github.com/Kotlin/kotlinx.coroutines",
        ),
        // ---------- Image ----------
        OssLibrary(
            "Coil 3",
            "io.coil-kt.coil3:coil-compose",
            "Apache-2.0",
            "https://github.com/coil-kt/coil",
        ),
        OssLibrary(
            "Landscapist",
            "com.github.skydoves:landscapist-image",
            "Apache-2.0",
            "https://github.com/skydoves/landscapist",
        ),
        OssLibrary(
            "Cloudy",
            "com.github.skydoves:cloudy",
            "Apache-2.0",
            "https://github.com/skydoves/cloudy",
        ),
        // ---------- UI / Visual Effects ----------
        OssLibrary(
            "MIUIX",
            "top.yukonga.miuix.kmp:miuix-ui",
            "Apache-2.0",
            "https://github.com/compose-miuix-ui/miuix",
        ),
        OssLibrary(
            "MaterialKolor",
            "com.materialkolor:material-kolor",
            "MIT",
            "https://github.com/jordond/MaterialKolor",
        ),
        OssLibrary(
            "Haze",
            "dev.chrisbanes.haze:haze",
            "Apache-2.0",
            "https://github.com/chrisbanes/haze",
        ),
        OssLibrary(
            "AndroidLiquidGlass",
            "com.github.Kyant0:AndroidLiquidGlass",
            "Apache-2.0",
            "https://github.com/Kyant0/AndroidLiquidGlass",
        ),
        OssLibrary(
            "Accompanist Permissions",
            "com.google.accompanist:accompanist-permissions",
            "Apache-2.0",
            "https://github.com/google/accompanist",
        ),
        OssLibrary(
            "AndroidAutoSize",
            "com.github.JessYanCoding:AndroidAutoSize",
            "Apache-2.0",
            "https://github.com/JessYanCoding/AndroidAutoSize",
        ),
        // ---------- Utility ----------
        OssLibrary(
            "Timber",
            "com.jakewharton.timber:timber",
            "Apache-2.0",
            "https://github.com/JakeWharton/timber",
        ),
        OssLibrary(
            "Pinyin4j",
            "com.belerweb:pinyin4j",
            "Apache-2.0",
            "https://github.com/belerweb/pinyin4j",
        ),
        OssLibrary(
            "PrettyTime",
            "org.ocpsoft.prettytime:prettytime",
            "Apache-2.0",
            "https://github.com/ocpsoft/prettytime",
        ),
    )
