package me.spica27.spicamusic.utils

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter


fun Modifier.clickableWithVibration(
  onClick: () -> Unit
) = composed {
  val vibrator = rememberVibrator()
  clickable {
    vibrator.cancel()
    onClick()
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
      val effect = VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK)
      vibrator.vibrate(effect)
    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      val effect = VibrationEffect.createOneShot(10, 1)
      vibrator.vibrate(effect)
    } else {
      vibrator.vibrate(10)
    }
  }
}


fun Modifier.clickableNoRippleWithVibration(
  onClick: () -> Unit
) = composed {
  val vibrator = rememberVibrator()
  clickable(
    interactionSource = remember { MutableInteractionSource() },
    indication = null
  ) {
    vibrator.cancel()
    onClick()
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
      val effect = VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK)
      vibrator.vibrate(effect)
    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      val effect = VibrationEffect.createOneShot(10, 1)
      vibrator.vibrate(effect)
    } else {
      vibrator.vibrate(10)
    }
  }
}

fun Modifier.noRippleClickable(onClick: () -> Unit): Modifier = composed {
  this.clickable(
    indication = null,
    interactionSource = remember { MutableInteractionSource() }) {
    onClick()
  }
}


// Enum to define vibration type for more clarity
enum class ScrollVibrationType {
  ON_ITEM_CHANGED,
  // Add ON_OFFSET_THRESHOLD if you implement that version
}

@Composable
fun rememberVibrator(): Vibrator {
  val context = LocalContext.current
  return remember {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
      val vibratorManager =
        context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as android.os.VibratorManager
      vibratorManager.defaultVibrator
    } else {
      @Suppress("DEPRECATION")
      context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    }
  }
}


fun Vibrator.tick() {
  this.cancel()
  if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
    val effect = VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK)
    this.vibrate(effect)
  } else {
    this.vibrate(10)
  }
}


@Suppress("Deprecated")
@Composable
fun ScrollHaptics(
  listState: LazyListState,
  vibrationType: ScrollVibrationType = ScrollVibrationType.ON_ITEM_CHANGED,
  enabled: Boolean = true,
  effectTickEnabled: Boolean = true, // Prefer EFFECT_TICK if available
  oneShotDurationMillis: Long = 10L // Short and subtle
) {
  if (!enabled) return

  val vibrator = rememberVibrator()

  LaunchedEffect(listState, vibrationType, vibrator, effectTickEnabled, oneShotDurationMillis) {
    if (!vibrator.hasVibrator()) return@LaunchedEffect
    when (vibrationType) {
      ScrollVibrationType.ON_ITEM_CHANGED -> {
        snapshotFlow {
          listState.firstVisibleItemIndex
        }
          .distinctUntilChanged()
          .filter { index ->
            listState.isScrollInProgress || (index != 0 && listState.layoutInfo.visibleItemsInfo.isNotEmpty() && listState.layoutInfo.visibleItemsInfo.first().index == index)
          }
          .collect {
            val vibrationEffect =
              if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && effectTickEnabled) {
                // Android 9 使用系统预定义的 tick 震动
                VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK)
              } else {
                // Android 8 10ms 强度1的 震动
                VibrationEffect.createOneShot(oneShotDurationMillis, 1)
              }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
              vibrator.vibrate(vibrationEffect)
            } else {
              // android 8以下 无法设置强度和波形 直接震动20ms
              vibrator.vibrate(20)
            }
          }
      }
    }
  }
}