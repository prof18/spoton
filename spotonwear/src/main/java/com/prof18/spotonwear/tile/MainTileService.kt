package com.prof18.spotonwear.tile

import android.content.Context
import androidx.wear.protolayout.ActionBuilders
import androidx.wear.protolayout.ColorBuilders.argb
import androidx.wear.protolayout.DimensionBuilders.dp
import androidx.wear.protolayout.LayoutElementBuilders
import androidx.wear.protolayout.LayoutElementBuilders.Image
import androidx.wear.protolayout.ModifiersBuilders
import androidx.wear.protolayout.ResourceBuilders
import androidx.wear.protolayout.TimelineBuilders
import androidx.wear.protolayout.material.Colors
import androidx.wear.protolayout.material.CompactChip
import androidx.wear.protolayout.material.Text
import androidx.wear.protolayout.material.Typography
import androidx.wear.protolayout.material.layouts.PrimaryLayout
import androidx.wear.tiles.RequestBuilders
import androidx.wear.tiles.TileBuilders
import androidx.wear.tiles.tooling.preview.Preview
import androidx.wear.tiles.tooling.preview.TilePreviewData
import androidx.wear.tooling.preview.devices.WearDevices
import com.google.android.horologist.annotations.ExperimentalHorologistApi
import com.google.android.horologist.tiles.SuspendingTileService
import com.prof18.spotonwear.R
import com.prof18.spotonwear.isHotspotEnabledFlow
import com.prof18.spotonwear.presentation.MainActivity

private const val RESOURCES_VERSION = "0"

@OptIn(ExperimentalHorologistApi::class)
class MainTileService : SuspendingTileService() {

    override suspend fun resourcesRequest(
        requestParams: RequestBuilders.ResourcesRequest
    ) = resources(requestParams)

    override suspend fun tileRequest(
        requestParams: RequestBuilders.TileRequest
    ): TileBuilders.Tile {
        val state = isHotspotEnabledFlow.value
        return tile(requestParams, state, this)
    }

    companion object {
        fun forceTileUpdate(applicationContext: Context) {
            getUpdater(applicationContext).requestUpdate(MainTileService::class.java)
        }
    }
}

private fun resources(
    requestParams: RequestBuilders.ResourcesRequest
): ResourceBuilders.Resources {
    return ResourceBuilders.Resources.Builder()
        .setVersion(RESOURCES_VERSION)
        .addIdToImageMapping(
            HOTSPOT_ON_ICON_ID, ResourceBuilders.ImageResource.Builder()
                .setAndroidResourceByResId(
                    ResourceBuilders.AndroidImageResourceByResId.Builder()
                        .setResourceId(R.drawable.tethering_on)
                        .build()
                ).build()
        )
        .addIdToImageMapping(
            HOTSPOT_OFF_ICON_ID, ResourceBuilders.ImageResource.Builder()
                .setAndroidResourceByResId(
                    ResourceBuilders.AndroidImageResourceByResId.Builder()
                        .setResourceId(R.drawable.tethering_off)
                        .build()
                ).build()
        )
        .build()
}

private fun tile(
    requestParams: RequestBuilders.TileRequest,
    isHotspotEnabled: Boolean,
    context: Context,
): TileBuilders.Tile {
    val singleTileTimeline = TimelineBuilders.Timeline.Builder()
        .addTimelineEntry(
            TimelineBuilders.TimelineEntry.Builder()
                .setLayout(
                    LayoutElementBuilders.Layout.Builder()
                        .setRoot(tileLayout(requestParams, isHotspotEnabled, context))
                        .build()
                )
                .build()
        )
        .build()

    return TileBuilders.Tile.Builder()
        .setResourcesVersion(RESOURCES_VERSION)
        .setTileTimeline(singleTileTimeline)
        .build()
}

private fun tileLayout(
    requestParams: RequestBuilders.TileRequest,
    isHotspotEnabled: Boolean,
    context: Context,
): LayoutElementBuilders.LayoutElement {
    return PrimaryLayout.Builder(requestParams.deviceConfiguration)
        .setResponsiveContentInsetEnabled(true)
        .setPrimaryLabelTextContent(
            Text.Builder(
                context,
                if (isHotspotEnabled) {
                    context.getString(R.string.hotspot_on_status_message)
                } else {
                    context.getString(R.string.hotspot_off_status_message)
                },
            )
            .setColor(argb(Colors.DEFAULT.onSurface))
            .setTypography(Typography.TYPOGRAPHY_TITLE3)
            .build()
        )
        .setPrimaryChipContent(
            CompactChip.Builder(
                /* context = */ context,
                /* text = */ if (isHotspotEnabled) {
                    context.getString(R.string.tile_btn_turn_off)
                } else {
                    context.getString(R.string.tile_btn_turn_on)
                },
                /* clickable = */ ModifiersBuilders.Clickable.Builder()
                    .setId("ID_CLICK_HOTSPOT")
                    .setOnClick(
                        ActionBuilders.LaunchAction.Builder()
                            .setAndroidActivity(getAndroidActivity())
                            .build()
                    )
                    .build(),
                /* deviceParameters = */ requestParams.deviceConfiguration
            ).build()
        )
        .setContent(
            Image.Builder()
                .setWidth(dp(48f))
                .setHeight(dp(48f))
                .setResourceId(
                    if (isHotspotEnabled) {
                        HOTSPOT_ON_ICON_ID
                    } else {
                        HOTSPOT_OFF_ICON_ID
                    }
                )
                .setColorFilter(
                    LayoutElementBuilders.ColorFilter.Builder()
                        .setTint(argb(Colors.DEFAULT.primary))
                        .build()
                )
                .build()
        )
        .build()
}

internal fun getAndroidActivity() = ActionBuilders.AndroidActivity.Builder()
    .setPackageName("com.prof18.spoton")
    .setClassName("com.prof18.spotonwear.presentation.MainActivity")
    .addKeyToExtraMapping(
        MainActivity.HOTSPOT_ACTION,
        ActionBuilders.stringExtra(MainActivity.TOGGLE_HOTSPOT)
    )
    .build()

internal const val HOTSPOT_ON_ICON_ID = "tethering_on"
internal const val HOTSPOT_OFF_ICON_ID = "tethering_off"

@Preview(device = WearDevices.SMALL_ROUND)
@Preview(device = WearDevices.LARGE_ROUND)
fun tilePreview(context: Context) = TilePreviewData(::resources) {
    tile(it, isHotspotEnabled = true, context)
}
