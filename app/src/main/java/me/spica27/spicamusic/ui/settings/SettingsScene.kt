package me.spica27.spicamusic.ui.settings

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Album
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material.icons.filled.Brightness6
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.common.collect.ImmutableList
import me.spica27.navkit.path.LocalNavigationPath
import me.spica27.navkit.scene.StackScene
import me.spica27.spicamusic.R
import me.spica27.spicamusic.common.entity.DynamicCoverType
import me.spica27.spicamusic.common.entity.DynamicSpectrumBackground
import me.spica27.spicamusic.common.entity.ProgressBarStyle
import me.spica27.spicamusic.ui.about.AboutScene
import me.spica27.spicamusic.ui.theme.LayoutTokens
import me.spica27.spicamusic.ui.theme.Shapes
import me.spica27.spicamusic.ui.theme.Spacing
import me.spica27.spicamusic.ui.widget.rememberIOSOverScrollEffect
import org.koin.compose.viewmodel.koinViewModel

class SettingsScene : StackScene() {
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val path = LocalNavigationPath.current
        val viewModel: SettingsViewModel = koinViewModel()

        val darkMode by viewModel.darkMode.collectAsStateWithLifecycle()
        val keepScreenOn by viewModel.keepScreenOn.collectAsStateWithLifecycle()
        val spectrumValue by viewModel.dynamicSpectrumBackground.collectAsStateWithLifecycle()
        val coverTypeValue by viewModel.dynamicCoverType.collectAsStateWithLifecycle()
        val progressBarStyleValue by viewModel.progressBarStyle.collectAsStateWithLifecycle()
        val dynamicWaveformLabel = stringResource(R.string.progress_bar_style_dynamic_waveform)
        val timeDomainWaveformLabel = stringResource(R.string.progress_bar_style_time_domain_waveform)
        val progressBarStyleName =
            when (ProgressBarStyle.fromString(progressBarStyleValue)) {
                ProgressBarStyle.DynamicWaveform -> dynamicWaveformLabel
                ProgressBarStyle.TimeDomainWaveform -> timeDomainWaveformLabel
            }
        val progressBarStyleOptions =
            remember(dynamicWaveformLabel, timeDomainWaveformLabel) {
                ImmutableList.copyOf(
                    listOf(
                        SelectOption(ProgressBarStyle.DynamicWaveform.value, dynamicWaveformLabel),
                        SelectOption(ProgressBarStyle.TimeDomainWaveform.value, timeDomainWaveformLabel),
                    ),
                )
            }
        val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    scrollBehavior = scrollBehavior,
                    navigationIcon = {
                        IconButton(onClick = { path.popTop() }) {
                            Icon(
                                imageVector = Icons.Default.ArrowBackIosNew,
                                contentDescription = stringResource(R.string.back),
                                tint = MaterialTheme.colorScheme.onSurface,
                            )
                        }
                    },
                    title = {
                        Text(
                            text = stringResource(R.string.finder_settings_title),
                            fontWeight = FontWeight.SemiBold,
                        )
                    },
                    colors =
                        TopAppBarDefaults.topAppBarColors(
                            containerColor = Color.Transparent,
                            scrolledContainerColor = Color.Transparent,
                        ),
                )
            },
        ) { paddingValues ->
            Box(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    MaterialTheme.colorScheme.surface,
                                    MaterialTheme.colorScheme.surfaceContainerLow,
                                    MaterialTheme.colorScheme.surface,
                                ),
                            ),
                        ),
            ) {
                SettingsAmbientBackground()

                LazyColumn(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .nestedScroll(scrollBehavior.nestedScrollConnection),
                    contentPadding =
                        PaddingValues(
                            start = LayoutTokens.MusicHeaderHorizontalPadding,
                            end = LayoutTokens.MusicHeaderHorizontalPadding,
                            top = Spacing.Small + paddingValues.calculateTopPadding(),
                            bottom = Spacing.Huge + paddingValues.calculateBottomPadding(),
                        ),
                    verticalArrangement = Arrangement.spacedBy(Spacing.Large),
                    overscrollEffect = rememberIOSOverScrollEffect(orientation = Orientation.Vertical),
                ) {
                    item {
                        SettingsHeroCard(
                            darkMode = darkMode,
                            spectrumName = DynamicSpectrumBackground.fromString(spectrumValue).name,
                            coverName = DynamicCoverType.fromString(coverTypeValue).name,
                        )
                    }

                    item {
                        SettingsSectionCard(
                            title = stringResource(R.string.settings_appearance),
                            subtitle = stringResource(R.string.settings_appearance_subtitle),
                        ) {
                            ModernSettingsSwitchItem(
                                title = stringResource(R.string.settings_dark_mode_title),
                                subtitle = stringResource(R.string.settings_dark_mode_toggle_subtitle),
                                icon = Icons.Default.Brightness6,
                                checked = darkMode,
                                onCheckedChange = viewModel::setDarkMode,
                            )
                            SettingsItemDivider()
                            ModernSettingsSelectItem(
                                title = stringResource(R.string.settings_dynamic_spectrum),
                                subtitle = DynamicSpectrumBackground.fromString(spectrumValue).name,
                                icon = Icons.Default.GraphicEq,
                                options =
                                    ImmutableList.copyOf(
                                        DynamicSpectrumBackground.presets.map {
                                            SelectOption(it.value, it.name)
                                        },
                                    ),
                                currentValue = spectrumValue,
                                onValueChange = viewModel::setDynamicSpectrumBackground,
                            )
                            SettingsItemDivider()
                            ModernSettingsSelectItem(
                                title = stringResource(R.string.settings_dynamic_cover),
                                subtitle = DynamicCoverType.fromString(coverTypeValue).name,
                                icon = Icons.Default.Album,
                                options =
                                    ImmutableList.copyOf(
                                        DynamicCoverType.presets.map {
                                            SelectOption(it.value, it.name)
                                        },
                                    ),
                                currentValue = coverTypeValue,
                                onValueChange = viewModel::setDynamicCoverType,
                            )
                            SettingsItemDivider()
                            ModernSettingsSelectItem(
                                title = stringResource(R.string.settings_progress_bar_style),
                                subtitle = progressBarStyleName,
                                icon = Icons.Default.GraphicEq,
                                options = progressBarStyleOptions,
                                currentValue = progressBarStyleValue,
                                onValueChange = viewModel::setProgressBarStyle,
                            )
                        }
                    }

                    item {
                        SettingsSectionCard(
                            title = stringResource(R.string.settings_playback),
                            subtitle = stringResource(R.string.settings_playback_subtitle),
                        ) {
                            ModernSettingsSwitchItem(
                                title = stringResource(R.string.settings_keep_screen_on),
                                subtitle = stringResource(R.string.settings_keep_screen_on_subtitle),
                                icon = Icons.Default.Visibility,
                                checked = keepScreenOn,
                                onCheckedChange = viewModel::setKeepScreenOn,
                            )
                        }
                    }

                    item {
                        SettingsSectionCard(
                            title = stringResource(R.string.settings_about),
                            subtitle = stringResource(R.string.settings_about_subtitle),
                        ) {
                            SettingsRow(
                                title = stringResource(R.string.settings_about),
                                subtitle = stringResource(R.string.settings_about_subtitle),
                                icon = Icons.Default.Info,
                                selected = false,
                                onClick = { path.push(AboutScene()) },
                                trailingContent = {
                                    Icon(
                                        imageVector = Icons.Default.ChevronRight,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsAmbientBackground() {
    val transition = rememberInfiniteTransition(label = "settings_ambient")
    val drift by transition.animateFloat(
        initialValue = -18f,
        targetValue = 18f,
        animationSpec =
            infiniteRepeatable(
                animation = tween(durationMillis = 4600),
                repeatMode = RepeatMode.Reverse,
            ),
        label = "settings_orb_drift",
    )

    Box(Modifier.fillMaxSize()) {
        AmbientOrb(
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.22f),
            modifier =
                Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = 48.dp, y = (-28).dp)
                    .graphicsLayer {
                        translationY = drift
                        translationX = -drift * 0.5f
                    },
        )
        AmbientOrb(
            color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.16f),
            modifier =
                Modifier
                    .align(Alignment.CenterStart)
                    .offset(x = (-72).dp)
                    .graphicsLayer {
                        translationY = -drift * 0.7f
                        translationX = drift * 0.35f
                    },
        )
    }
}

@Composable
private fun AmbientOrb(
    color: Color,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier =
            modifier
                .size(220.dp)
                .blur(48.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors =
                            listOf(
                                color,
                                color.copy(alpha = 0f),
                            ),
                    ),
                ),
    )
}

@Composable
private fun SettingsHeroCard(
    darkMode: Boolean,
    spectrumName: String,
    coverName: String,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .clip(Shapes.ExtraLarge1CornerBasedShape)
                .background(
                    Brush.linearGradient(
                        listOf(
                            MaterialTheme.colorScheme.primaryContainer,
                            MaterialTheme.colorScheme.secondaryContainer,
                            MaterialTheme.colorScheme.tertiaryContainer,
                        ),
                    ),
                ).padding(Spacing.ExtraLarge),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.Large)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.Medium),
            ) {
                Box(
                    modifier =
                        Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Default.Brightness6,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(30.dp),
                    )
                }
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.ExtraSmall)) {
                    Text(
                        text = stringResource(R.string.settings_hero_title),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                    Text(
                        text = stringResource(R.string.settings_hero_subtitle),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.78f),
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.Small)) {
                SettingsPill(
                    label =
                        if (darkMode) {
                            stringResource(
                                R.string.settings_dark_mode_title,
                            )
                        } else {
                            stringResource(R.string.settings_light_mode_label)
                        },
                )
                SettingsPill(label = spectrumName)
                SettingsPill(label = coverName)
            }
        }
    }
}

