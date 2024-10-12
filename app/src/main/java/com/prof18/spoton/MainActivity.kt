package com.prof18.spoton

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.prof18.spoton.ui.icons.Wifi_tethering
import com.prof18.spoton.ui.icons.Wifi_tethering_off
import com.prof18.spoton.ui.theme.SpotonTheme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

class MainActivity : ComponentActivity() {

    private val canWritePermission = MutableStateFlow(false)

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val isSystemWritePermissionGranted by canWritePermission.collectAsState()
            val isHotspotEnabled by isHotspotEnabledFlow.collectAsState()

            val openAlertDialog = remember { mutableStateOf(false) }
            val context = LocalContext.current

            SpotonTheme {
                when {
                    openAlertDialog.value -> {
                        AlertDialog(
                            text = {
                                Column {
                                    Text(text = stringResource(R.string.about_app).trimIndent())

                                    AnnotatedClickableText(
                                        modifier = Modifier
                                            .padding(top = 32.dp)
                                            .fillMaxWidth(),
                                        onTextClick = {
                                            val url = "https://www.marcogomiero.com"
                                            val intent = Intent(Intent.ACTION_VIEW)
                                            intent.data = Uri.parse(url)
                                            startActivity(intent)
                                        }
                                    )
                                }
                            },
                            onDismissRequest = { openAlertDialog.value = false },
                            confirmButton = {
                                TextButton(
                                    onClick = {
                                        openAlertDialog.value = false
                                    }
                                ) {
                                    Text(stringResource(R.string.about_ok_button))
                                }
                            },
                        )
                    }
                }

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    topBar = {
                        CenterAlignedTopAppBar(
                            title = {
                                Text(
                                    text = "spoton",
                                    style = MaterialTheme.typography.displayMedium,
                                )
                            },
                            actions = {
                                IconButton(
                                    onClick = {
                                        openAlertDialog.value = true
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Info,
                                        contentDescription = null
                                    )
                                }
                            }
                        )
                    }
                ) { innerPadding ->
                    Column(
                        modifier = Modifier
                            .padding(innerPadding)
                            .padding(top = 36.dp)
                            .padding(horizontal = 16.dp)
                    ) {
                        if (isSystemWritePermissionGranted) {
                            Text(stringResource(R.string.permission_to_write_settings_granted))

                            Text(
                                modifier = Modifier
                                    .padding(top = 16.dp),
                                text = stringResource(R.string.after_permission_enabled_message),
                            )

                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.BottomCenter
                            ) {
                                Row(
                                    modifier = Modifier
                                        .padding(bottom = 48.dp)
                                        .fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Image(
                                        modifier = Modifier.size(48.dp),
                                        imageVector = if (isHotspotEnabled) Wifi_tethering else Wifi_tethering_off,
                                        contentDescription = null,
                                        colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onSurface)
                                    )

                                    Text(
                                        modifier = Modifier
                                            .padding(horizontal = 16.dp),
                                        text = if (isHotspotEnabled) {
                                            stringResource(R.string.hotspot_on_status_message)
                                        } else {
                                            stringResource(R.string.hotspot_off_status_message)
                                        },
                                        style = MaterialTheme.typography.titleLarge,
                                    )

                                    Switch(
                                        checked = isHotspotEnabled,
                                        onCheckedChange = {
                                            if (isHotspotEnabled) {
                                                stopTethering(context)
                                            } else {
                                                startTethering(context)
                                            }
                                        }
                                    )
                                }
                            }
                        } else {
                            Text(stringResource(R.string.permissions_needed_message))

                            Button(
                                modifier = Modifier
                                    .padding(top = 16.dp),
                                onClick = {
                                    openAndroidPermissionsMenu()
                                }
                            ) {
                                Text(stringResource(R.string.grant_permission_button))
                            }
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun AnnotatedClickableText(
        modifier: Modifier = Modifier,
        onTextClick: () -> Unit,
    ) {
        val annotatedText = buildAnnotatedString {
            append(stringResource(id = R.string.author_label))

            pushStringAnnotation(
                tag = "URL",
                annotation = "https://www.marcogomiero.com"
            )
            withStyle(
                style = SpanStyle(
                    color = Color.Blue,
                    fontWeight = FontWeight.Bold
                )
            ) {
                append(" Marco Gomiero")
            }

            pop()
        }

        ClickableText(
            modifier = modifier,
            text = annotatedText,
            style = MaterialTheme.typography.bodyMedium.copy(
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurface,
            ),
            onClick = { offset ->
                annotatedText.getStringAnnotations(
                    tag = "URL",
                    start = offset,
                    end = offset,
                ).firstOrNull()?.let {
                    onTextClick()
                }
            }
        )
    }

    private fun checkSystemWritePermission(): Boolean {
        val canWriteSettings = Settings.System.canWrite(applicationContext)
        return canWriteSettings
    }

    private fun openAndroidPermissionsMenu() {
        val intent = Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS)
        intent.setData(Uri.parse("package:" + this.packageName))
        startActivity(intent)
    }

    override fun onResume() {
        super.onResume()
        canWritePermission.update { checkSystemWritePermission() }
        isHotspotEnabledFlow.update { isHotspotEnabled(this) }
    }
}


