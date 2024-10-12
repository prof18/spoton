package com.prof18.spotonwear

import android.content.Context
import android.util.Log
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class Messenger {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    fun toggleHotspot(context: Context) {
        sendMessage(context, message = MESSAGE_TOGGLE)
    }

    fun sendStatusMessage(context: Context) {
        sendMessage(context, message = MESSAGE_STATUS)
    }

    private fun sendMessage(context: Context, message: String) {
        scope.launch {
            Tasks.await(Wearable.getNodeClient(context).connectedNodes)
                .map { it.id }
                .firstOrNull()
                ?.let { node ->
                    val sendTask: Task<*> = Wearable.getMessageClient(context).sendMessage(
                        node,
                        HOTSPOT_FROM_WATCH_MESSAGE_PATH,
                        message.toByteArray(),
                    ).apply {
                        addOnSuccessListener {
                            Log.d(Messenger::class.simpleName, "message sent: $it")
                        }
                        addOnFailureListener {
                            Log.d(Messenger::class.simpleName, "message failed: $it")
                        }
                    }
                    sendTask.await()
                }
        }
    }
}
