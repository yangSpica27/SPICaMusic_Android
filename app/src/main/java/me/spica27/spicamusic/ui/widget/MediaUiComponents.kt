package me.spica27.spicamusic.ui.widget

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.PressFeedbackType

object MediaUiDefaults {
    val placeholderTint
        @Composable get() = MiuixTheme.colorScheme.onSurfaceVariantActions.copy(alpha = 0.3f)

    val emptyTitleColor
        @Composable get() = MiuixTheme.colorScheme.onSurfaceVariantActions.copy(alpha = 0.6f)

    val emptySummaryColor
        @Composable get() = MiuixTheme.colorScheme.onSurfaceVariantActions.copy(alpha = 0.5f)
}

@Composable
fun EmptyStateContent(
    icon: ImageVector,
    title: String,
    summary: String? = null,
    modifier: Modifier = Modifier,
    iconSize: Dp = 48.dp,
    action: (@Composable ColumnScope.() -> Unit)? = null,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(iconSize),
            tint = MediaUiDefaults.placeholderTint,
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = title,
            style = MiuixTheme.textStyles.title4,
            color = MediaUiDefaults.emptyTitleColor,
        )
        if (summary != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = summary,
                style = MiuixTheme.textStyles.body2,
                color = MediaUiDefaults.emptySummaryColor,
            )
        }
        action?.invoke(this)
    }
}

@Composable
fun EmptyStateCard(
    title: String,
    summary: String? = null,
    icon: ImageVector,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    iconSize: Dp = 48.dp,
    action: (@Composable ColumnScope.() -> Unit)? = null,
) {
    Card(
        onClick = onClick,
        pressFeedbackType = PressFeedbackType.Sink,
        cornerRadius = 10.dp,
        modifier = modifier,
    ) {
        EmptyStateContent(
            icon = icon,
            title = title,
            summary = summary,
            iconSize = iconSize,
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(16.dp),
            action = action,
        )
    }
}

@Composable
fun GradientPlaceholderCover(
    modifier: Modifier = Modifier,
    gradientColors: List<Color> =
        listOf(
            MiuixTheme.colorScheme.tertiaryContainer,
            MiuixTheme.colorScheme.surfaceContainerHigh,
        ),
    overlayText: String? = null,
    icon: ImageVector = Icons.Default.MusicNote,
    iconSize: Dp = 36.dp,
    contentDescription: String? = null,
    textPaddingValues: PaddingValues = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
) {
    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .background(Brush.verticalGradient(gradientColors))
                .padding(textPaddingValues),
        contentAlignment = Alignment.Center,
    ) {
        if (overlayText != null) {
            Text(
                text = overlayText,
                style = MiuixTheme.textStyles.headline1,
                color = MediaUiDefaults.placeholderTint,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        } else {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                modifier = Modifier.size(iconSize),
                tint = MediaUiDefaults.placeholderTint,
            )
        }
    }
}

@Composable
fun MediaMiniCardText(
    title: String,
    subtitle: String? = null,
    modifier: Modifier = Modifier,
    titleMaxLines: Int = 1,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = title,
            style = SongListDefaults.songTitleTextStyle,
            maxLines = titleMaxLines,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
        )
        if (subtitle != null) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = subtitle,
                style = SongListDefaults.songMetaTextStyle,
                color = MiuixTheme.colorScheme.onSurfaceContainerVariant,
                maxLines = 1,
                modifier = Modifier.padding(horizontal = 12.dp),
            )
            Spacer(modifier = Modifier.height(10.dp))
        }
    }
}

@Composable
fun MediaMiniCardFrame(
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    cover: @Composable BoxScope.() -> Unit,
    text: @Composable ColumnScope.() -> Unit,
) {
    Card(
        onClick = onClick,
        pressFeedbackType = PressFeedbackType.Sink,
        cornerRadius = 10.dp,
        modifier = modifier,
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .size(120.dp),
                contentAlignment = Alignment.Center,
                content = cover,
            )
            text()
        }
    }
}
