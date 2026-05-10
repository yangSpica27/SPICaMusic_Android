package me.spica27.spicamusic.ui.widget

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asComposePath
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.graphics.shapes.Morph
import androidx.graphics.shapes.toPath

class CustomRotatingMorphShape(
    private val morph: Morph,
    private val percentage: Float,
    private val rotation: Float,
) : Shape {
    private val matrix = Matrix()

    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density,
    ): Outline {
        // Below assumes that you haven't changed the default radius of 1f, nor the centerX and centerY of 0f
        // By default this stretches the path to the size of the container, if you don't want stretching, use the same size.width for both x and y.
        matrix.scale(size.width / 2f, size.height / 2f)
        matrix.translate(1f, 1f)
        matrix.rotateZ(rotation)

        val path = morph.toPath(progress = percentage).asComposePath()
        path.transform(matrix)

        return Outline.Generic(path)
    }
}
