package me.spica27.spicamusic.ui.widget

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import me.spica27.spicamusic.ui.theme.Shapes

object LyricsControlDefaults {
    val surfaceColor
        @Composable get() = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f)

    val pillLabelTextStyle
        @Composable get() = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium, fontSize = 13.sp)

    val badgeTextStyle
        @Composable get() = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
}

@Composable
fun LyricsCircleButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 40.dp,
    containerColor: Color = LyricsControlDefaults.surfaceColor,
    content: @Composable () -> Unit,
) {
    Box(
        modifier =
            modifier
                .size(size)
                .clip(CircleShape)
                .background(containerColor)
                .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}

@Composable
fun LyricsPill(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    shape: Shape = Shapes.ExtraLargeCornerBasedShape,
    horizontalPadding: Dp = 12.dp,
    verticalPadding: Dp = 8.dp,
    containerColor: Color = LyricsControlDefaults.surfaceColor,
    content: @Composable RowScope.() -> Unit,
) {
    val clickableModifier =
        if (onClick == null) {
            Modifier
        } else {
            Modifier.clickable(onClick = onClick)
        }

    Row(
        modifier =
            modifier
                .clip(shape)
                .background(containerColor)
                .then(clickableModifier)
                .padding(horizontal = horizontalPadding, vertical = verticalPadding),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        content = content,
    )
}

@Composable
fun LyricsBadge(
    text: String,
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
    contentColor: Color = MaterialTheme.colorScheme.primary,
) {
    Box(
        modifier =
            modifier
                .clip(CircleShape)
                .background(containerColor)
                .padding(horizontal = 10.dp, vertical = 4.dp),
    ) {
        Text(
            text = text,
            style = LyricsControlDefaults.badgeTextStyle,
            color = contentColor,
        )
    }
}
