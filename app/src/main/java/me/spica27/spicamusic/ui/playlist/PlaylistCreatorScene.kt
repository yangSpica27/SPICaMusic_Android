package me.spica27.spicamusic.ui.playlist

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import me.spica27.navkit.path.LocalNavigationPath
import me.spica27.navkit.scene.StackScene
import me.spica27.spicamusic.R
import me.spica27.spicamusic.common.entity.Song
import me.spica27.spicamusic.common.entity.getAlbumCoverUri
import me.spica27.spicamusic.common.entity.getCoverUri
import me.spica27.spicamusic.ui.theme.EaseOutEmphasized
import me.spica27.spicamusic.ui.theme.LayoutTokens
import me.spica27.spicamusic.ui.theme.ScaleEnterFrom
import me.spica27.spicamusic.ui.theme.Shapes
import me.spica27.spicamusic.ui.theme.Spacing
import me.spica27.spicamusic.ui.theme.entranceGraphics
import me.spica27.spicamusic.ui.theme.rememberEntrance
import me.spica27.spicamusic.ui.widget.AudioCover
import me.spica27.spicamusic.ui.widget.clickHighlight
import org.koin.compose.viewmodel.koinActivityViewModel
import kotlin.math.roundToInt

/** 歌单名称最大长度 */
private const val MAX_NAME_LENGTH = 40

/** 推荐歌单名 */
private val NAME_SUGGESTION_RES =
    listOf(
        R.string.playlist_suggestion_1,
        R.string.playlist_suggestion_2,
        R.string.playlist_suggestion_3,
        R.string.playlist_suggestion_4,
        R.string.playlist_suggestion_5,
    )

/**
 * 创建歌单页。
 */
class PlaylistCreatorScene : StackScene() {
    @Composable
    override fun Content() {
        val path = LocalNavigationPath.current
        val viewModel: PlaylistViewModel = koinActivityViewModel()
        val playlists by viewModel.playlists.collectAsStateWithLifecycle()
        val candidates by viewModel.creatorCandidates.collectAsStateWithLifecycle()

        var name by remember { mutableStateOf("") }
        // 提交过一次空名称后才显示"名称不能为空"，避免一进页面就红着
        var submittedEmpty by remember { mutableStateOf(false) }
        var selectedIds by remember { mutableStateOf(emptySet<Long>()) }
        val focusRequester = remember { FocusRequester() }
        val keyboardController = LocalSoftwareKeyboardController.current

        val trimmedName = name.trim()
        val duplicate =
            trimmedName.isNotEmpty() &&
                playlists.any { it.playlistName.equals(trimmedName, ignoreCase = true) }
        val canCreate = trimmedName.isNotEmpty() && !duplicate

        val errorText =
            when {
                duplicate -> stringResource(R.string.playlist_name_error_duplicate)
                submittedEmpty && trimmedName.isEmpty() ->
                    stringResource(R.string.playlist_name_error_empty)
                else -> null
            }

        // 错误时的水平抖动
        val shakeOffset = remember { Animatable(0f) }
        var shakeTrigger by remember { mutableIntStateOf(0) }

        LaunchedEffect(Unit) {
            // 等推场动画落定再唤起键盘，避免键盘上升与场景滑入互相抢帧
            waitAppear()
            focusRequester.requestFocus()
            keyboardController?.show()
        }

        LaunchedEffect(shakeTrigger) {
            if (shakeTrigger == 0) return@LaunchedEffect
            shakeOffset.animateTo(
                targetValue = 0f,
                animationSpec =
                    keyframes {
                        durationMillis = 320
                        (-12f) at 55 using FastOutSlowInEasing
                        12f at 130 using FastOutSlowInEasing
                        (-7f) at 205 using FastOutSlowInEasing
                        4f at 270 using FastOutSlowInEasing
                        0f at 320
                    },
            )
        }

        fun confirm() {
            if (trimmedName.isEmpty()) {
                submittedEmpty = true
                shakeTrigger++
                return
            }
            if (duplicate) {
                shakeTrigger++
                return
            }
            keyboardController?.hide()
            viewModel.createPlaylist(trimmedName, selectedIds.toList())
            path.popTop()
        }

        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
                    .clickHighlight {},
        ) {
            val headerEntrance = rememberEntrance(order = 0)
            val inputEntrance = rememberEntrance(order = 1)
            val suggestionEntrance = rememberEntrance(order = 2)
            val pickerEntrance = rememberEntrance(order = 3)

            // 顶部只有返回键：输入框就是页面标题，不再需要第二处标题
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = Spacing.Small, vertical = Spacing.ExtraSmall)
                        .entranceGraphics(headerEntrance),
            ) {
                IconButton(
                    onClick = {
                        keyboardController?.hide()
                        path.popTop()
                    },
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.back),
                        tint = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }

            NameHeadlineField(
                name = name,
                onNameChange = { newValue ->
                    if (newValue.length <= MAX_NAME_LENGTH) {
                        name = newValue
                    }
                    if (newValue.isNotBlank()) submittedEmpty = false
                },
                onClear = {
                    name = ""
                    focusRequester.requestFocus()
                },
                errorText = errorText,
                onImeDone = { keyboardController?.hide() },
                focusRequester = focusRequester,
                modifier =
                    Modifier
                        .padding(horizontal = LayoutTokens.MusicHeaderHorizontalPadding)
                        .offset { IntOffset(shakeOffset.value.roundToInt(), 0) }
                        .entranceGraphics(inputEntrance),
            )

            Spacer(Modifier.height(Spacing.Medium))

            SuggestionSection(
                currentName = trimmedName,
                takenNames = remember(playlists) { playlists.map { it.playlistName } },
                onPick = {
                    name = it
                    submittedEmpty = false
                },
                modifier =
                    Modifier
                        .padding(horizontal = LayoutTokens.MusicHeaderHorizontalPadding)
                        .entranceGraphics(suggestionEntrance),
            )

            Spacer(Modifier.height(Spacing.Large))

            // 选歌区吃掉全部剩余空间；键盘弹起时随操作条抬升等量收缩
            SongPickerSection(
                candidates = candidates,
                selectedIds = selectedIds,
                onToggle = { mediaStoreId ->
                    selectedIds =
                        if (mediaStoreId in selectedIds) {
                            selectedIds - mediaStoreId
                        } else {
                            selectedIds + mediaStoreId
                        }
                },
                modifier =
                    Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .entranceGraphics(pickerEntrance),
            )

            CreatorActionBar(
                enabled = canCreate,
                selectedCount = selectedIds.size,
                onCreate = { confirm() },
            )
        }
    }
}