@Composable
private fun SettingsPill(label: String) {
    AnimatedContent(
        targetState = label,
        transitionSpec = { fadeIn(tween(180)) togetherWith fadeOut(tween(120)) },
        label = "settings_pill_content",
    ) { targetLabel ->
        Text(
            text = targetLabel,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier =
                Modifier
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.10f))
                    .padding(horizontal = Spacing.Medium, vertical = Spacing.Small),
        )
    }
}

@Composable
private fun SettingsSectionCard(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .clip(Shapes.ExtraLargeCornerBasedShape)
                .background(MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.86f))
                .padding(vertical = Spacing.Large),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = Spacing.Large),
            verticalArrangement = Arrangement.spacedBy(Spacing.ExtraSmall),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(modifier = Modifier.height(Spacing.Small))
        content()
    }
}

@Composable
private fun ModernSettingsSwitchItem(
    title: String,
    subtitle: String,
    icon: ImageVector,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    val iconScale by animateFloatAsState(
        targetValue = if (checked) 1.08f else 1f,
        animationSpec = tween(durationMillis = 180),
        label = "settings_switch_icon_scale",
    )

    SettingsRow(
        title = title,
        subtitle = subtitle,
        icon = icon,
        selected = checked,
        onClick = { onCheckedChange(!checked) },
        iconModifier = Modifier.graphicsLayer(scaleX = iconScale, scaleY = iconScale),
        trailingContent = {
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
            )
        },
    )
}

