package com.sysadmindoc.alarmclock.sync

import android.content.Intent
import android.util.Log
import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.WearableListenerService
import com.sysadmindoc.alarmclock.data.model.Alarm
import com.sysadmindoc.alarmclock.data.repository.AlarmRepository
import com.sysadmindoc.alarmclock.domain.AlarmScheduler
import com.sysadmindoc.alarmclock.service.AlarmService
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.time.DayOfWeek
import javax.inject.Inject

@AndroidEntryPoint
class PhoneWakeSyncListenerService : WearableListenerService() {
    @Inject lateinit var coordinator: AlarmSyncCoordinator
    @Inject lateinit var alarmRepository: AlarmRepository

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onDataChanged(dataEvents: DataEventBuffer) {
        Log.i(TAG, "onDataChanged: ${dataEvents.count} event(s)")
        for (event in dataEvents) {
            if (event.type != DataEvent.TYPE_CHANGED) {
                Log.d(TAG, "onDataChanged: skipping non-CHANGED event type=${event.type}")
                continue
            }
            val path = event.dataItem.uri.path.orEmpty()
            val dataMap = runCatching { DataMapItem.fromDataItem(event.dataItem).dataMap }.getOrNull()
            if (dataMap == null) {
                Log.w(TAG, "onDataChanged: null DataMap for path=$path")
                continue
            }
            Log.i(TAG, "onDataChanged: path=$path keys=${dataMap.keySet()}")

            when {
                path.startsWith(PATH_ALARM_STATE_PREFIX) -> {
                    val syncId = path.removePrefix(PATH_ALARM_STATE_PREFIX)
                    val encoded = dataMap.getString(KEY_MUTATION).orEmpty()
                    if (encoded.isBlank()) {
                        Log.w(TAG, "onDataChanged: blank mutation for syncId=$syncId")
                        continue
                    }
                    Log.i(TAG, "onDataChanged: ALARM_STATE syncId=$syncId payloadLen=${encoded.length}")
                    scope.launch {
                        val decoded = AlarmSyncCodec.decode(encoded).getOrNull()
                        if (decoded == null) {
                            Log.w(TAG, "onDataChanged:Ignoring invalid persistent alarm mutation for syncId=$syncId payloadPreview=${encoded.take(100)}")
                            return@launch
                        }
                        Log.i(TAG, "onDataChanged: decoded op=${decoded.operation} syncId=${decoded.syncId} rev=${decoded.revision} enabled=${decoded.enabled}")
                        coordinator.applyRemote(decoded)
                            .onFailure { Log.e(TAG, "onDataChanged: Failed to apply Data Layer ${decoded.operation} for ${decoded.syncId}", it) }
                            .onSuccess { Log.i(TAG, "onDataChanged: Successfully applied ${decoded.operation} for ${decoded.syncId}") }
                    }
                }
                path == PATH_ALARM_SNAPSHOT -> {
                    val rawList = dataMap.getString(KEY_ALARM_LIST).orEmpty()
                    val snapshotTimestamp = dataMap.getLong(KEY_UPDATED_AT, System.currentTimeMillis())
                    Log.i(TAG, "onDataChanged: ALARM_SNAPSHOT payloadLen=${rawList.length} ts=$snapshotTimestamp")
                    if (rawList.isBlank()) continue
                    scope.launch {
                        runCatching { parseSnapshot(rawList) }
                            .onSuccess { entries ->
                                Log.i(TAG, "onDataChanged: parsed ${entries.size} snapshot entries")
                                coordinator.applyWatchSnapshot(entries, snapshotTimestamp)
                                    .onFailure { Log.e(TAG, "onDataChanged:Failed to reconcile Watch snapshot", it) }
                                    .onSuccess { Log.i(TAG, "onDataChanged: Watch snapshot reconciled (${entries.size} entries)") }
                            }
                            .onFailure { Log.e(TAG, "onDataChanged:Failed to parse Watch snapshot", it) }
                    }
                }
                else -> Log.d(TAG, "onDataChanged: unhandled path=$path")
            }
        }
    }

