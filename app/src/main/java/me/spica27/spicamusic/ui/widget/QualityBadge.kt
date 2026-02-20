package me.spica27.spicamusic.ui.widget

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.HighQuality
import androidx.compose.material.icons.rounded.SurroundSound
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import me.spica27.spicamusic.common.entity.QualityType
import me.spica27.spicamusic.common.entity.Song
import me.spica27.spicamusic.common.entity.getQualityType
import me.spica27.spicamusic.ui.theme.Shapes
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

private enum class QualityLevel {
    EXCELLENT, // Studio Master, Dolby Atmos/TrueHD, DTS-HD MA
    GOOD, // Hi-Res Lossless, CD Quality Lossless
    STANDARD, // Dolby Digital, DTS, High-quality lossy
}

@Composable
fun AudioQualityBadges(
    song: Song,
    modifier: Modifier = Modifier,
) {
    val audioQuality =
        remember(song.songId) {
            song.getQualityType()
        }

    audioQuality.let { quality ->

        Row(
            modifier = modifier.padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Primary quality badge based on type with enhanced color coding
            when (audioQuality) {
                QualityType.HI_RES_STUDIO_MASTER -> {
                    QualityBadge(
                        text = "HI-RES",
                        icon = Icons.Rounded.HighQuality,
                        qualityLevel = QualityLevel.EXCELLENT,
                    )
                }

                QualityType.DOLBY_LOSSLESS -> {
                    val badgeText = "DOLBY"
                    QualityBadge(
                        text = badgeText,
                        icon = Icons.Rounded.SurroundSound,
                        qualityLevel = QualityLevel.EXCELLENT,
                    )
                }

                QualityType.DOLBY_LOSSY_SURROUND -> {
                    val badgeText = "DOLBY"
                    QualityBadge(
                        text = badgeText,
                        icon = Icons.Rounded.SurroundSound,
                        qualityLevel = QualityLevel.STANDARD,
                    )
                }

                QualityType.DTS_SURROUND -> {
                    val badgeText = "DTS"
                    QualityBadge(
                        text = badgeText,
                        icon = Icons.Rounded.SurroundSound,
                        qualityLevel = QualityLevel.EXCELLENT,
                    )
                }

                QualityType.HI_RES_LOSSLESS -> {
                    QualityBadge(
                        text = "HI-RES",
                        icon = Icons.Rounded.HighQuality,
                        qualityLevel = QualityLevel.GOOD,
                    )
                }

                QualityType.CD_QUALITY_LOSSLESS -> {
                    QualityBadge(
                        text = song.codec,
                        icon = Icons.Rounded.HighQuality,
                        qualityLevel = QualityLevel.GOOD,
                    )
                }

                QualityType.LOSSY_COMPRESSED -> {
                    QualityBadge(
                        text = "320K",
                        icon = Icons.Rounded.HighQuality,
                        qualityLevel = QualityLevel.STANDARD,
                    )
                }

                else -> {
                    QualityBadge(
                        text = song.codec,
                        icon = Icons.Rounded.SurroundSound,
                        qualityLevel = QualityLevel.STANDARD,
                    )
                }
            }
        }
    }
}

@Composable
private fun QualityBadge(
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    qualityLevel: QualityLevel,
    modifier: Modifier = Modifier,
) {
    val (gradientColors, contentColor) =
        when (qualityLevel) {
            QualityLevel.EXCELLENT ->
                listOf(
                    MiuixTheme.colorScheme.primaryContainer,
                    MiuixTheme.colorScheme.primaryContainer,
                ) to MiuixTheme.colorScheme.onPrimaryContainer

            QualityLevel.GOOD ->
                listOf(
                    MiuixTheme.colorScheme.primaryContainer,
                    MiuixTheme.colorScheme.primaryContainer,
                ) to MiuixTheme.colorScheme.onPrimaryContainer

            QualityLevel.STANDARD ->
                listOf(
                    MiuixTheme.colorScheme.tertiaryContainer,
                    MiuixTheme.colorScheme.tertiaryContainer,
                ) to MiuixTheme.colorScheme.onTertiaryContainer
        }

    Box(
        modifier =
            Modifier
                .clip(Shapes.ExtraSmallCornerBasedShape)
                .background(
                    brush = Brush.horizontalGradient(gradientColors),
                ).padding(horizontal = 4.dp, vertical = 2.dp),
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = contentColor,
            )
            Text(
                text = text,
                style =
                    MiuixTheme.textStyles.footnote2.copy(
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 0.8.sp,
                    ),
                color = contentColor,
            )
        }
    }
}
