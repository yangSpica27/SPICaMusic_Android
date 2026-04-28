package me.spica27.spicamusic.ui.widget

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import me.spica27.spicamusic.ui.theme.Shapes
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Composable
fun InfoSectionCard(
    title: String,
    modifier: Modifier = Modifier,
    containerColor: Color = MiuixTheme.colorScheme.surfaceContainerHigh,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .clip(Shapes.LargeCornerBasedShape)
                .background(containerColor)
                .padding(16.dp),
    ) {
        Text(
            text = title,
            style = MiuixTheme.textStyles.title4,
            fontWeight = FontWeight.Bold,
            color = MiuixTheme.colorScheme.onSurface,
        )

        Spacer(modifier = Modifier.height(12.dp))

        content()
    }
}

@Composable
fun InfoSectionRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    labelColor: Color = MiuixTheme.colorScheme.onSurface.copy(alpha = 0.6f),
    valueColor: Color = MiuixTheme.colorScheme.onSurface,
    valueMaxLines: Int = 3,
) {
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp),
    ) {
        Text(
            text = label,
            style = MiuixTheme.textStyles.title4,
            color = labelColor,
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            style = MiuixTheme.textStyles.body1,
            color = valueColor,
            maxLines = valueMaxLines,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
