package me.spica27.spicamusic.widget

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import me.spica27.spicamusic.App
import me.spica27.spicamusic.R

/**
 * 输入文本弹框
 */
@Composable
fun InputTextDialog(
    onDismissRequest: () -> Unit,
    title: String,
    placeholder: String = App.getInstance().getString(R.string.default_edittext_hint),
    onConfirm: (String) -> Unit = { onDismissRequest.invoke() },
    onCancel: () -> Unit = { onDismissRequest.invoke() },
    defaultText: String = "",
) {
    // 重命名对话框
    val inputText = remember { mutableStateOf(defaultText) }
    AlertDialog(
        shape = MaterialTheme.shapes.large,
        onDismissRequest = {
            onDismissRequest.invoke()
        },
        title = { Text(title) },
        text = {
            TextField(
                value = inputText.value,
                onValueChange = { inputText.value = it },
                placeholder = { Text(placeholder) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                maxLines = 1,
                colors =
                    TextFieldDefaults.colors().copy(
                        disabledIndicatorColor = Color.Transparent,
                        errorIndicatorColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                    ),
            )
        },
        confirmButton = {
            TextButton(onClick = {
                onConfirm.invoke(inputText.value)
            }) {
                Text(
                    stringResource(android.R.string.ok),
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        },
        dismissButton = {
            TextButton(onClick = {
                onCancel.invoke()
            }) {
                Text(
                    stringResource(android.R.string.cancel),
                    color = MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        },
    )
}
