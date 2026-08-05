package me.spica27.spicamusic.ui.theme

import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext

/**
 * 是否走降级动效。
 *
 * 语义是"更少更柔"而不是"全部关掉"：保留透明度淡入（它帮助理解状态变化），
 * 去掉位移与逐个排队的 stagger 延迟。默认 false，即完整播放动效。
 *
 * 用 static 变体：这个值在一次进程生命周期内不会变，没必要让读取它的地方参与重组追踪。
 */
val LocalReducedMotion: ProvidableCompositionLocal<Boolean> = staticCompositionLocalOf { false }

/**
 * 读取系统「设置 → 无障碍 → 移除动画」开关。
 *
 * 必须自己读：Compose 动画不像 View 的 ValueAnimator 那样会自动遵守
 * animator duration scale，系统开关对 Compose 完全不起作用。
 *
 * 只在首次组合时读一次——切换这个系统开关是罕见操作，重启 App 后生效即可，
 * 换来的是零监听、零每帧开销。
 */
@Composable
fun rememberSystemReducedMotion(): Boolean {
    val context = LocalContext.current
    return remember(context) {
        // 三参重载在键缺失时返回缺省值 1f，不抛异常，无需 try/catch
        Settings.Global.getFloat(
            context.contentResolver,
            Settings.Global.ANIMATOR_DURATION_SCALE,
            1f,
        ) == 0f
    }
}
