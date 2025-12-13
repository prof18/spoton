package com.prof18.spoton

import android.annotation.SuppressLint
import android.content.Context
import android.net.ConnectivityManager
import android.net.wifi.WifiManager
import android.util.Log
import com.android.dx.stock.ProxyBuilder
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import java.lang.reflect.Method

private const val TAG = "HotspotManager"

internal val isHotspotEnabledFlow = MutableStateFlow(false)

internal fun toggleTethering(ctx: Context) {
    if (isHotspotEnabled(ctx)) {
        stopTethering(ctx)
    } else {
        startTethering(ctx)
    }
}

internal fun startTethering(ctx: Context) {
    try {
        val outputDir = ctx.codeCacheDir
        val proxy = ProxyBuilder.forClass(getOnStartTetheringCallbackClass())
            .dexCache(outputDir).handler { proxy, method, args ->
                {
                    when (method.name) {
                        "onTetheringStarted" -> {
                            Log.v(TAG, "onTetheringStarted")
                        }

                        "onTetheringFailed" -> {
                            Log.v(TAG, "onTetheringFailed")
                        }

                        else -> {
                            ProxyBuilder.callSuper(proxy, method, args)
                        }
                    }
                }
            }.build()
        val connMng = ctx.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        for (method in connMng.javaClass.methods) {
            if (method.name.equals("startTethering") && method.parameterCount == 3) {
                Log.v(TAG, "Invoking startTethering method.")
                method.invoke(connMng, 0, false, proxy)
                isHotspotEnabledFlow.update { true }
                Messenger.sendMessage(status = true, context = ctx)
            }
        }
    } catch (e: Throwable) {
        Log.e(TAG, "Error while starting tethering")
        e.printStackTrace()
    }
}

internal fun stopTethering(ctx: Context) {
    try {
        val connMng = ctx.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        for (method in connMng.javaClass.methods) {
            if (method.name.equals("stopTethering")) {
                Log.v(TAG, "Invoking stopTethering method.")
                method.invoke(connMng, 0)
                isHotspotEnabledFlow.update { false }
                Messenger.sendMessage(status = false, context = ctx)
            }
        }
    } catch (e: Throwable) {
        Log.e(TAG, "Error while stopping tethering")
        e.printStackTrace()
    }
}

internal fun isHotspotEnabled(ctx: Context): Boolean {
    val wifiManager: WifiManager = ctx.getSystemService(Context.WIFI_SERVICE) as WifiManager
    return try {
        val method: Method = wifiManager.javaClass.getMethod("isWifiApEnabled")
        method.invoke(wifiManager) as Boolean
    } catch (e: Throwable) {
        Log.e(TAG, "Error while getting hotspot status")
        e.printStackTrace()
        false
    }
}

@SuppressLint("PrivateApi")
private fun getOnStartTetheringCallbackClass(): Class<*>? {
    return try {
        Class.forName("android.net.ConnectivityManager\$OnStartTetheringCallback")
    } catch (e: Throwable) {
        Log.e(TAG, "Unable to find OnStartTetheringCallbackClass")
        e.printStackTrace()
        null
    }
}
