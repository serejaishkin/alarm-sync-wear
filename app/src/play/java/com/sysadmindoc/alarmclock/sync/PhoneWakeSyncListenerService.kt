package com.sysadmindoc.alarmclock.sync

import android.content.Intent
import android.util.Log
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

    override fun onMessageReceived(messageEvent: MessageEvent) {
        when (messageEvent.path) {
            AlarmSyncTransportPaths.ALARM_MUTATION -> {
                val encoded = messageEvent.data.toString(Charsets.UTF_8)
                scope.launch {
                    val decoded = AlarmSyncCodec.decode(encoded).getOrNull()
                    if (decoded == null) {
                        Log.w(TAG, "Ignoring invalid alarm mutation")
                        return@launch
                    }
                    val result = when (decoded.operation) {
                        AlarmSyncOperation.SNOOZE -> runCatching {
                            handleWearAlarmCommand(decoded.syncId, AlarmService.ACTION_SNOOZE)
                        }
                        AlarmSyncOperation.DISMISS -> runCatching {
                            handleWearAlarmCommand(decoded.syncId, AlarmService.ACTION_DISMISS)
                        }
                        else -> coordinator.applyRemote(decoded)
                    }
                    result.onFailure { Log.e(TAG, "Failed to apply ${decoded.operation} for ${decoded.syncId}", it) }
                }
            }
            PATH_CREATE_REQUEST -> scope.launch {
                runCatching { createFromWear(messageEvent.data.toString(Charsets.UTF_8)) }
                    .onFailure { Log.e(TAG, "Failed to create alarm from Wear", it) }
            }
            PATH_UPDATE_REQUEST -> scope.launch {
                runCatching { updateFromWear(messageEvent.data.toString(Charsets.UTF_8)) }
                    .onFailure { Log.e(TAG, "Failed to update alarm from Wear", it) }
            }
            PATH_REQUEST_SNAPSHOT -> scope.launch {
                runCatching { coordinator.syncNow() }
                    .onFailure { Log.e(TAG, "Failed to publish snapshot to Wear", it) }
            }
        }
    }

    private fun handleWearAlarmCommand(syncId: String, action: String) {
        val alarmId = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
            .getLong("alarm_id_$syncId", 0L)
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
        val syncId = json.optString("syncId").takeIf { it.isNotBlank() }
            ?: throw IllegalArgumentException("Wear create request has no syncId")

        // Message delivery may be retried. Never create a second phone alarm
        // for the same logical syncId.
        val existingId = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
            .getLong("alarm_id_$syncId", 0L)
        if (existingId > 0L && alarmRepository.getById(existingId) != null) {
            updateFromWear(json.put("operation", "UPDATE_REQUEST").toString())
            return
        }

        val alarm = Alarm(
            hour = json.optInt("hour", 7).coerceIn(0, 23),
            minute = json.optInt("minute", 0).coerceIn(0, 59),
            label = json.optString("label").take(120),
            isEnabled = json.optBoolean("enabled", true),
            repeatDays = parseRepeatDays(json.optJSONArray("repeatDays")),
            vibrationEnabled = json.optBoolean("vibrationEnabled", true),
            volume = json.optInt("volume", 100).coerceIn(0, 100),
            snoozeDurationMinutes = json.optInt("snoozeDurationMinutes", 10).coerceIn(1, 60)
        )
        val alarmId = alarmRepository.save(alarm)
        coordinator.registerWearCreatedAlarm(
            syncId = syncId,
            alarmId = alarmId,
            revision = json.optLong("revision", 1L),
            timestamp = System.currentTimeMillis(),
            alarmToken = json.optString("alarmToken").ifBlank { null }
        )
        coordinator.start()
        coordinator.syncNow()
    }

    private suspend fun updateFromWear(raw: String) {
        val json = JSONObject(raw)
        require(json.optString("operation") == "UPDATE_REQUEST")
        val syncId = json.getString("syncId")
        coordinator.updateFromWear(
            syncId = syncId,
            hour = json.optInt("hour", 7),
            minute = json.optInt("minute", 0),
            label = json.optString("label"),
            snoozeDurationMinutes = json.optInt("snoozeDurationMinutes", 10),
            vibrationEnabled = json.optBoolean("vibrationEnabled", true),
            volume = json.optInt("volume", 100),
            repeatDays = parseRepeatDays(json.optJSONArray("repeatDays"))
        ).getOrThrow()
        coordinator.syncNow()
    }

    private fun parseRepeatDays(array: JSONArray?): Set<DayOfWeek> {
        if (array == null) return emptySet()
        return buildSet {
            for (i in 0 until array.length()) {
                when (array.optInt(i, 0)) {
                    1 -> add(DayOfWeek.MONDAY)
                    2 -> add(DayOfWeek.TUESDAY)
                    3 -> add(DayOfWeek.WEDNESDAY)
                    4 -> add(DayOfWeek.THURSDAY)
                    5 -> add(DayOfWeek.FRIDAY)
                    6 -> add(DayOfWeek.SATURDAY)
                    7 -> add(DayOfWeek.SUNDAY)
                }
            }
        }
    }

    override fun onDestroy() { scope.cancel(); super.onDestroy() }

    companion object {
        private const val TAG = "WakeSyncPhone"
        private const val PREFS_NAME = "wakesync_state"
        const val PATH_CREATE_REQUEST = "/wakesync/alarm/create_request"
        const val PATH_UPDATE_REQUEST = "/wakesync/alarm/update_request"
        const val PATH_REQUEST_SNAPSHOT = "/wakesync/alarm/request_snapshot"
    }
}