@Composable
private fun NameHeadlineField(
    name: String,
    onNameChange: (String) -> Unit,
    onClear: () -> Unit,
    errorText: String?,
    onImeDone: () -> Unit,
    focusRequester: FocusRequester,
    modifier: Modifier = Modifier,
) {
    val isError = errorText != null
    val accentColor by animateColorAsState(
        targetValue =
            if (isError) {
                MaterialTheme.colorScheme.error
            } else {
                MaterialTheme.colorScheme.primary
            },
        animationSpec = tween(durationMillis = 200, easing = EaseOutEmphasized),
        label = "creator_name_accent",
    )
    val nearLimit = name.length > MAX_NAME_LENGTH * 3 / 4

    Column(
        modifier =
            modifier
                .fillMaxWidth(),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.Small),
        ) {
            BasicTextField(
                value = name,
                onValueChange = onNameChange,
                modifier =
                    Modifier
                        .weight(1f)
                        .focusRequester(focusRequester),
                textStyle =
                    MaterialTheme.typography.headlineMedium.copy(
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold,
                    ),
                cursorBrush = SolidColor(accentColor),
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { onImeDone() }),
                decorationBox = { innerTextField ->
                    Box(contentAlignment = Alignment.CenterStart) {
                        if (name.isEmpty()) {
                            Text(
                                text = stringResource(R.string.playlist_name_placeholder_hint),
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        innerTextField()
                    }
                },
            )
            AnimatedVisibility(
                visible = name.isNotEmpty(),
                enter = fadeIn(tween(durationMillis = 160, easing = EaseOutEmphasized)),
                exit = fadeOut(tween(durationMillis = 120, easing = EaseOutEmphasized)),
            ) {
                Box(
                    modifier =
                        Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .clickHighlight(onClick = onClear),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = stringResource(R.string.clear_input),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
        }

        Spacer(Modifier.height(Spacing.Small))

        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(2.dp)
                    .clip(CircleShape)
                    .background(accentColor.copy(alpha = 0.9f)),
        )

        Row(
            modifier = Modifier.fillMaxWidth().padding(top = Spacing.ExtraSmall),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ErrorHint(
                message = errorText,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = "${name.length}/$MAX_NAME_LENGTH",
                style = MaterialTheme.typography.labelSmall,
                color =
                    if (nearLimit) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    },
            )
        }
    }
}

