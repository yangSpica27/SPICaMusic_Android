package me.spica27.spicamusic.ui.dialog

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import me.spica27.spicamusic.R
import me.spica27.spicamusic.ui.navigation.LocalBackStack
import me.spica27.spicamusic.ui.theme.Shapes
import me.spica27.spicamusic.ui.theme.Spacing
import me.spica27.spicamusic.ui.widget.clickHighlight

@Immutable
data class SortMenuOption(
    val id: String,
    @field:StringRes val labelRes: Int,
    val icon: ImageVector,
)

@Composable
fun SortMenuDialogContent(
    options: List<SortMenuOption>,
    selectedId: String,
    onSelect: (String) -> Unit,
) {
    val backStack = LocalBackStack.current

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = Shapes.ExtraLarge1CornerBasedShape,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        tonalElevation = 6.dp,
        shadowElevation = 8.dp,
    ) {
        Column(
            modifier =
                Modifier
                    .width(236.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = Spacing.Small, vertical = Spacing.Medium),
        ) {
            Text(
                text = stringResource(R.string.music_sort_cd),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = Spacing.Small, bottom = Spacing.Small),
            )
            options.forEach { option ->
                SortMenuRow(
                    option = option,
                    selected = option.id == selectedId,
                    onClick = {
                        onSelect(option.id)
                        backStack.removeLastOrNull()
                    },
                )
            }
        }
    }
}

@Composable
private fun SortMenuRow(
    option: SortMenuOption,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val contentColor =
        if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(vertical = 2.dp)
                .clip(Shapes.LargeCornerBasedShape)
                .clickHighlight(onClick = onClick)
                .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Surface(
            modifier = Modifier.size(32.dp),
            shape = Shapes.MediumCornerBasedShape,
            color =
                if (selected) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f)
                },
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = option.icon,
                    contentDescription = null,
                    tint =
                        if (selected) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSecondaryContainer
                        },
                    modifier = Modifier.size(18.dp),
                )
            }
        }
        Text(
            text = stringResource(option.labelRes),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = contentColor,
            maxLines = 1,
            modifier =
                Modifier
                    .weight(1f)
                    .padding(horizontal = Spacing.Medium),
        )
        if (selected) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}
