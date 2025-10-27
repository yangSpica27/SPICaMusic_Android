package me.spica27.spicamusic.ui.main

import androidx.activity.compose.BackHandler
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import me.spica27.spicamusic.route.LocalNavController
import me.spica27.spicamusic.route.Routes
import me.spica27.spicamusic.ui.main.home.HomePage
import me.spica27.spicamusic.ui.setting.SettingPage
import me.spica27.spicamusic.utils.DataStoreUtil
import me.spica27.spicamusic.viewModel.PlayBackViewModel
import me.spica27.spicamusic.wrapper.activityViewModel
import org.koin.compose.koinInject

// / 主页
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun MainScreen(
    modifier: Modifier = Modifier,
    playBackViewModel: PlayBackViewModel = activityViewModel(),
    dataStoreUtil: DataStoreUtil = koinInject<DataStoreUtil>(),
) {
    val homeScrollState = rememberScrollState()

    val pagerState =
        rememberPagerState(
            pageCount = {
                10
            },
        )

    val coroutineScope = rememberCoroutineScope()

    BackHandler(
        pagerState.currentPage != 0,
    ) {
        coroutineScope.launch {
            pagerState.animateScrollToPage(0)
        }
    }

    val agreePrivacy = dataStoreUtil.getAgreePrivacy().collectAsStateWithLifecycle(null).value

    if (agreePrivacy == false) {
        LocalNavController.current.navigate(Routes.AgreePrivacy)
    }

    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surface),
    ) {
        // 主页
        Scaffold { innerPadding ->
            Box(
                modifier =
                    modifier
                        .fillMaxSize()
                        .padding(
                            innerPadding,
                        ),
            ) {
                // 水平滚动的页面
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize(),
                    userScrollEnabled = false,
                    key = { it },
                ) { page ->
                    when (page) {
                        0 ->
                            HomePage(
                                listState = homeScrollState,
                                pagerState = pagerState,
                            )

                        1 -> SettingPage(pagerState = pagerState)
                    }
                }
            }
        }
    }
}
