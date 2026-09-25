package me.spica27.spicamusic.ui.dialog

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import me.spica27.spicamusic.R
import me.spica27.spicamusic.ui.component.DialogMenuItem
import me.spica27.spicamusic.ui.component.PopupMenuContainer
import me.spica27.spicamusic.ui.navigation.LocalBackStack
import me.spica27.spicamusic.ui.theme.Spacing

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

    PopupMenuContainer(modifier = Modifier.width(236.dp)) {
        Column(
            modifier =
                Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = Spacing.Medium),
        ) {
            Text(
                text = stringResource(R.string.music_sort_cd),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = Spacing.Large, bottom = Spacing.Small),
            )
            options.forEachIndexed { index, option ->
                val isSelected = option.id == selectedId
                DialogMenuItem(
                    title = stringResource(option.labelRes),
                    icon = option.icon,
                    onClick = {
                        onSelect(option.id)
                        backStack.removeLastOrNull()
                    },
                    showDivider = index < options.lastIndex,
                    trailing =
                        if (isSelected) {
                            {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp),
                                )
                            }
                        } else {
                            null
                        },
                )
            }
        }
    }
}
