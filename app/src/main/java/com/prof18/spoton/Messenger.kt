package com.prof18.spoton

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

object Messenger {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    fun sendMessage(context: Context, status: Boolean) {
        scope.launch {
            try {
                val nodes =
                    Tasks.await(Wearable.getNodeClient(context).connectedNodes).map { it.id }
                val sendTask: Task<*> = Wearable.getMessageClient(context).sendMessage(
                    nodes.first(),
                    HOTSPOT_TO_WATCH_MESSAGE_PATH,
                    status.toString().toByteArray(),
                ).apply {
                    addOnSuccessListener {
                        Log.d(Messenger::class.simpleName, "message sent: $it")
                    }
                    addOnFailureListener {
                        Log.d(Messenger::class.simpleName, "message failed: $it")
                    }
                }
                sendTask.await()
            } catch (_: Exception) {
                // WearOS connection not present, skip it
            }
        }
    }
}
