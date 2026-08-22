package com.sysadmindoc.alarmclock.sync

import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.WearableListenerService
import com.sysadmindoc.alarmclock.data.model.Alarm
import com.sysadmindoc.alarmclock.data.repository.AlarmRepository
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

/** Receives mutations initiated by the Wear peer. */
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
                    AlarmSyncCodec.decode(encoded).onSuccess { coordinator.applyRemote(it) }
                }
            }
            "/wakesync/alarm/create_request" -> {
                val raw = messageEvent.data.toString(Charsets.UTF_8)
                scope.launch { createFromWear(raw) }
            }
        }
    }

    private suspend fun createFromWear(raw: String) {
        runCatching {
            val json = JSONObject(raw)
            require(json.optString("operation") == "CREATE_REQUEST")
            val repeatDays = parseRepeatDays(json.optJSONArray("repeatDays"))
            val alarm = Alarm(
                hour = json.optInt("hour", 7).coerceIn(0, 23),
                minute = json.optInt("minute", 0).coerceIn(0, 59),
                label = json.optString("label").take(120),
                isEnabled = true,
                repeatDays = repeatDays,
                vibrationEnabled = json.optBoolean("vibrationEnabled", true),
                volume = json.optInt("volume", 100).coerceIn(0, 100),
                snoozeDurationMinutes = json.optInt("snoozeDurationMinutes", 10).coerceIn(1, 60)
            )
            alarmRepository.save(alarm)
            coordinator.start()
        }
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

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }
}