@Composable
private fun ErrorHint(
    message: String?,
    modifier: Modifier = Modifier,
) {
    AnimatedVisibility(
        visible = message != null,
        modifier = modifier,
        enter =
            expandVertically(tween(durationMillis = 200, easing = EaseOutEmphasized)) +
                fadeIn(tween(durationMillis = 160, easing = EaseOutEmphasized)),
        exit =
            shrinkVertically(tween(durationMillis = 160, easing = EaseOutEmphasized)) +
                fadeOut(tween(durationMillis = 110, easing = EaseOutEmphasized)),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.ExtraSmall),
        ) {
            Icon(
                imageVector = Icons.Default.ErrorOutline,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(16.dp),
            )
            var lastMessage by remember { mutableStateOf("") }
            if (message != null) lastMessage = message
            Text(
                // AnimatedVisibility 退场帧会用 null 重组，保留最后一份文案避免闪空
                text = lastMessage,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/** 推荐名称：横向 FlowRow 胶囊，已存在的名字置灰不可选。 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SuggestionSection(
    currentName: String,
    takenNames: List<String>,
    onPick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Spacing.Small),
        verticalArrangement = Arrangement.spacedBy(Spacing.Small),
    ) {
        NAME_SUGGESTION_RES.forEach { res ->
            val suggestion = stringResource(res)
            SuggestionPill(
                label = suggestion,
                selected = suggestion.equals(currentName, ignoreCase = true),
                taken = takenNames.any { it.equals(suggestion, ignoreCase = true) },
                onClick = { onPick(suggestion) },
            )
        }
    }
}

/** 建议胶囊：与 ScanRulesScene 的 ScanRuleChip 同一套观感；已存在的名字置灰不可点。 */
@Composable
private fun SuggestionPill(
    label: String,
    selected: Boolean,
    taken: Boolean,
    onClick: () -> Unit,
) {
    val containerColor by animateColorAsState(
        targetValue =
            when {
                taken -> MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.4f)
                selected -> MaterialTheme.colorScheme.primary
                else -> MaterialTheme.colorScheme.surfaceContainerHigh
            },
        animationSpec = tween(durationMillis = 200, easing = EaseOutEmphasized),
        label = "suggestion_pill_container",
    )
    val contentColor by animateColorAsState(
        targetValue =
            when {
                taken -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
                selected -> MaterialTheme.colorScheme.onPrimary
                else -> MaterialTheme.colorScheme.onSurfaceVariant
            },
        animationSpec = tween(durationMillis = 200, easing = EaseOutEmphasized),
        label = "suggestion_pill_content",
    )
    Box(
        modifier =
            Modifier
                .clip(CircleShape)
                .background(containerColor)
                .then(
                    if (taken) Modifier else Modifier.clickHighlight(onClick = onClick),
                ).padding(horizontal = Spacing.Large, vertical = Spacing.Small),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = contentColor,
            maxLines = 1,
        )
    }
}

/**
 * 选歌区
 */
@Composable
private fun SongPickerSection(
    candidates: List<Song>,
    selectedIds: Set<Long>,
    onToggle: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = LayoutTokens.MusicHeaderHorizontalPadding),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.Small),
        ) {
            Text(
                text = stringResource(R.string.playlist_creator_add_songs_label),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = stringResource(R.string.playlist_creator_add_songs_optional),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
            )
        }

        Spacer(Modifier.height(Spacing.Small))

        if (candidates.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = stringResource(R.string.playlist_creator_no_songs),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding =
                    PaddingValues(
                        start = Spacing.Medium,
                        end = Spacing.Medium,
                        bottom = Spacing.Small,
                    ),
            ) {
                items(
                    items = candidates,
                    key = { it.mediaStoreId },
                ) { song ->
                    CandidateSongRow(
                        song = song,
                        selected = song.mediaStoreId in selectedIds,
                        onToggle = { onToggle(song.mediaStoreId) },
                    )
                }
            }
        }
    }
}