    override fun onMessageReceived(messageEvent: MessageEvent) {
        Log.i(TAG, "onMessageReceived: path=${messageEvent.path} dataLen=${messageEvent.data?.size ?: 0}")
        when (messageEvent.path) {
            AlarmSyncTransportPaths.ALARM_MUTATION -> {
                val encoded = messageEvent.data.toString(Charsets.UTF_8)
                Log.i(TAG, "onMessageReceived: ALARM_MUTATION payloadLen=${encoded.length}")
                scope.launch {
                    val decoded = AlarmSyncCodec.decode(encoded).getOrNull()
                    if (decoded == null) {
                        Log.w(TAG, "onMessageReceived: Failed to decode ALARM_MUTATION payloadPreview=${encoded.take(100)}")
                        return@launch
                    }
                    Log.i(TAG, "onMessageReceived: decoded op=${decoded.operation} syncId=${decoded.syncId} rev=${decoded.revision} enabled=${decoded.enabled} src=${decoded.source}")
                    // Use the coordinator for transient actions too. It
                    // remembers the version before the DataClient duplicate
                    // arrives, so one Wear action cannot execute twice.
                    val result = coordinator.applyRemote(decoded)
                    result.onFailure { Log.e(TAG, "onMessageReceived: Failed to apply ${decoded.operation} for ${decoded.syncId}", it) }
                        .onSuccess { Log.i(TAG, "onMessageReceived: Successfully applied ${decoded.operation} for ${decoded.syncId}") }
                }
            }
            PATH_CREATE_REQUEST -> {
                Log.i(TAG, "onMessageReceived: CREATE_REQUEST")
                scope.launch {
                    runCatching { createFromWear(messageEvent.data.toString(Charsets.UTF_8)) }
                        .onFailure { Log.e(TAG, "onMessageReceived:Failed to create alarm from Wear", it) }
                        .onSuccess { Log.i(TAG, "onMessageReceived: Alarm created from Wear") }
                }
            }
            PATH_UPDATE_REQUEST -> {
                Log.i(TAG, "onMessageReceived: UPDATE_REQUEST")
                scope.launch {
                    runCatching { updateFromWear(messageEvent.data.toString(Charsets.UTF_8)) }
                        .onFailure { Log.e(TAG, "onMessageReceived:Failed to update alarm from Wear", it) }
                        .onSuccess { Log.i(TAG, "onMessageReceived: Alarm updated from Wear") }
                }
            }
            PATH_REQUEST_SNAPSHOT -> {
                Log.i(TAG, "onMessageReceived: REQUEST_SNAPSHOT")
                scope.launch {
                    runCatching { coordinator.syncNow() }
                        .onFailure { Log.e(TAG, "onMessageReceived:Failed to publish snapshot to Wear", it) }
                        .onSuccess { Log.i(TAG, "onMessageReceived: Snapshot published to Wear") }
                }
            }
            PATH_WATCH_SNAPSHOT -> {
                Log.i(TAG, "onMessageReceived: WATCH_SNAPSHOT")
                scope.launch {
                    runCatching { applyWatchSnapshotMessage(messageEvent.data.toString(Charsets.UTF_8)) }
                        .onFailure { Log.e(TAG, "onMessageReceived:Failed to apply Watch snapshot", it) }
                        .onSuccess { Log.i(TAG, "onMessageReceived: Watch snapshot applied") }
                }
            }
            else -> Log.d(TAG, "onMessageReceived: unhandled path=${messageEvent.path}")
        }
    }

    private suspend fun applyWatchSnapshotMessage(raw: String) {
        val entries = parseSnapshot(raw)
        // Snapshot is a recovery hint only; do not publish the local phone list back here.
        coordinator.applyWatchSnapshot(entries, System.currentTimeMillis()).getOrThrow()
    }

    private fun parseSnapshot(raw: String): List<AlarmSyncPayload> {
        val array = JSONArray(raw)
        return buildList(array.length()) {
            for (i in 0 until array.length()) {
                val o = array.getJSONObject(i)
                add(AlarmSyncPayload(
                    protocolVersion = o.optInt("protocolVersion", AlarmSyncEnvelope.CURRENT_PROTOCOL_VERSION),
                    syncId = o.getString("syncId"),
                    operation = runCatching { AlarmSyncOperation.valueOf(o.optString("operation", "UPDATE")) }.getOrDefault(AlarmSyncOperation.UPDATE),
                    source = runCatching { AlarmSyncSource.valueOf(o.optString("source", "WATCH")) }.getOrDefault(AlarmSyncSource.WATCH),
                    revision = o.optLong("revision", 0L),
                    timestamp = o.optLong("timestamp", System.currentTimeMillis()),
                    alarmToken = o.optString("alarmToken").ifBlank { null },
                    originDeviceId = o.optString("originDeviceId", "WATCH"),
                    hour = if (o.has("hour")) o.optInt("hour") else null,
                    minute = if (o.has("minute")) o.optInt("minute") else null,
                    label = if (o.has("label")) o.optString("label") else null,
                    enabled = if (o.has("enabled")) o.optBoolean("enabled") else null,
                    repeatDays = o.optJSONArray("repeatDays")?.let { days -> buildList(days.length()) { for (j in 0 until days.length()) add(days.optInt(j)) } } ?: emptyList(),
                    snoozeDurationMinutes = o.optInt("snoozeDurationMinutes", 10),
                    vibrationEnabled = o.optBoolean("vibrationEnabled", true),
                    volume = o.optInt("volume", 100)
                ))
            }
        }
    }

