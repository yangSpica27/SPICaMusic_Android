@file:Suppress("unused")

package me.spica27.spicamusic.utils

import android.annotation.SuppressLint
import android.app.Activity
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.Drawable.ConstantState
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.MessageQueue
import android.view.View
import android.view.WindowInsets
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.annotation.BoolRes
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import androidx.annotation.IntegerRes
import androidx.annotation.RequiresApi
import androidx.annotation.StringRes
import androidx.core.app.ActivityOptionsCompat
import androidx.core.content.ContextCompat
import androidx.core.graphics.Insets
import androidx.fragment.app.Fragment
import me.spica27.spicamusic.MainActivity
import kotlin.reflect.KClass

/**
 * 获取版本号
 */
fun Context.getVersion(): String {
  return try {
    val packageInfo = packageManager.getPackageInfo(packageName, 0)
    packageInfo?.versionName?: "-1"
  } catch (e: Exception) {
    e.printStackTrace()
    "-1"
  }
}

// 隐藏软键盘
fun View.hideKeyboard() {
  val inputMethodManager =
    context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
  inputMethodManager.hideSoftInputFromWindow(windowToken, 0)
}

// 显示软键盘
fun View.showKeyboard() {
  if (requestFocus()) {
    val inputMethodManager =
      context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
    inputMethodManager.showSoftInput(this, InputMethodManager.SHOW_IMPLICIT)
  }
}

fun doOnMainThreadIdle(action: () -> Unit, timeout: Long? = null) {
  val handler = Handler(Looper.getMainLooper())

  val idleHandler = MessageQueue.IdleHandler {
    handler.removeCallbacksAndMessages(null)
    action()
    return@IdleHandler false
  }

  fun setupIdleHandler(queue: MessageQueue) {
    if (timeout != null) {
      handler.postDelayed({
        queue.removeIdleHandler(idleHandler)
        action()
      }, timeout)
    }
    queue.addIdleHandler(idleHandler)
  }

  if (Looper.getMainLooper() == Looper.myLooper()) {
    setupIdleHandler(Looper.myQueue())
  } else {
    setupIdleHandler(Looper.getMainLooper().queue)
  }
}


fun Context?.toast(text: CharSequence, duration: Int = Toast.LENGTH_LONG) =
  this?.let {
    doOnMainThreadIdle({
      Toast.makeText(it, text, duration).show()
    }, 200)
  }

fun Context?.toast(@StringRes textId: Int, duration: Int = Toast.LENGTH_LONG) =
  this?.let {
    doOnMainThreadIdle({
      Toast.makeText(it, textId, duration).show()
    }, 200)
  }

fun Fragment?.toast(text: CharSequence, duration: Int = Toast.LENGTH_LONG) =
  this?.let { activity.toast(text, duration) }

fun Fragment?.toast(@StringRes textId: Int, duration: Int = Toast.LENGTH_LONG) =
  this?.let { activity.toast(textId, duration) }

fun Context.getCompatColor(@ColorRes id: Int) = ContextCompat.getColor(this, id)

fun Context.getCompatDrawable(@DrawableRes id: Int) = ContextCompat.getDrawable(this, id)

fun Context.getInteger(@IntegerRes id: Int) = resources.getInteger(id)

fun Context.getBoolean(@BoolRes id: Int) = resources.getBoolean(id)

// startActivityWithAnimation
inline fun <reified T : Activity> Context.startActivityWithAnimation(
  enterResId: Int = 0,
  exitResId: Int = 0
) {
  val intent = Intent(this, T::class.java)
  val bundle = ActivityOptionsCompat.makeCustomAnimation(this, enterResId, exitResId).toBundle()
  ContextCompat.startActivity(this, intent, bundle)
}

inline fun <reified T : Activity> Context.startActivityWithAnimation(
  enterResId: Int = 0,
  exitResId: Int = 0,
  intentBody: Intent.() -> Unit
) {
  val intent = Intent(this, T::class.java)
  intent.intentBody()
  val bundle = ActivityOptionsCompat.makeCustomAnimation(this, enterResId, exitResId).toBundle()
  ContextCompat.startActivity(this, intent, bundle)
}

fun View.getBitmap(): Bitmap {
  val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
  val canvas = Canvas(bmp)
  draw(canvas)
  canvas.save()
  return bmp
}

fun Drawable.toBitmap(): Bitmap? {

  return if (this is BitmapDrawable) {
    this.bitmap
  } else {
    val constantState: ConstantState = constantState ?: return null
    val drawable = constantState.newDrawable().mutate()
    val bitmap = Bitmap.createBitmap(
      drawable.intrinsicWidth, drawable.intrinsicHeight,
      Bitmap.Config.ARGB_8888
    )
    val canvas = Canvas(bitmap)
    drawable.setBounds(0, 0, canvas.width, canvas.height)
    drawable.draw(canvas)
    bitmap
  }
}

val Int.dp: Float
  get() = android.util.TypedValue.applyDimension(
    android.util.TypedValue.COMPLEX_UNIT_DIP, this.toFloat(),
    Resources.getSystem().displayMetrics
  )

val Int.sp: Float
  get() = android.util.TypedValue.applyDimension(
    android.util.TypedValue.COMPLEX_UNIT_SP, this.toFloat(),
    Resources.getSystem().displayMetrics
  )


fun View.hide() {
  this.visibility = View.GONE
}


fun requireBackgroundThread() {
  check(Looper.myLooper() != Looper.getMainLooper()) {
    "This operation must be ran on a background thread"
  }
}

@SuppressLint("DiscouragedApi")
fun Context.getStatusBarHeight(): Int {
  var height = 0
  val resourceId =
    applicationContext.resources.getIdentifier("status_bar_height", "dimen", "android")
  if (resourceId > 0) {
    height = applicationContext.resources.getDimensionPixelSize(resourceId)
  }
  return height
}


val WindowInsets.systemBarInsetsCompat: Insets
  get() =
    when {
      Build.VERSION.SDK_INT >= Build.VERSION_CODES.R -> {
        // API 30+, use window inset map.
        getCompatInsets(WindowInsets.Type.systemBars())
      }
      // API 21+, use window inset fields.
      else -> getSystemWindowCompatInsets()
    }

@RequiresApi(Build.VERSION_CODES.R)
private fun WindowInsets.getCompatInsets(typeMask: Int) = Insets.toCompatInsets(getInsets(typeMask))

@Suppress("DEPRECATION")
private fun WindowInsets.getSystemWindowCompatInsets() =
  Insets.of(
    systemWindowInsetLeft,
    systemWindowInsetTop,
    systemWindowInsetRight,
    systemWindowInsetBottom
  )

fun lazyReflectedField(clazz: KClass<*>, field: String) = lazy {
  clazz.java.getDeclaredField(field).also { it.isAccessible = true }
}

fun lazyReflectedMethod(clazz: KClass<*>, method: String) = lazy {
  clazz.java.getDeclaredMethod(method).also { it.isAccessible = true }
}


const val DEFAULT_RES_CODE = 0xA0C0

fun Context.newBroadcastPendingIntent(action: String): PendingIntent =
  PendingIntent.getBroadcast(
    this,
    DEFAULT_RES_CODE,
    Intent(action).setFlags(Intent.FLAG_RECEIVER_REGISTERED_ONLY),
    PendingIntent.FLAG_IMMUTABLE
  )


fun Context.newMainPendingIntent(): PendingIntent =
  PendingIntent.getActivity(
    this,
    DEFAULT_RES_CODE,
    Intent(this, MainActivity::class.java),
    PendingIntent.FLAG_IMMUTABLE
  )