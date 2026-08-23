package com.sysadmindoc.alarmclock.wear

import android.content.ComponentName
import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.WearableListenerService
import androidx.wear.tiles.TileService
import androidx.wear.watchface.complications.datasource.ComplicationDataSourceUpdateRequester
import org.json.JSONArray

class WearAlarmDataListenerService : WearableListenerService() {

    override fun onDataChanged(dataEvents: DataEventBuffer) {
        var changed = false
        dataEvents.forEach { event ->
            val item = event.dataItem
            if (item.uri.path != WearAlarmData.PATH_NEXT_ALARM || event.type != DataEvent.TYPE_CHANGED) return@forEach
            val dataMap = DataMapItem.fromDataItem(item).dataMap
            val snapshot = WearAlarmStore.fromDataMap(dataMap)
            WearAlarmStore.save(applicationContext, snapshot)

            val rawList = dataMap.getString(KEY_ALARM_LIST).orEmpty()
            if (rawList.isNotBlank()) {
                val entries = parseAlarmList(rawList)
                WearAlarmListStore.save(applicationContext, entries)
            }
            changed = true
        }
        if (changed) {
            TileService.getUpdater(applicationContext).requestUpdate(NextAlarmTileService::class.java)
            ComplicationDataSourceUpdateRequester.create(
                context = applicationContext,
                complicationDataSourceComponent = ComponentName(
                    applicationContext,
                    NextAlarmComplicationDataSourceService::class.java
                )
            ).requestUpdateAll()
        }
    }

    private fun parseAlarmList(raw: String): List<WearAlarmListStore.Entry> = runCatching {
        val array = JSONArray(raw)
        buildList(array.length()) {
            for (i in 0 until array.length()) {
                val o = array.getJSONObject(i)
                val days = buildSet {
                    val a = o.optJSONArray("repeatDays")
                    if (a != null) for (j in 0 until a.length()) add(a.optInt(j))
                }
                add(
                    WearAlarmListStore.Entry(
                        syncId = o.optString("syncId"),
                        label = o.optString("label"),
                        hour = o.optInt("hour"),
                        minute = o.optInt("minute"),
                        enabled = o.optBoolean("enabled", true),
                        repeatDays = days,
                        snoozeDurationMinutes = o.optInt("snoozeDurationMinutes", 10),
                        vibrationEnabled = o.optBoolean("vibrationEnabled", true),
                        volume = o.optInt("volume", 100),
                        revision = o.optLong("revision", 0L),
                        updatedAt = o.optLong("updatedAt", System.currentTimeMillis()),
                        alarmToken = o.optString("alarmToken")
                    )
                )
            }
        }
    }.getOrDefault(emptyList())

    companion object {
        const val KEY_ALARM_LIST = "alarm_list"
    }
}
