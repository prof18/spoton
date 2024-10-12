package com.prof18.spoton

import android.util.Log
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.WearableListenerService
import java.nio.charset.Charset

class WearListeningService : WearableListenerService() {
    override fun onMessageReceived(messageEvent: MessageEvent) {
        if (messageEvent.path == HOTSPOT_FROM_WATCH_MESSAGE_PATH) {
            val message = messageEvent.data.toString(Charset.defaultCharset())
            Log.d(WearListeningService::class.simpleName, "onMessageReceived: $message")

            when (message) {
                MESSAGE_TOGGLE -> {
                    toggleTethering(this)
                }

                MESSAGE_STATUS -> {
                    Messenger.sendMessage(status = isHotspotEnabled(this), context = this)
                }
            }
        }
    }
}