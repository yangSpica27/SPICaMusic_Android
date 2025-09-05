package me.spica27.spicamusic.ui.translate

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.navigation.NavController
import kotlinx.coroutines.delay
import me.spica27.spicamusic.shaderrippleeffect.ExpandingWaveEffect
import me.spica27.spicamusic.shaderrippleeffect.RevealContentTransition
import me.spica27.spicamusic.shaderrippleeffect.data.RevealTransitionParams
import me.spica27.spicamusic.shaderrippleeffect.data.WaveEffectParams
import me.spica27.spicamusic.theme.AppTheme
import me.spica27.spicamusic.ui.setting.SettingPage

@Composable
fun TranslateScreen(
    navigator: NavController? = null,
    pointX: Float,
    pointY: Float,
    fromLight: Boolean,
) {
    var trigger by remember { mutableIntStateOf(0) }

    BackHandler(true) {
    }

    LaunchedEffect(Unit) {
        trigger = 1
        delay(1750)
        navigator?.popBackStack()
    }

    Box(
        modifier =
            Modifier
                .fillMaxSize(),
    ) {
        ExpandingWaveEffect(
            origin =
                Offset(
                    pointX,
                    pointY,
                ),
            trigger = trigger,
            params = WaveEffectParams(),
        ) {
            RevealContentTransition(
                origin =
                    Offset(
                        pointX,
                        pointY,
                    ),
                trigger = trigger,
                params = RevealTransitionParams(),
                firstContent = {
                    AppTheme(
                        darkTheme = fromLight,
                        dynamicColor = false,
                    ) {
                        Scaffold { paddingValues ->
                            Box(
                                modifier =
                                    Modifier
                                        .fillMaxSize()
                                        .padding(paddingValues)
                                        .pointerInput(Unit) {
                                            detectTapGestures {}
                                        },
                            ) {
                                SettingPage()
                            }
                        }
                    }
                },
                secondContent = {
                    AppTheme(
                        darkTheme = !fromLight,
                        dynamicColor = false,
                    ) {
                        Scaffold { paddingValues ->
                            Box(
                                modifier =
                                    Modifier
                                        .fillMaxSize()
                                        .padding(paddingValues)
                                        .pointerInput(Unit) {
                                            detectTapGestures {}
                                        },
                            ) {
                                SettingPage()
                            }
                        }
                    }
                },
            )
        }
    }
}