    private fun handleWearAlarmCommand(syncId: String, action: String) {
        val alarmId = getSharedPreferences(PREFS_NAME, MODE_PRIVATE).getLong("alarm_id_$syncId", 0L)
            .takeIf { it > 0L } ?: error("Unknown synchronized alarm: $syncId")
        startService(Intent(this, AlarmService::class.java).apply {
            this.action = action
            putExtra(AlarmScheduler.EXTRA_ALARM_ID, alarmId)
            putExtra(AlarmScheduler.EXTRA_SCHEDULED_AT, System.currentTimeMillis())
        })
    }

    private suspend fun createFromWear(raw: String) {
        val json = JSONObject(raw)
        require(json.optString("operation") == "CREATE_REQUEST")
        val syncId = json.optString("syncId").takeIf { it.isNotBlank() } ?: throw IllegalArgumentException("Wear create request has no syncId")
        val existingId = getSharedPreferences(PREFS_NAME, MODE_PRIVATE).getLong("alarm_id_$syncId", 0L)
        if (existingId > 0L && alarmRepository.getById(existingId) != null) {
            updateFromWear(json.put("operation", "UPDATE_REQUEST").toString()); return
        }
        val alarm = Alarm(
            hour = json.optInt("hour", 7).coerceIn(0, 23), minute = json.optInt("minute", 0).coerceIn(0, 59),
            label = json.optString("label").take(120), isEnabled = json.optBoolean("enabled", true),
            repeatDays = parseRepeatDays(json.optJSONArray("repeatDays")),
            vibrationEnabled = json.optBoolean("vibrationEnabled", true), volume = json.optInt("volume", 100).coerceIn(0, 100),
            snoozeDurationMinutes = json.optInt("snoozeDurationMinutes", 10).coerceIn(1, 60)
        )
        val alarmId = alarmRepository.save(alarm)
        coordinator.registerWearCreatedAlarm(syncId, alarmId, json.optLong("revision", 1L), json.optLong("timestamp", System.currentTimeMillis()), json.optString("alarmToken").ifBlank { null })
        coordinator.start()
    }

    private suspend fun updateFromWear(raw: String) {
        val json = JSONObject(raw)
        require(json.optString("operation") == "UPDATE_REQUEST")
        coordinator.updateFromWear(
            syncId = json.getString("syncId"), hour = json.optInt("hour", 7), minute = json.optInt("minute", 0),
            label = json.optString("label"), snoozeDurationMinutes = json.optInt("snoozeDurationMinutes", 10),
            vibrationEnabled = json.optBoolean("vibrationEnabled", true), volume = json.optInt("volume", 100),
            repeatDays = parseRepeatDays(json.optJSONArray("repeatDays"))
        ).getOrThrow()
        coordinator.start()
    }

    private fun parseRepeatDays(array: JSONArray?): Set<DayOfWeek> = buildSet {
        if (array == null) return@buildSet
        for (i in 0 until array.length()) when (array.optInt(i, 0)) {
            1 -> add(DayOfWeek.MONDAY); 2 -> add(DayOfWeek.TUESDAY); 3 -> add(DayOfWeek.WEDNESDAY)
            4 -> add(DayOfWeek.THURSDAY); 5 -> add(DayOfWeek.FRIDAY); 6 -> add(DayOfWeek.SATURDAY); 7 -> add(DayOfWeek.SUNDAY)
        }
    }

    override fun onDestroy() { scope.cancel(); super.onDestroy() }

    companion object {
        private const val TAG = "WakeSyncPhone"
        private const val PREFS_NAME = "wakesync_state"
        private const val PATH_ALARM_STATE_PREFIX = "/wakesync/alarm/state/"
        private const val PATH_ALARM_SNAPSHOT = "/alarms/next"
        private const val KEY_MUTATION = "mutation"
        private const val KEY_ALARM_LIST = "alarm_list"
        private const val KEY_UPDATED_AT = "updated_at"
        const val PATH_CREATE_REQUEST = "/wakesync/alarm/create_request"
        const val PATH_UPDATE_REQUEST = "/wakesync/alarm/update_request"
        const val PATH_REQUEST_SNAPSHOT = "/wakesync/alarm/request_snapshot"
        const val PATH_WATCH_SNAPSHOT = "/wakesync/alarm/watch_snapshot"
    }
}
