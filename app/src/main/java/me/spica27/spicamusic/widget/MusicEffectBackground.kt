package me.spica27.spicamusic.widget

import android.graphics.RuntimeShader
import android.os.Build
import androidx.annotation.OptIn
import androidx.annotation.RequiresApi
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.media3.common.util.UnstableApi
import kotlinx.coroutines.android.awaitFrame
import kotlinx.coroutines.delay
import me.spica27.spicamusic.R
import me.spica27.spicamusic.utils.DataStoreUtil
import me.spica27.spicamusic.utils.readRawResource
import me.spica27.spicamusic.visualiser.MusicVisualiser

@OptIn(UnstableApi::class)
@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@Composable
fun MusicEffectBackground(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val density = LocalDensity.current

    // 从资源加载GLSL字符串或使用传入的字符串
    val glsl =
        remember {
            readRawResource(context, R.raw.effect)
        }

    val shader =
        remember(glsl) {
            RuntimeShader(glsl).also { shader ->
                shader.setFloatUniform(
                    "uPoints",
                    floatArrayOf(
                        0.67f,
                        0.42f,
                        1.0f,
                        0.69f,
                        0.75f,
                        1.0f,
                        0.14f,
                        0.71f,
                        0.95f,
                        0.14f,
                        0.27f,
                        0.8f,
                    ),
                )
                shader.setFloatUniform(
                    "uColors",
                    floatArrayOf(
                        0.57f,
                        0.76f,
                        0.98f,
                        1.0f,
                        0.98f,
                        0.85f,
                        0.68f,
                        1.0f,
                        0.98f,
                        0.75f,
                        0.93f,
                        1.0f,
                        0.73f,
                        0.7f,
                        0.98f,
                        1.0f,
                    ),
                )
                shader.setFloatUniform("uNoiseScale", 1.0f)
                shader.setFloatUniform("uPointOffset", 0.1f)
                shader.setFloatUniform("uPointRadiusMulti", 1.0f)
                shader.setFloatUniform("uSaturateOffset", 0.6f)
                shader.setFloatUniform("uShadowColorMulti", 0.21f)
                shader.setFloatUniform("uShadowColorOffset", .2f)
                shader.setFloatUniform("uShadowOffset", 0.21f)
                shader.setFloatUniform("uBound", floatArrayOf(0.4f, 0.5f, 0.6f, 0.7f))
                shader.setFloatUniform("uAlphaMulti", 1.5f)
                shader.setFloatUniform("uLightOffset", 0.1f)
                shader.setFloatUniform("uAlphaOffset", 0.5f)
                shader.setFloatUniform("uShadowNoiseScale", 3.5f)
                shader.setFloatUniform("uMusicLevel", 0f)
                shader.setFloatUniform("uBeat", 0f)
                shader.setFloatUniform("uAnimTime", 0f)
            }
        }

    val isNight = DataStoreUtil().getForceDarkTheme.collectAsState(false).value

    LaunchedEffect(isNight) {
        if (isNight) {
            shader.setFloatUniform("uLightOffset", -0.5f)
            shader.setFloatUniform("uSaturateOffset", 0.2f)
//      shader.setFloatUniform("uBound", floatArrayOf(0f, 1.0f, 1.0f, 0f))
            shader.setFloatUniform(
                "uPoints",
                floatArrayOf(
                    0.67f,
                    0.42f,
                    1.0f,
                    0.69f,
                    0.75f,
                    1.0f,
                    0.14f,
                    0.71f,
                    0.95f,
                    0.14f,
                    0.27f,
                    0.8f,
                ),
            )
            shader.setFloatUniform(
                "uColors",
                floatArrayOf(
                    1.00f,
                    1.00f,
                    1.00f,
                    0.50f,
                    0.35f,
                    0.35f,
                    0.35f,
                    0.10f,
                    0.25f,
                    0.25f,
                    0.25f,
                    0.10f,
                    0.55f,
                    0.55f,
                    0.55f,
                    0.10f,
                ),
            )
        } else {
            shader.setFloatUniform("uLightOffset", 0.5f)
            shader.setFloatUniform("uSaturateOffset", 0.2f)
//      shader.setFloatUniform("uBound", floatArrayOf(0f, 1.0f, 1.0f, 0f))
            shader.setFloatUniform(
                "uPoints",
                floatArrayOf(
                    0.67f,
                    0.42f,
                    1.0f,
                    0.69f,
                    0.75f,
                    1.0f,
                    0.14f,
                    0.71f,
                    0.95f,
                    0.14f,
                    0.27f,
                    0.8f,
                ),
            )
            shader.setFloatUniform(
                // uniformName =
                "uColors",
                // values =
                floatArrayOf(
                    0.35f,
                    0.35f,
                    0.35f,
                    0.10f,
                    0.35f,
                    0.35f,
                    0.35f,
                    0.10f,
                    0.25f,
                    0.25f,
                    0.25f,
                    0.10f,
                    0.55f,
                    0.55f,
                    0.55f,
                    0.20f,
                ),
            )
        }
    }

    DisposableEffect(Unit) {
        val musicVisualiser = MusicVisualiser()
        musicVisualiser.setListener(
            object : MusicVisualiser.Listener {
                override fun getDrawData(list: List<Float>) {
                    shader.setFloatUniform("uMusicLevel", 1f)
                    shader.setFloatUniform("uBeat", list.max())
                }
            },
        )
        musicVisualiser.ready()
        onDispose {
            musicVisualiser.dispose()
        }
    }

    var animTimeState by remember { mutableStateOf(0f) }

    LaunchedEffect(Unit) {
        val startTime = withFrameNanos { it }
        do {
            val now = withFrameNanos { it }
            animTimeState = (now - startTime) / 1.0E9f
            delay(16)
            awaitFrame()
        } while (true)
    }

    Canvas(modifier = modifier.fillMaxSize()) {
        shader.setFloatUniform("uResolution", size.width, size.height)
        shader.setFloatUniform("uAnimTime", animTimeState)
        val brush = ShaderBrush(shader)
        drawRect(brush = brush)
    }
}
