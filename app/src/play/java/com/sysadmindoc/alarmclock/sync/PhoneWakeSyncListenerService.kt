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
import org.json.JSONObject
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
            val hour = json.optInt("hour", 7).coerceIn(0, 23)
            val minute = json.optInt("minute", 0).coerceIn(0, 59)
            val label = json.optString("label").take(120)
            alarmRepository.save(
                Alarm(
                    hour = hour,
                    minute = minute,
                    label = label,
                    isEnabled = true
                )
            )
            coordinator.start()
        }
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }
}
