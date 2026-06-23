package me.spica27.spicamusic.ui.home.player_bar

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout

@Composable
fun BottomBarV2(
    navigationBar: @Composable () -> Unit,
    playBar: @Composable () -> Unit,
    fullScreenPlayer: @Composable () -> Unit,
) {
    val progress by rememberSaveable {
        mutableStateOf(0.0f)
    }

    Layout(
        modifier = Modifier.fillMaxSize(),
        content = {
            navigationBar()
            playBar()
            fullScreenPlayer()
        },
        measurePolicy = { measurables, constraints ->
            val placeables =
                measurables.mapIndexed { index, measurable ->
                    when (index) {
                        0 -> measurable.measure(constraints.copy(minWidth = 0, minHeight = 0))
                        1 -> measurable.measure(constraints.copy(minWidth = 0, minHeight = 0))
                        2 ->
                            measurable.measure(
                                constraints.copy(
                                    minWidth = 0,
                                    minHeight = 0,
                                    maxWidth = constraints.maxWidth,
                                    maxHeight = constraints.maxHeight,
                                ),
                            )
                        else -> measurable.measure(constraints)
                    }
                }

            layout(constraints.maxWidth, constraints.maxHeight) {
                val navigationBar = placeables[0]
                val playBar = placeables[1]
                val fullScreenPlayer = placeables[2]

                layout(constraints.maxWidth, constraints.maxHeight) {
                    navigationBar.place(0, constraints.maxHeight - fullScreenPlayer.height)
                }
            }
        },
    )
}
