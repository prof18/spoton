package com.prof18.spoton

import android.annotation.SuppressLint
import android.content.Context
import android.net.ConnectivityManager
import android.net.wifi.WifiManager
import android.os.Build
import android.util.Log
import com.android.dx.stock.ProxyBuilder
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import org.lsposed.hiddenapibypass.HiddenApiBypass
import java.lang.reflect.Method
import java.util.concurrent.Executor

private const val TAG = "HotspotManager"

internal val isHotspotEnabledFlow = MutableStateFlow(false)

private var hiddenApiBypassInitialized = false

/**
 * Initialize hidden API bypass to allow reflection on blocked APIs.
 * Must be called before any reflection on hidden APIs.
 */
private fun initHiddenApiBypass() {
    if (!hiddenApiBypassInitialized && Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        try {
            HiddenApiBypass.addHiddenApiExemptions("")
            hiddenApiBypassInitialized = true
            Log.v(TAG, "Hidden API bypass initialized successfully")
        } catch (e: Throwable) {
            Log.e(TAG, "Failed to initialize hidden API bypass", e)
        }
    }
}

internal fun toggleTethering(ctx: Context) {
    if (isHotspotEnabled(ctx)) {
        stopTethering(ctx)
    } else {
        startTethering(ctx)
    }
}

internal fun startTethering(ctx: Context) {
    initHiddenApiBypass()
    
    try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // Android 11+ uses TetheringManager
            startTetheringViaTetheringManager(ctx)
        } else {
            // Older versions use ConnectivityManager
            startTetheringViaConnectivityManager(ctx)
        }
    } catch (e: Throwable) {
        Log.e(TAG, "Error while starting tethering", e)
        e.printStackTrace()
    }
}

/**
 * Start tethering via TetheringManager (Android 11+).
 * TetheringManager is the modern API that replaced ConnectivityManager for tethering.
 */
@SuppressLint("PrivateApi")
private fun startTetheringViaTetheringManager(ctx: Context) {
    try {
        // Get TetheringManager via reflection
        val tetheringManager = ctx.getSystemService("tethering")
            ?: throw IllegalStateException("TetheringManager not available")
        
        Log.v(TAG, "TetheringManager obtained: ${tetheringManager.javaClass.name}")
        
        // Get the StartTetheringCallback class/interface
        val callbackClass = Class.forName("android.net.TetheringManager\$StartTetheringCallback")
        
        // Create a proxy for the callback
        // StartTetheringCallback is an INTERFACE, so we use Proxy.newProxyInstance
        val proxy = java.lang.reflect.Proxy.newProxyInstance(
            callbackClass.classLoader,
            arrayOf(callbackClass)
        ) { _, method, args ->
            when (method.name) {
                "onTetheringStarted" -> {
                    Log.v(TAG, "onTetheringStarted (TetheringManager)")
                    isHotspotEnabledFlow.update { true }
                    Messenger.sendMessage(status = true, context = ctx)
                }
                "onTetheringFailed" -> {
                    // Log the error code if available
                    val errorCode = args?.getOrNull(0)
                    Log.e(TAG, "onTetheringFailed (TetheringManager) - error code: $errorCode")
                }
            }
            null
        }
        
        val tetheringTypeWifi = 0 // TETHERING_WIFI = 0
        val executor = Executor { command -> command.run() }
        
        // Find and invoke startTethering method
        // There are two possible signatures:
        // 1. startTethering(int type, Executor, StartTetheringCallback) - Android 16+
        // 2. startTethering(TetheringRequest, Executor, StartTetheringCallback) - Android 11-15
        for (method in tetheringManager.javaClass.methods) {
            if (method.name == "startTethering" && method.parameterCount == 3) {
                val paramTypes = method.parameterTypes
                Log.v(TAG, "Found startTethering with params: ${paramTypes.map { it.name }}")
                
                // Check if first param is int (simpler API)
                if (paramTypes[0] == Int::class.javaPrimitiveType) {
                    Log.v(TAG, "Invoking TetheringManager.startTethering(int, Executor, Callback)")
                    method.invoke(tetheringManager, tetheringTypeWifi, executor, proxy)
                    return
                }
                
                // Check if first param is TetheringRequest (older API)
                if (paramTypes[0].name == "android.net.TetheringManager\$TetheringRequest") {
                    // Build TetheringRequest
                    val tetheringRequestBuilderClass = Class.forName("android.net.TetheringManager\$TetheringRequest\$Builder")
                    val builder = tetheringRequestBuilderClass
                        .getConstructor(Int::class.javaPrimitiveType)
                        .newInstance(tetheringTypeWifi)
                    val buildMethod = tetheringRequestBuilderClass.getMethod("build")
                    val tetheringRequest = buildMethod.invoke(builder)
                    
                    Log.v(TAG, "Invoking TetheringManager.startTethering(TetheringRequest, Executor, Callback)")
                    method.invoke(tetheringManager, tetheringRequest, executor, proxy)
                    return
                }
            }
        }
        
        Log.e(TAG, "startTethering method not found on TetheringManager")
    } catch (e: Throwable) {
        Log.e(TAG, "Error in startTetheringViaTetheringManager, falling back to ConnectivityManager", e)
        e.printStackTrace()
        // Fallback to ConnectivityManager if TetheringManager fails
        startTetheringViaConnectivityManager(ctx)
    }
}

/**
 * Start tethering via ConnectivityManager (older approach, pre-Android 11).
 */
@SuppressLint("PrivateApi")
private fun startTetheringViaConnectivityManager(ctx: Context) {
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
        Log.e(TAG, "Error while starting tethering via ConnectivityManager")
        e.printStackTrace()
    }
}

internal fun stopTethering(ctx: Context) {
    initHiddenApiBypass()
    
    try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // Android 11+ uses TetheringManager
            stopTetheringViaTetheringManager(ctx)
        } else {
            // Older versions use ConnectivityManager
            stopTetheringViaConnectivityManager(ctx)
        }
    } catch (e: Throwable) {
        Log.e(TAG, "Error while stopping tethering", e)
        e.printStackTrace()
    }
}

/**
 * Stop tethering via TetheringManager (Android 11+).
 */
@SuppressLint("PrivateApi")
private fun stopTetheringViaTetheringManager(ctx: Context) {
    try {
        val tetheringManager = ctx.getSystemService("tethering")
            ?: throw IllegalStateException("TetheringManager not available")
        
        val tetheringTypeWifi = 0 // TETHERING_WIFI = 0
        
        for (method in tetheringManager.javaClass.methods) {
            if (method.name == "stopTethering" && method.parameterCount == 1) {
                Log.v(TAG, "Invoking TetheringManager.stopTethering")
                method.invoke(tetheringManager, tetheringTypeWifi)
                isHotspotEnabledFlow.update { false }
                Messenger.sendMessage(status = false, context = ctx)
                return
            }
        }
        
        Log.e(TAG, "stopTethering method not found on TetheringManager")
    } catch (e: Throwable) {
        Log.e(TAG, "Error in stopTetheringViaTetheringManager, falling back to ConnectivityManager", e)
        e.printStackTrace()
        // Fallback to ConnectivityManager if TetheringManager fails
        stopTetheringViaConnectivityManager(ctx)
    }
}

/**
 * Stop tethering via ConnectivityManager (older approach, pre-Android 11).
 */
private fun stopTetheringViaConnectivityManager(ctx: Context) {
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
        Log.e(TAG, "Error while stopping tethering via ConnectivityManager")
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