@Composable
private fun CandidateSongRow(
    song: Song,
    selected: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val rowBackground by animateColorAsState(
        targetValue =
            if (selected) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
            } else {
                Color.Transparent
            },
        animationSpec = tween(durationMillis = 200, easing = EaseOutEmphasized),
        label = "candidate_row_background",
    )
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(vertical = 2.dp)
                .clip(Shapes.MediumCornerBasedShape)
                .background(rowBackground)
                .clickHighlight(onClick = onToggle)
                .padding(horizontal = Spacing.Small, vertical = Spacing.Small),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.Medium),
    ) {
        AudioCover(
            uri = song.getCoverUri(),
            fallbackUri = song.getAlbumCoverUri(),
            modifier =
                Modifier
                    .size(44.dp)
                    .clip(Shapes.MediumCornerBasedShape),
            placeHolder = {
                Box(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.MusicNote,
                        contentDescription = stringResource(R.string.cover_placeholder),
                        modifier = Modifier.size(22.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            },
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = song.displayName,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = song.artist,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        SelectIndicator(selected = selected)
    }
}

@Composable
private fun SelectIndicator(selected: Boolean) {
    Box(
        modifier = Modifier.size(22.dp),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .border(
                        width = 1.5.dp,
                        color = MaterialTheme.colorScheme.outlineVariant,
                        shape = CircleShape,
                    ),
        )
        AnimatedVisibility(
            visible = selected,
            enter =
                fadeIn(tween(durationMillis = 220, easing = EaseOutEmphasized)) +
                    scaleIn(
                        animationSpec = tween(durationMillis = 220, easing = EaseOutEmphasized),
                        initialScale = ScaleEnterFrom,
                    ),
            exit = fadeOut(tween(durationMillis = 120, easing = EaseOutEmphasized)),
        ) {
            Box(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(14.dp),
                )
            }
        }
    }
}

/**
 * 底部主操作条
 */
@Composable
private fun CreatorActionBar(
    enabled: Boolean,
    selectedCount: Int,
    onCreate: () -> Unit,
) {
    val containerColor by animateColorAsState(
        targetValue =
            if (enabled) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.surfaceContainerHigh
            },
        animationSpec = tween(durationMillis = 220, easing = EaseOutEmphasized),
        label = "creator_action_container",
    )
    val contentColor by animateColorAsState(
        targetValue =
            if (enabled) {
                MaterialTheme.colorScheme.onPrimary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            },
        animationSpec = tween(durationMillis = 220, easing = EaseOutEmphasized),
        label = "creator_action_content",
    )

    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .imePadding()
                .navigationBarsPadding()
                .padding(
                    horizontal = LayoutTokens.MusicHeaderHorizontalPadding,
                    vertical = Spacing.Medium,
                ),
    ) {
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .clip(CircleShape)
                    .background(containerColor)
                    .clickHighlight(enabled = enabled, onClick = onCreate),
            contentAlignment = Alignment.Center,
        ) {
            // 文案随勾选数切换：淡入淡出即可，固定高度按钮里不做尺寸动画
            AnimatedContent(
                targetState = selectedCount,
                transitionSpec = {
                    ContentTransform(
                        targetContentEnter =
                            fadeIn(tween(durationMillis = 180, easing = EaseOutEmphasized)),
                        initialContentExit =
                            fadeOut(tween(durationMillis = 120, easing = EaseOutEmphasized)),
                        sizeTransform = null,
                    )
                },
                label = "creator_action_label",
            ) { count ->
                Text(
                    text =
                        if (count == 0) {
                            stringResource(R.string.playlist_creator_create_action)
                        } else {
                            stringResource(R.string.playlist_creator_create_action_with_count, count)
                        },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = contentColor,
                )
            }
        }
    }
}