@Composable
private fun ModernSettingsSelectItem(
    title: String,
    subtitle: String,
    icon: ImageVector,
    options: ImmutableList<SelectOption>,
    currentValue: String,
    onValueChange: (String) -> Unit,
) {
    var showDialog by remember { mutableStateOf(false) }

    SettingsRow(
        title = title,
        subtitle = subtitle,
        icon = icon,
        selected = false,
        onClick = { showDialog = true },
        trailingContent = {
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
    )

    if (showDialog) {
        SettingsSelectDialog(
            title = title,
            options = options,
            currentValue = currentValue,
            onDismiss = { showDialog = false },
            onValueChange = {
                onValueChange(it)
                showDialog = false
            },
        )
    }
}

@Composable
private fun SettingsRow(
    title: String,
    subtitle: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    iconModifier: Modifier = Modifier,
    trailingContent: @Composable () -> Unit,
) {
    val iconBackground by animateColorAsState(
        targetValue =
            if (selected) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceContainerHigh
            },
        label = "settings_icon_background",
    )
    val iconTint by animateColorAsState(
        targetValue =
            if (selected) {
                MaterialTheme.colorScheme.onPrimaryContainer
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
        label = "settings_icon_tint",
    )

    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(horizontal = Spacing.Large, vertical = Spacing.Medium),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.Medium),
    ) {
        Box(
            modifier =
                Modifier
                    .size(46.dp)
                    .clip(Shapes.LargeCornerBasedShape)
                    .background(iconBackground),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = iconModifier,
            )
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(Spacing.ExtraSmall),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        trailingContent()
    }
}

@Composable
private fun SettingsItemDivider() {
    Box(
        modifier =
            Modifier
                .padding(start = 78.dp, end = Spacing.Large)
                .fillMaxWidth()
                .height(1.dp)
                .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
    )
}

@Composable
private fun SettingsSelectDialog(
    title: String,
    options: ImmutableList<SelectOption>,
    currentValue: String,
    onDismiss: () -> Unit,
    onValueChange: (String) -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.Small)) {
                options.forEach { option ->
                    val selected = currentValue == option.value
                    Row(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .clip(Shapes.LargeCornerBasedShape)
                                .background(
                                    if (selected) {
                                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.72f)
                                    } else {
                                        MaterialTheme.colorScheme.surfaceContainerHigh
                                    },
                                ).clickable { onValueChange(option.value) }
                                .padding(horizontal = Spacing.Small, vertical = Spacing.ExtraSmall),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(Spacing.Small),
                    ) {
                        RadioButton(
                            selected = selected,
                            onClick = { onValueChange(option.value) },
                        )
                        Text(
                            text = option.label,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                            color =
                                if (selected) {
                                    MaterialTheme.colorScheme.onPrimaryContainer
                                } else {
                                    MaterialTheme.colorScheme.onSurface
                                },
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.close))
            }
        },
    )
}
