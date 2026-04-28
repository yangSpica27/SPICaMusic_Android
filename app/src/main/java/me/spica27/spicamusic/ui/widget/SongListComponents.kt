package me.spica27.spicamusic.ui.widget

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import me.spica27.spicamusic.ui.theme.Shapes
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.Surface
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

object SongListDefaults {
    val actionLabelTextStyle
        @Composable get() = MiuixTheme.textStyles.body1.copy(fontWeight = FontWeight.Medium)

    val songTitleTextStyle
        @Composable get() = MiuixTheme.textStyles.body1.copy(fontWeight = FontWeight.Medium)

    val songMetaTextStyle
        @Composable get() = MiuixTheme.textStyles.body2

    val songDurationTextStyle
        @Composable get() = MiuixTheme.textStyles.body2.copy(fontSize = 14.sp)

    val statPrimaryTextStyle
        @Composable get() = MiuixTheme.textStyles.body2.copy(fontWeight = FontWeight.W500, fontSize = 12.sp)

    val statSecondaryTextStyle
        @Composable get() = MiuixTheme.textStyles.body2.copy(fontSize = 11.sp)

    val sectionHeaderTextStyle
        @Composable get() = MiuixTheme.textStyles.body2.copy(fontWeight = FontWeight.W600)

    val menuLabelTextStyle
        @Composable get() = MiuixTheme.textStyles.body2.copy(fontSize = 12.sp)
}

@Composable
fun LibraryActionCard(
    icon: ImageVector,
    label: String,
    containerColor: Color,
    contentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        onClick = onClick,
        modifier = modifier,
        colors =
            CardDefaults.defaultColors(
                color = containerColor,
                contentColor = containerColor,
            ),
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(24.dp),
            )
            Text(
                text = label,
                style = SongListDefaults.actionLabelTextStyle,
                color = contentColor,
                modifier = Modifier.padding(start = 8.dp),
            )
        }
    }
}

@Composable
fun IconActionCard(
    icon: ImageVector,
    contentDescription: String?,
    containerColor: Color,
    contentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    iconSize: Int = 24,
    cardSize: Int = 48,
) {
    Card(
        onClick = onClick,
        modifier = modifier.size(cardSize.dp),
        colors =
            CardDefaults.defaultColors(
                color = containerColor,
                contentColor = containerColor,
            ),
        cornerRadius = 16.dp,
    ) {
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(12.dp),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = contentColor,
                modifier = Modifier.size(iconSize.dp),
            )
        }
    }
}

@Composable
fun SelectionMenuActionItem(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .padding(8.dp)
                .combinedClickable(
                    onClick = onClick,
                    indication = ripple(bounded = false, radius = 32.dp),
                    interactionSource = MutableInteractionSource(),
                ),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = MiuixTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier.size(28.dp),
        )
        Text(
            text = label,
            style = SongListDefaults.menuLabelTextStyle,
            color = MiuixTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}

