package me.spica27.spicamusic.ui.widget

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun StatusCard(
    containerColor: Color,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors =
            CardDefaults.defaultColors(
                color = containerColor,
                contentColor = containerColor,
            ),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
            content = content,
        )
    }
}

@Composable
fun StatusCardHeader(
    icon: ImageVector,
    title: String,
    titleColor: Color,
    modifier: Modifier = Modifier,
    iconTint: Color = titleColor,
    trailing: (@Composable RowScope.() -> Unit)? = null,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(24.dp),
            )
            androidx.compose.foundation.layout
                .Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = title,
                style = MiuixTheme.textStyles.title4,
                color = titleColor,
            )
        }
        trailing?.invoke(this)
    }
}

@Composable
fun StatusCardBodyText(
    text: String,
    color: Color,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text,
        style = MiuixTheme.textStyles.body1,
        color = color,
        modifier = modifier,
    )
}

@Composable
fun StatusKeyValueRow(
    label: String,
    value: String,
    labelColor: Color,
    valueColor: Color,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = label,
            style = MiuixTheme.textStyles.body2,
            color = labelColor,
        )
        Text(
            text = value,
            style = MiuixTheme.textStyles.body2,
            color = valueColor,
        )
    }
}
