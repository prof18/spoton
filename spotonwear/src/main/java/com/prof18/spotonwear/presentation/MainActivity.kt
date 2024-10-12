package com.prof18.spotonwear.presentation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.Icon
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.TimeText
import androidx.wear.tooling.preview.devices.WearDevices
import com.google.android.gms.wearable.MessageClient
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.Wearable
import com.prof18.spotonwear.HOTSPOT_TO_WATCH_MESSAGE_PATH
import com.prof18.spotonwear.Messenger
import com.prof18.spotonwear.R
import com.prof18.spotonwear.presentation.icon.Wifi_tethering
import com.prof18.spotonwear.presentation.icon.Wifi_tethering_off
import com.prof18.spotonwear.presentation.theme.SpotonTheme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import java.nio.charset.Charset

class MainActivity : ComponentActivity(), MessageClient.OnMessageReceivedListener {
    private val messenger by lazy { Messenger() }
    private var isHotspotEnabledFlow = MutableStateFlow(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)

        setTheme(android.R.style.Theme_DeviceDefault)

        setContent {
            val isHotspotEnabled by isHotspotEnabledFlow.collectAsState()
            WearApp(
                isHotspotEnabled = isHotspotEnabled,
                onToggleHotspotClick = {
                    messenger.toggleHotspot(this)
                }
            )
        }
    }

    override fun onResume() {
        super.onResume()
        Wearable.getMessageClient(this).addListener(this)
        messenger.sendStatusMessage(this)
    }

    override fun onPause() {
        super.onPause()
        Wearable.getMessageClient(this).removeListener(this)
    }

    override fun onMessageReceived(messageEvent: MessageEvent) {
        if (messageEvent.path == HOTSPOT_TO_WATCH_MESSAGE_PATH) {
            val message = messageEvent.data.toString(Charset.defaultCharset())
            when (message) {
                "true" -> {
                    isHotspotEnabledFlow.update { true }
                }

                "false" -> {
                    isHotspotEnabledFlow.update { false }
                }
            }
        }
    }
}


@Composable
fun WearApp(isHotspotEnabled: Boolean, onToggleHotspotClick: () -> Unit) {
    SpotonTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colors.background),
            contentAlignment = Alignment.Center
        ) {
            TimeText()

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    modifier = Modifier.fillMaxWidth()
                        .padding(bottom = 16.dp),
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colors.primary,
                    text = if (isHotspotEnabled) {
                        stringResource(R.string.hotspot_on_status_message)
                    } else {
                        stringResource(R.string.hotspot_off_status_message)
                    }
                )

                Button(
                    onClick = onToggleHotspotClick
                ) {
                    Icon(
                        imageVector = if (isHotspotEnabled) {
                            Wifi_tethering_off
                        } else {
                            Wifi_tethering
                        },
                        contentDescription = null
                    )
                }
            }
        }
    }
}

@Preview(device = WearDevices.SMALL_ROUND, showSystemUi = true)
@Composable
private fun HotspotEnabledPreview() {
    WearApp(isHotspotEnabled = true, onToggleHotspotClick = {})
}

@Preview(device = WearDevices.SMALL_ROUND, showSystemUi = true)
@Composable
private fun HotspotDisabledPreview() {
    WearApp(isHotspotEnabled = false, onToggleHotspotClick = {})
}