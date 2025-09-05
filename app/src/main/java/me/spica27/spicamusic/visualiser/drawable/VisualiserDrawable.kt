package me.spica27.spicamusic.visualiser.drawable

import android.graphics.Canvas

abstract class VisualiserDrawable {
    // 半径
    var radius = 0

    // 主题色
    var themeColor = 0

    // 二级颜色
    var secondaryColor = 0

    // 背景颜色
    var backgroundColor = 0
    var width = 0

    var height = 0

    open fun setBounds(
        width: Int,
        height: Int,
    ) {
        this.width = width
        this.height = height
    }

    abstract fun draw(canvas: Canvas)

    abstract fun update(list: List<Float>)
}
