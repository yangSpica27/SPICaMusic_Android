package me.spica27.spicamusic.ui.agree_privacy

import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.navigation.NavController
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import me.spica27.spicamusic.utils.DataStoreUtil
import me.spica27.spicamusic.utils.ToastUtils
import org.koin.compose.koinInject

@Composable
fun AgreePrivacyScreen(
    navigator: NavController,
    dataStoreUtil: DataStoreUtil = koinInject<DataStoreUtil>(),
) {
    BackHandler(true) {
        ToastUtils.showToast("请同意隐私政策")
    }

    val coroutineScope = rememberCoroutineScope()

    var showPrivacy by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(1500)
        showPrivacy = true
    }

    Scaffold { paddingValues ->
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
        ) {
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .verticalScroll(
                            rememberScrollState(),
                        ),
            ) {
                MyWebView(
                    url = "file:///android_asset/privacy.html",
                    onPageFinished = {
                    },
                )
                Row(
                    modifier =
                        Modifier
                            .background(Color(0xfff4f4f4))
                            .padding(horizontal = 16.dp)
                            .padding(bottom = 33.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    ElevatedButton(
                        modifier = Modifier.weight(1f),
                        onClick = {
                            System.exit(0)
                        },
                        shape = MaterialTheme.shapes.small,
                        colors =
                            ButtonDefaults.elevatedButtonColors().copy(
                                containerColor =
                                    Color(
                                        0xfff5222d,
                                    ),
                                contentColor = Color.White,
                            ),
                    ) {
                        Text("拒绝")
                    }
                    ElevatedButton(
                        modifier = Modifier.weight(1f),
                        onClick = {
                            coroutineScope.launch {
                                dataStoreUtil.setAgreePrivacy(true)
                                navigator.popBackStack()
                            }
                        },
                        shape = MaterialTheme.shapes.small,
                        colors =
                            ButtonDefaults.elevatedButtonColors().copy(
                                containerColor =
                                    Color(
                                        0xff52c41a,
                                    ),
                                contentColor = Color.White,
                            ),
                    ) {
                        Text("同意")
                    }
                }
            }
            AnimatedVisibility(
                visible = !showPrivacy,
                exit =
                    fadeOut(
                        tween(
                            durationMillis = 500,
                        ),
                    ),
            ) {
                Box(
                    modifier =
                        Modifier
                            .background(
                                Color(0xfff4f4f4),
                            ).fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
            }
        }
    }
}

@Composable
fun MyWebView(
    url: String,
    onPageFinished: (() -> Unit)? = null,
) {
    AndroidView(
        factory = { context ->
            WebView(context).apply {
                webViewClient =
                    object : WebViewClient() {
                        override fun onPageFinished(
                            view: WebView?,
                            url: String?,
                        ) {
                            super.onPageFinished(view, url)
                            onPageFinished?.invoke()
                        }
                    }
                post {
                    loadUrl(url)
                }
            }
        },
        update = { webView ->
            webView.post {
                webView.loadUrl(url)
            }
        },
        onRelease = {
            it.destroy()
        },
    )
}