@Composable
fun RankedSongRow(
    rank: Int,
    title: String,
    subtitle: String,
    coverUri: Uri?,
    trailingPrimaryText: String,
    trailingSecondaryText: String,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    surfaceColor: Color = Color.Transparent,
    shape: Shape = Shapes.MediumCornerBasedShape,
    placeholderContainerColor: Color = MiuixTheme.colorScheme.surfaceContainer,
    titleColor: Color = MiuixTheme.colorScheme.onSurface,
    subtitleColor: Color = MiuixTheme.colorScheme.onSurfaceSecondary,
    trailingPrimaryColor: Color = MiuixTheme.colorScheme.onSurfaceContainer,
    trailingSecondaryColor: Color = MiuixTheme.colorScheme.onSurfaceContainerVariant,
) {
    val titleTextStyle = SongListDefaults.songTitleTextStyle
    val metaTextStyle = SongListDefaults.songMetaTextStyle
    val primaryStatTextStyle = SongListDefaults.statPrimaryTextStyle
    val secondaryStatTextStyle = SongListDefaults.statSecondaryTextStyle

    @Composable
    fun Content() {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier.width(28.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = rank.toString(),
                    style = MiuixTheme.textStyles.body2,
                    color =
                        if (rank <= 3) {
                            MiuixTheme.colorScheme.primary
                        } else {
                            MiuixTheme.colorScheme.onSurfaceContainerVariant
                        },
                    fontWeight = if (rank <= 3) FontWeight.Bold else FontWeight.Normal,
                )
            }

            AudioCover(
                uri = coverUri,
                modifier =
                    Modifier
                        .size(44.dp)
                        .clip(Shapes.SmallCornerBasedShape),
                placeHolder = {
                    Box(
                        modifier =
                            Modifier
                                .fillMaxSize()
                                .clip(Shapes.SmallCornerBasedShape)
                                .background(placeholderContainerColor),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Default.MusicNote,
                            contentDescription = null,
                            tint = MiuixTheme.colorScheme.onSurfaceContainerVariant,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                },
            )

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = titleTextStyle,
                    color = titleColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(modifier = Modifier.size(2.dp))
                Text(
                    text = subtitle,
                    style = metaTextStyle,
                    color = subtitleColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = trailingPrimaryText,
                    style = primaryStatTextStyle,
                    color = trailingPrimaryColor,
                )
                Spacer(modifier = Modifier.size(2.dp))
                Text(
                    text = trailingSecondaryText,
                    style = secondaryStatTextStyle,
                    color = trailingSecondaryColor,
                )
            }
        }
    }

    if (onClick == null) {
        Box(modifier = modifier.fillMaxWidth()) {
            Content()
        }
    } else {
        Surface(
            modifier = modifier.fillMaxWidth(),
            onClick = onClick,
            color = surfaceColor,
            shape = shape,
        ) {
            Content()
        }
    }
}

@Composable
fun SongArtworkThumbnail(
    coverUri: Uri?,
    modifier: Modifier = Modifier,
    containerColor: Color = MiuixTheme.colorScheme.surfaceVariant,
    placeholderContainerColor: Color = MiuixTheme.colorScheme.surfaceContainerHigh,
    placeholderIconTint: Color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
    placeholder: (@Composable BoxScope.() -> Unit)? = null,
) {
    Box(
        modifier =
            modifier
                .clip(Shapes.SmallCornerBasedShape)
                .background(containerColor),
        contentAlignment = Alignment.Center,
    ) {
        AudioCover(
            uri = coverUri,
            modifier = Modifier.fillMaxSize(),
            placeHolder = {
                if (placeholder != null) {
                    placeholder()
                } else {
                    Box(
                        modifier =
                            Modifier
                                .fillMaxSize()
                                .background(placeholderContainerColor),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Default.MusicNote,
                            contentDescription = null,
                            tint = placeholderIconTint,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }
            },
        )
    }
}

@Composable
fun CompactSongRow(
    title: String,
    subtitle: String,
    coverUri: Uri?,
    modifier: Modifier = Modifier,
    titleColor: Color = MiuixTheme.colorScheme.onSurface,
    subtitleColor: Color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
    coverSize: Dp = 44.dp,
    coverContainerColor: Color = MiuixTheme.colorScheme.surfaceVariant,
    placeholderContainerColor: Color = MiuixTheme.colorScheme.surfaceContainerHigh,
    placeholderIconTint: Color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
    leading: (@Composable RowScope.() -> Unit)? = null,
    trailing: (@Composable RowScope.() -> Unit)? = null,
    coverPlaceholder: (@Composable BoxScope.() -> Unit)? = null,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (leading != null) {
            leading()
            Spacer(modifier = Modifier.size(12.dp))
        }

        SongArtworkThumbnail(
            coverUri = coverUri,
            modifier = Modifier.size(coverSize),
            containerColor = coverContainerColor,
            placeholderContainerColor = placeholderContainerColor,
            placeholderIconTint = placeholderIconTint,
            placeholder = coverPlaceholder,
        )

        Spacer(modifier = Modifier.size(12.dp))

        Column(
            modifier = Modifier.weight(1f),
        ) {
            Text(
                text = title,
                style = SongListDefaults.songTitleTextStyle,
                color = titleColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = subtitle,
                style = SongListDefaults.songMetaTextStyle,
                color = subtitleColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        if (trailing != null) {
            Spacer(modifier = Modifier.size(12.dp))
            trailing()
        }
    }
}
