package me.spica27.spicamusic.ui.about

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import me.spica27.navkit.scene.StackScene
import me.spica27.spicamusic.R
import me.spica27.spicamusic.ui.theme.Spacing

class PrivacyPolicyScene : StackScene() {
    @Composable
    override fun Content() {
        AboutScaffold(title = stringResource(R.string.privacy_title)) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.ExtraSmall)) {
                    Text(
                        text = stringResource(R.string.privacy_intro),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text =
                            stringResource(
                                R.string.privacy_updated_format,
                                stringResource(R.string.privacy_updated_value),
                            ),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    )
                }
            }

            item {
                PrivacySection(
                    title = stringResource(R.string.privacy_section_data_title),
                    body = stringResource(R.string.privacy_section_data_body),
                )
            }
            item {
                PrivacySection(
                    title = stringResource(R.string.privacy_section_network_title),
                    body = stringResource(R.string.privacy_section_network_body),
                )
            }
            item {
                PrivacySection(
                    title = stringResource(R.string.privacy_section_storage_title),
                    body = stringResource(R.string.privacy_section_storage_body),
                )
            }
            item {
                PrivacySection(
                    title = stringResource(R.string.privacy_section_permissions_title),
                    body = stringResource(R.string.privacy_section_permissions_body),
                )
            }
            item {
                PrivacySection(
                    title = stringResource(R.string.privacy_section_thirdparty_title),
                    body = stringResource(R.string.privacy_section_thirdparty_body),
                )
            }
            item {
                PrivacySection(
                    title = stringResource(R.string.privacy_section_contact_title),
                    body = stringResource(R.string.privacy_section_contact_body),
                )
            }
        }
    }
}

@Composable
private fun PrivacySection(
    title: String,
    body: String,
    modifier: Modifier = Modifier,
) {
    AboutSectionCard(title = title, modifier = modifier) {
        Text(
            text = body,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.Large),
        )
    }
}
