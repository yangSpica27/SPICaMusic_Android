package me.spica27.spicamusic.widget

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import me.spica27.spicamusic.ui.player.LocalPlayerWidgetState
import kotlin.math.absoluteValue

@Composable
fun rememberBindPlayerOverlyConnect(): BindPlayerPageConnect {
    val overlyState = LocalPlayerWidgetState.current
    val density = LocalDensity.current
    return remember {
        BindPlayerPageConnect(
            scrollThresholdPx = with(density) { 120.dp.toPx() },
            listener = {
//        if (overlyState.value == PlayerOverlyState.BOTTOM) {
// //          overlyState.value = PlayerOverlyState.MINI
//        }
            },
        )
    }
}

class BindPlayerPageConnect(
    private val scrollThresholdPx: Float = 120f,
    private val listener: () -> Unit,
) : NestedScrollConnection {
    fun inline() {
        listener.invoke()
    }

    private var accumulatedScroll = 0f

    override fun onPreScroll(
        available: Offset,
        source: NestedScrollSource,
    ): Offset {
        val scrollDelta = available.y.absoluteValue

        accumulatedScroll += scrollDelta

        if (accumulatedScroll >= scrollThresholdPx) {
            inline()
            accumulatedScroll = 0f
        }

        return Offset.Zero
    }
}
