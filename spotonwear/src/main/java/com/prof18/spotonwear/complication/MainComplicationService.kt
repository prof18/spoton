package com.prof18.spotonwear.complication

import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Icon
import androidx.annotation.DrawableRes
import androidx.wear.watchface.complications.data.ComplicationData
import androidx.wear.watchface.complications.data.ComplicationType
import androidx.wear.watchface.complications.data.MonochromaticImage
import androidx.wear.watchface.complications.data.PlainComplicationText
import androidx.wear.watchface.complications.data.ShortTextComplicationData
import androidx.wear.watchface.complications.datasource.ComplicationDataSourceUpdateRequester
import androidx.wear.watchface.complications.datasource.ComplicationRequest
import androidx.wear.watchface.complications.datasource.SuspendingComplicationDataSourceService
import com.prof18.spotonwear.R
import com.prof18.spotonwear.isHotspotEnabledFlow
import com.prof18.spotonwear.presentation.MainActivity

class MainComplicationService : SuspendingComplicationDataSourceService() {

    override fun getPreviewData(type: ComplicationType): ComplicationData? {
        if (type != ComplicationType.SHORT_TEXT) {
            return null
        }
        return createComplicationData("ON", R.drawable.tethering_on)
    }

    override suspend fun onComplicationRequest(request: ComplicationRequest): ComplicationData {
        val isHotspotEnabled = isHotspotEnabledFlow.value
        val text = if (isHotspotEnabled) {
            "ON"
        } else {
            "OFF"
        }
        val iconRes = if (isHotspotEnabled) {
            R.drawable.tethering_on
        } else {
            R.drawable.tethering_off
        }

        return createComplicationData(text, iconRes)
    }

    private fun createComplicationData(
        text: String,
        @DrawableRes iconRes: Int,
    ) = ShortTextComplicationData.Builder(
            text = PlainComplicationText.Builder(text).build(),
            contentDescription = PlainComplicationText.Builder(text).build()
        ).setMonochromaticImage(
            MonochromaticImage.Builder(
                Icon.createWithResource(applicationContext, iconRes)
            ).build()
        )
        .setTapAction(createAppPendingIntent(applicationContext))
        .build()

    companion object {
        fun forceComplicationUpdate(applicationContext: Context) {
            val request = ComplicationDataSourceUpdateRequester.create(
                applicationContext, ComponentName(
                    applicationContext, MainComplicationService::class.java
                )
            )
            request.requestUpdateAll()
        }

        fun createAppPendingIntent(context: Context): PendingIntent {
            val intent = Intent(context, MainActivity::class.java).apply {
                putExtra(MainActivity.HOTSPOT_ACTION , MainActivity.TOGGLE_HOTSPOT)
            }
            val flags = PendingIntent.FLAG_IMMUTABLE
            return PendingIntent.getActivity(context, 0, intent, flags)
        }
    }
}
