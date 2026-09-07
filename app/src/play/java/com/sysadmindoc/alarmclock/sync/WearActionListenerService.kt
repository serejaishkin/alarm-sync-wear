package com.sysadmindoc.alarmclock.sync

import com.google.android.gms.wearable.DataMap
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.WearableListenerService
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class WearActionListenerService : WearableListenerService() {
    @Inject lateinit var coordinator: AlarmSyncCoordinator
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onMessageReceived(event: MessageEvent) {
        val operation = when (event.path) {
            PATH_SNOOZE, PATH_WAKESYNC_SNOOZE -> AlarmSyncOperation.SNOOZE
            PATH_DISMISS, PATH_WAKESYNC_DISMISS -> AlarmSyncOperation.DISMISS
            PATH_ENABLE, PATH_WAKESYNC_ENABLE -> AlarmSyncOperation.ENABLE
            PATH_DISABLE, PATH_WAKESYNC_DISABLE -> AlarmSyncOperation.DISABLE
            else -> return
        }
        val alarmId = runCatching {
            DataMap.fromByteArray(event.data).getLong(KEY_ALARM_ID, -1L)
        }.getOrNull()?.takeIf { it > 0L } ?: return
        scope.launch { coordinator.sendWearAction(alarmId, operation) }
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    companion object {
        const val PATH_SNOOZE = "/wakesync/action/snooze"
        const val PATH_DISMISS = "/wakesync/action/dismiss"
        const val PATH_ENABLE = "/wakesync/action/enable"
        const val PATH_DISABLE = "/wakesync/action/disable"
        const val PATH_WAKESYNC_SNOOZE = "/wakesync/action/snooze"
        const val PATH_WAKESYNC_DISMISS = "/wakesync/action/dismiss"
        const val PATH_WAKESYNC_ENABLE = "/wakesync/action/enable"
        const val PATH_WAKESYNC_DISABLE = "/wakesync/action/disable"
        private const val KEY_ALARM_ID = "alarm_id"
    }
}
