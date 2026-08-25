package com.sysadmindoc.alarmclock.wear

import android.content.ComponentName
import androidx.wear.tiles.TileService
import androidx.wear.watchface.complications.datasource.ComplicationDataSourceUpdateRequester
import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.WearableListenerService
import org.json.JSONArray
import org.json.JSONObject

class WearAlarmDataListenerService : WearableListenerService() {
    override fun onMessageReceived(messageEvent: MessageEvent) {
        when (messageEvent.path) {
            WakeSyncPeerController.PATH_REQUEST_WATCH_SNAPSHOT,
            WakeSyncPeerController.PATH_REQUEST_SNAPSHOT -> WakeSyncPeerController.sendWatchSnapshot(applicationContext)
        }
    }

    override fun onDataChanged(dataEvents: DataEventBuffer) {
        var changed = false
        dataEvents.forEach { event ->
            if (event.type != DataEvent.TYPE_CHANGED) return@forEach
            val path = event.dataItem.uri.path.orEmpty()
            val dataMap = runCatching { DataMapItem.fromDataItem(event.dataItem).dataMap }.getOrNull() ?: return@forEach
            when {
                path == WearAlarmData.PATH_NEXT_ALARM -> {
                    WearAlarmStore.save(applicationContext, WearAlarmStore.fromDataMap(dataMap))
                    val rawList = dataMap.getString(KEY_ALARM_LIST).orEmpty()
                    if (rawList.isNotBlank()) {
                        // A snapshot is a recovery hint, not an authoritative delete list.
                        // Deletions must arrive as explicit DELETE mutations; otherwise an
                        // older phone snapshot can resurrect alarms removed on the watch.
                        // Also check tombstones to prevent resurrection of deleted alarms.
                        parseAlarmList(rawList).forEach { incoming ->
                            val current = WearAlarmListStore.load(applicationContext).firstOrNull { it.syncId == incoming.syncId }
                            val tombstone = WearAlarmListStore.tombstones(applicationContext).firstOrNull { it.syncId == incoming.syncId }
                            // Skip if there's a tombstone with higher or equal version
                            if (tombstone != null && compareVersion(incoming.revision, incoming.updatedAt, incoming.source, incoming.originDeviceId, tombstone) <= 0) {
                                return@forEach
                            }
                            if (current == null || compareVersion(incoming, current) > 0) {
                                WearAlarmListStore.upsert(applicationContext, incoming)
                            }
                        }
                    }
                    changed = true
                }
                path.startsWith(WakeSyncPeerController.PATH_ALARM_STATE + "/") -> {
                    val raw = dataMap.getString(WakeSyncPeerController.KEY_MUTATION).orEmpty()
                    if (raw.isBlank()) return@forEach
                    if (applyPersistentMutation(raw)) changed = true
                }
            }
        }
        if (changed) notifyAlarmUi()
    }

    private fun applyPersistentMutation(raw: String): Boolean = runCatching {
        val o = JSONObject(raw)
        val syncId = o.optString("syncId").takeIf { it.isNotBlank() } ?: return false
        val operation = o.optString("operation")
        val revision = o.optLong("revision", 0L)
        val timestamp = o.optLong("timestamp", System.currentTimeMillis())
        val current = WearAlarmListStore.load(applicationContext).firstOrNull { it.syncId == syncId }
        if (operation == "DELETE") {
            if (current != null && compareVersion(revision, timestamp, o.optString("source"), o.optString("originDeviceId"), current) <= 0) return false
            WearAlarmListStore.removeWithTombstone(applicationContext, syncId, revision, timestamp, o.optString("source", "PHONE"), o.optString("originDeviceId"))
            return true
        }
        val incoming = WearAlarmListStore.Entry(
            syncId = syncId, label = o.optString("label"), hour = o.optInt("hour", 7).coerceIn(0, 23), minute = o.optInt("minute", 0).coerceIn(0, 59),
            enabled = o.optBoolean("enabled", true), repeatDays = parseDays(o.optJSONArray("repeatDays")),
            snoozeDurationMinutes = o.optInt("snoozeDurationMinutes", 10).coerceIn(1, 60), vibrationEnabled = o.optBoolean("vibrationEnabled", true),
            volume = o.optInt("volume", 100), revision = revision, updatedAt = timestamp, alarmToken = o.optString("alarmToken"),
            source = o.optString("source", "PHONE"), originDeviceId = o.optString("originDeviceId")
        )
        if (current != null && compareVersion(incoming, current) <= 0) return false
        WearAlarmListStore.upsert(applicationContext, incoming); true
    }.getOrDefault(false)

    private fun parseDays(array: JSONArray?): Set<Int> = buildSet {
        if (array != null) for (i in 0 until array.length()) add(array.optInt(i))
    }
    private fun compareVersion(a: WearAlarmListStore.Entry, b: WearAlarmListStore.Entry): Int = compareVersion(a.revision, a.updatedAt, a.source, a.originDeviceId, b)
    private fun compareVersion(revision: Long, timestamp: Long, source: String, deviceId: String, b: WearAlarmListStore.Entry): Int = when {
        revision != b.revision -> revision.compareTo(b.revision)
        timestamp != b.updatedAt -> timestamp.compareTo(b.updatedAt)
        source != b.source -> sourcePriority(source).compareTo(sourcePriority(b.source))
        else -> deviceId.compareTo(b.originDeviceId)
    }
    private fun compareVersion(revision: Long, timestamp: Long, source: String, deviceId: String, b: WearAlarmListStore.Tombstone): Int = when {
        revision != b.revision -> revision.compareTo(b.revision)
        timestamp != b.timestamp -> timestamp.compareTo(b.timestamp)
        source != b.source -> sourcePriority(source).compareTo(sourcePriority(b.source))
        else -> deviceId.compareTo(b.deviceId)
    }
    private fun sourcePriority(source: String): Int = if (source == "WATCH") 2 else 1

    private fun parseAlarmList(raw: String): List<WearAlarmListStore.Entry> = runCatching {
        val array = JSONArray(raw)
        buildList(array.length()) {
            for (i in 0 until array.length()) {
                val o = array.getJSONObject(i)
                add(WearAlarmListStore.Entry(
                    syncId = o.optString("syncId"), label = o.optString("label"), hour = o.optInt("hour"), minute = o.optInt("minute"),
                    enabled = o.optBoolean("enabled", true), repeatDays = parseDays(o.optJSONArray("repeatDays")),
                    snoozeDurationMinutes = o.optInt("snoozeDurationMinutes", 10), vibrationEnabled = o.optBoolean("vibrationEnabled", true),
                    volume = o.optInt("volume", 100), revision = o.optLong("revision", 0L), updatedAt = o.optLong("timestamp", o.optLong("updatedAt", System.currentTimeMillis())),
                    alarmToken = o.optString("alarmToken"), source = o.optString("source", "PHONE"), originDeviceId = o.optString("originDeviceId")
                ))
            }
        }
    }.getOrDefault(emptyList())

    private fun notifyAlarmUi() {
        TileService.getUpdater(applicationContext).requestUpdate(NextAlarmTileService::class.java)
        ComplicationDataSourceUpdateRequester.create(applicationContext, ComponentName(applicationContext, NextAlarmComplicationDataSourceService::class.java)).requestUpdateAll()
    }
    companion object { const val KEY_ALARM_LIST = "alarm_list"; const val KEY_UPDATED_AT = "updated_at" }
}
