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
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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

/**
 * 音质等级 - 根据优化后的分级系统
 */
private enum class QualityLevel {
    EXCELLENT, // Level 4: 母带、杜比全景声、DTS-HD MA
    GOOD, // Level 3: Hi-Res、多声道无损
    STANDARD, // Level 2: CD 无损、超高品质有损
    LOW, // Level 1: 标准/高品质有损、普通环绕声
    POOR, // Level 0: 低品质、未知
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

    Row(
        modifier = modifier.padding(horizontal = 4.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        when (audioQuality) {
            // 顶级音质 - 母带品质
            QualityType.HI_RES_STUDIO_MASTER -> {
                QualityBadge(
                    text = "MASTER",
                    icon = Icons.Rounded.HighQuality,
                    qualityLevel = QualityLevel.EXCELLENT,
                )
            }

            // 顶级音质 - 杜比全景声/无损
            QualityType.DOLBY_LOSSLESS -> {
                QualityBadge(
                    text = "ATMOS",
                    icon = Icons.Rounded.SurroundSound,
                    qualityLevel = QualityLevel.EXCELLENT,
                )
            }

            // 顶级音质 - DTS-HD MA
            QualityType.DTS_HD_MA -> {
                QualityBadge(
                    text = "DTS-HD",
                    icon = Icons.Rounded.SurroundSound,
                    qualityLevel = QualityLevel.EXCELLENT,
                )
            }

            // 优秀音质 - Hi-Res 无损
            QualityType.HI_RES_LOSSLESS -> {
                QualityBadge(
                    text = "Hi-Res",
                    icon = Icons.Rounded.HighQuality,
                    qualityLevel = QualityLevel.GOOD,
                )
            }

            // 优秀音质 - 多声道无损
            QualityType.FLAC_SURROUND -> {
                QualityBadge(
                    text = "5.1",
                    icon = Icons.Rounded.SurroundSound,
                    qualityLevel = QualityLevel.GOOD,
                )
            }

            // 良好音质 - CD 无损
            QualityType.CD_QUALITY_LOSSLESS -> {
                QualityBadge(
                    text = song.codec.uppercase(),
                    icon = Icons.Rounded.HighQuality,
                    qualityLevel = QualityLevel.STANDARD,
                )
            }

            // 良好音质 - 超高品质有损
            QualityType.LOSSY_HIGH -> {
                QualityBadge(
                    text = "${song.bitRate / 1000}K",
                    icon = Icons.Rounded.HighQuality,
                    qualityLevel = QualityLevel.STANDARD,
                )
            }

            // 标准音质 - 高品质有损
            QualityType.LOSSY_STANDARD -> {
                QualityBadge(
                    text = "${song.bitRate / 1000}K",
                    icon = Icons.Rounded.HighQuality,
                    qualityLevel = QualityLevel.LOW,
                )
            }

            // 标准音质 - 杜比数字环绕声
            QualityType.DOLBY_DIGITAL -> {
                QualityBadge(
                    text = "DOLBY",
                    icon = Icons.Rounded.SurroundSound,
                    qualityLevel = QualityLevel.LOW,
                )
            }

            // 标准音质 - DTS 环绕声
            QualityType.DTS_SURROUND -> {
                QualityBadge(
                    text = "DTS",
                    icon = Icons.Rounded.SurroundSound,
                    qualityLevel = QualityLevel.LOW,
                )
            }

            // 低音质
            QualityType.LOSSY_LOW -> {
                QualityBadge(
                    text = "${song.bitRate / 1000}K",
                    icon = Icons.Rounded.HighQuality,
                    qualityLevel = QualityLevel.POOR,
                )
            }

            // 未知
            QualityType.UNKNOWN -> {
                QualityBadge(
                    text = song.codec.uppercase(),
                    icon = Icons.Rounded.HighQuality,
                    qualityLevel = QualityLevel.POOR,
                )
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
            // 顶级 - 强调色
            QualityLevel.EXCELLENT ->
                listOf(
                    MaterialTheme.colorScheme.primary,
                    MaterialTheme.colorScheme.primary,
                ) to MaterialTheme.colorScheme.onPrimary

            // 优秀 - 主色容器
            QualityLevel.GOOD ->
                listOf(
                    MaterialTheme.colorScheme.primaryContainer,
                    MaterialTheme.colorScheme.primaryContainer,
                ) to MaterialTheme.colorScheme.onPrimaryContainer

            // 良好 - 次要色容器
            QualityLevel.STANDARD ->
                listOf(
                    MaterialTheme.colorScheme.secondaryContainer,
                    MaterialTheme.colorScheme.secondaryContainer,
                ) to MaterialTheme.colorScheme.onSecondaryContainer

            // 标准 - 第三色容器
            QualityLevel.LOW ->
                listOf(
                    MaterialTheme.colorScheme.tertiaryContainer,
                    MaterialTheme.colorScheme.tertiaryContainer,
                ) to MaterialTheme.colorScheme.onTertiaryContainer

            // 低质量 - 表面变体
            QualityLevel.POOR ->
                listOf(
                    MaterialTheme.colorScheme.surfaceVariant,
                    MaterialTheme.colorScheme.surfaceVariant,
                ) to MaterialTheme.colorScheme.onSurfaceVariant
        }

    Box(
        modifier =
            Modifier
                .clip(Shapes.ExtraSmallCornerBasedShape)
                .background(brush = Brush.horizontalGradient(gradientColors))
                .padding(horizontal = 4.dp, vertical = 2.dp),
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
                    MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 0.8.sp,
                    ),
                color = contentColor,
            )
        }
    }
}
