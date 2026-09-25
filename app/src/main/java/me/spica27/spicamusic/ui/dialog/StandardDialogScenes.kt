package me.spica27.spicamusic.ui.dialog

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import me.spica27.spicamusic.ui.component.DialogContainer
import me.spica27.spicamusic.ui.navigation.LocalBackStack
import me.spica27.spicamusic.ui.theme.Spacing

@Composable
fun TextInputDialogContent(
    title: String,
    initialValue: String,
    label: String,
    confirmLabel: String,
    dismissLabel: String,
    onConfirm: (String, dismiss: () -> Unit) -> Unit,
) {
    val backStack = LocalBackStack.current
    var value by remember { mutableStateOf(initialValue) }
    val dismiss = {
        backStack.removeLastOrNull()
        Unit
    }

    fun submit() {
        if (value.isNotBlank()) {
            onConfirm(value.trim(), dismiss)
        }
    }

    DialogContainer {
        Column(
            modifier = Modifier.padding(Spacing.ExtraLarge),
            verticalArrangement = Arrangement.spacedBy(Spacing.Large),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
            OutlinedTextField(
                value = value,
                onValueChange = { value = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(label) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions =
                    KeyboardActions(
                        onDone = {
                            submit()
                        },
                    ),
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                TextButton(onClick = dismiss) {
                    Text(dismissLabel)
                }
                TextButton(
                    onClick = ::submit,
                    enabled = value.isNotBlank(),
                ) {
                    Text(confirmLabel)
                }
            }
        }
    }
}

@Composable
fun ConfirmationDialogContent(
    title: String,
    message: String,
    confirmLabel: String,
    dismissLabel: String? = null,
    icon: ImageVector? = null,
    destructive: Boolean = false,
    onConfirm: (dismiss: () -> Unit) -> Unit = { dismiss -> dismiss() },
) {
    val backStack = LocalBackStack.current
    val dismiss = {
        backStack.removeLastOrNull()
        Unit
    }

    DialogContainer {
        Column(
            modifier = Modifier.padding(Spacing.ExtraLarge),
            verticalArrangement = Arrangement.spacedBy(Spacing.Large),
        ) {
            icon?.let {
                Surface(
                    modifier = Modifier.size(52.dp),
                    shape = CircleShape,
                    color =
                        if (destructive) {
                            MaterialTheme.colorScheme.errorContainer
                        } else {
                            MaterialTheme.colorScheme.primaryContainer
                        },
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = it,
                            contentDescription = null,
                            tint =
                                if (destructive) {
                                    MaterialTheme.colorScheme.onErrorContainer
                                } else {
                                    MaterialTheme.colorScheme.onPrimaryContainer
                                },
                            modifier = Modifier.size(26.dp),
                        )
                    }
                }
            }
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                dismissLabel?.let {
                    TextButton(onClick = dismiss) {
                        Text(it)
                    }
                }
                TextButton(
                    onClick = { onConfirm(dismiss) },
                    colors =
                        if (destructive) {
                            ButtonDefaults.textButtonColors(
                                contentColor = MaterialTheme.colorScheme.error,
                            )
                        } else {
                            ButtonDefaults.textButtonColors()
                        },
                ) {
                    Text(confirmLabel)
                }
            }
        }
    }
}
