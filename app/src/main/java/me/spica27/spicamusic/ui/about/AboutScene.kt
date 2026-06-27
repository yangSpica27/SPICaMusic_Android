package me.spica27.spicamusic.ui.about

import android.content.ActivityNotFoundException
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import com.skydoves.landscapist.image.LandscapistImage
import me.spica27.navkit.path.LocalNavigationPath
import me.spica27.navkit.scene.StackScene
import me.spica27.spicamusic.BuildConfig
import me.spica27.spicamusic.R
import me.spica27.spicamusic.ui.theme.Shapes
import me.spica27.spicamusic.ui.theme.Spacing

/** 项目 GitHub 主页地址。 */
internal const val PROJECT_HOME_URL = "https://github.com/yangSpica27/SPICaMusic_Android"

class AboutScene : StackScene() {
    @Composable
    override fun Content() {
        val path = LocalNavigationPath.current
        val context = LocalContext.current
        val cannotOpenText = stringResource(R.string.about_cannot_open_link)

        fun openUrl(url: String) {
            try {
                context.startActivity(
                    Intent(Intent.ACTION_VIEW, url.toUri()).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    },
                )
            } catch (_: ActivityNotFoundException) {
                Toast.makeText(context, cannotOpenText, Toast.LENGTH_SHORT).show()
            }
        }

        AboutScaffold(title = stringResource(R.string.about_title)) {
            item {
                AboutHeroCard()
            }

            item {
                AboutSectionCard(title = stringResource(R.string.about_section_intro)) {
                    Text(
                        text = stringResource(R.string.about_intro_body),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = Spacing.Large),
                    )
                }
            }

            item {
                AboutSectionCard(title = stringResource(R.string.about_section_more)) {
                    AboutRow(
                        title = stringResource(R.string.about_open_source),
                        subtitle = stringResource(R.string.about_open_source_subtitle),
                        icon = Icons.Default.Code,
                        onClick = { path.push(OpenSourceLicensesScene()) },
                        trailingContent = { ChevronRightIcon() },
                    )
                    AboutItemDivider()
                    AboutRow(
                        title = stringResource(R.string.about_app_license),
                        subtitle = stringResource(R.string.about_app_license_subtitle),
                        icon = Icons.Default.Description,
                        onClick = { path.push(AppLicenseScene()) },
                        trailingContent = { ChevronRightIcon() },
                    )
                    AboutItemDivider()
                    AboutRow(
                        title = stringResource(R.string.about_privacy),
                        subtitle = stringResource(R.string.about_privacy_subtitle),
                        icon = Icons.Default.PrivacyTip,
                        onClick = { path.push(PrivacyPolicyScene()) },
                        trailingContent = { ChevronRightIcon() },
                    )
                    AboutItemDivider()
                    AboutRow(
                        title = stringResource(R.string.about_project_home),
                        subtitle = stringResource(R.string.about_project_home_subtitle),
                        icon = Icons.AutoMirrored.Filled.OpenInNew,
                        onClick = { openUrl(PROJECT_HOME_URL) },
                        trailingContent = {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.OpenInNew,
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

@Composable
private fun ChevronRightIcon() {
    Icon(
        imageVector = Icons.Default.ChevronRight,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun AboutHeroCard(modifier: Modifier = Modifier) {
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
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Spacing.Medium),
        ) {
            LandscapistImage(
                imageModel = { R.mipmap.ic_launcher },
                modifier =
                    Modifier
                        .size(72.dp)
                        .clip(RoundedCornerShape(20.dp)),
            )
            Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            Text(
                text = stringResource(R.string.about_app_tagline),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.78f),
                textAlign = TextAlign.Center,
            )
            Text(
                text = stringResource(R.string.about_version_format, BuildConfig.VERSION_NAME),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier =
                    Modifier
                        .clip(androidx.compose.foundation.shape.CircleShape)
                        .background(MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.10f))
                        .padding(horizontal = Spacing.Medium, vertical = Spacing.Small),
            )
        }
    }
}
