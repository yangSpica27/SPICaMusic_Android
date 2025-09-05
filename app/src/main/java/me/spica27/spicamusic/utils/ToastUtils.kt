package me.spica27.spicamusic.utils

import android.widget.Toast
import me.spica27.spicamusic.App

object ToastUtils {
    fun showToast(message: String) {
        Toast.makeText(App.getInstance(), message, Toast.LENGTH_SHORT).show()
    }
}
