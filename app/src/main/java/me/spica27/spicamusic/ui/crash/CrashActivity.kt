package me.spica27.spicamusic.ui.crash

import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.OptIn
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ListItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.util.UnstableApi
import kotlinx.coroutines.launch
import me.spica27.spicamusic.MainActivity
import me.spica27.spicamusic.R
import me.spica27.spicamusic.crash.CrashHandler
import me.spica27.spicamusic.db.entity.Crash
import me.spica27.spicamusic.widget.SimpleTopBar
import kotlin.system.exitProcess

class CrashActivity : ComponentActivity() {
    companion object {
        const val ARG_CLASH_MODEL = "CRASH_MODEL"
    }

    @OptIn(UnstableApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val crash =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                intent.getParcelableExtra(ARG_CLASH_MODEL, Crash::class.java)
            } else {
                intent.getParcelableExtra(ARG_CLASH_MODEL)
            }
        if (crash == null) {
            finish()
            return
        }
        setContent {
            val scrollState = rememberScrollState()
            Scaffold(
                topBar = {
                    SimpleTopBar(
                        title = stringResource(R.string.crash_log),
                        onBack = {
                            finish()
                        },
                    )
                },
            ) { paddingValues ->
                Box(modifier = Modifier.padding(paddingValues)) {
                    Column(
                        modifier =
                            Modifier
                                .fillMaxSize()
                                .verticalScroll(
                                    scrollState,
                                ).padding(horizontal = 16.dp, vertical = 8.dp),
                    ) {
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            TextButton(
                                onClick = {
                                    lifecycleScope.launch {
                                        CrashHandler.shareLog(this@CrashActivity, crash)
                                    }
                                },
                            ) {
                                Text("分享日志")
                            }
                            TextButton(
                                onClick = {
                                    val intent = Intent(this@CrashActivity, MainActivity::class.java)
                                    val restartIntent = Intent.makeRestartActivityTask(intent.component)
                                    startActivity(restartIntent)
                                    exitProcess(0)
                                },
                            ) {
                                Text("重启应用")
                            }
                        }
                        ListItem(
                            headlineContent = {
                                Text(crash.title)
                            },
                        )
                        ListItem(
                            headlineContent = {
                                Text("Crash Message")
                            },
                            supportingContent = {
                                Text(crash.message)
                            },
                        )
                        ListItem(
                            headlineContent = {
                                Text("Cause Class Name")
                            },
                            supportingContent = {
                                Text(crash.causeClass)
                            },
                        )
                        ListItem(
                            headlineContent = {
                                Text("Crash File")
                            },
                            supportingContent = {
                                Text(crash.causeFile)
                            },
                        )
                        ListItem(
                            headlineContent = {
                                Text("Crash Line")
                            },
                            supportingContent = {
                                Text(crash.causeLine)
                            },
                        )
                        ListItem(
                            headlineContent = {
                                Text("Android Version")
                            },
                            supportingContent = {
                                Text(crash.buildVersion)
                            },
                        )
                        ListItem(
                            headlineContent = {
                                Text("Device Info")
                            },
                            supportingContent = {
                                Text(crash.deviceInfo)
                            },
                        )
                        ListItem(
                            headlineContent = {
                                Text("Stack Trace")
                            },
                            supportingContent = {
                                Text(crash.stackTrace)
                            },
                        )
                    }
                }
            }
        }
    }
}
