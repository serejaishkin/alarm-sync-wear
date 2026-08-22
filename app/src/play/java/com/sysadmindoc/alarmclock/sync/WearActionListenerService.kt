package com.sysadmindoc.alarmclock.sync

import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.WearableListenerService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import javax.inject.Inject
import dagger.hilt.android.AndroidEntryPoint
import java.nio.ByteBuffer

@AndroidEntryPoint
class WearActionListenerService : WearableListenerService() {
    @Inject lateinit var coordinator: AlarmSyncCoordinator

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onMessageReceived(event: MessageEvent) {
        val operation = when (event.path) {
            PATH_SNOOZE -> AlarmSyncOperation.SNOOZE
            PATH_DISMISS -> AlarmSyncOperation.DISMISS
            PATH_ENABLE -> AlarmSyncOperation.ENABLE
            PATH_DISABLE -> AlarmSyncOperation.DISABLE
            PATH_SKIP -> AlarmSyncOperation.DISABLE
            else -> return
        }
        val alarmId = decodeAlarmId(event.data) ?: return
        scope.launch {
            coordinator.sendWearAction(alarmId, operation)
        }
    }

    private fun decodeAlarmId(data: ByteArray): Long? = runCatching {
        if (data.size < Long.SIZE_BYTES) return null
        ByteBuffer.wrap(data).long
    }.getOrNull()

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    companion object {
        const val PATH_SNOOZE = "/alarmclockxtreme/action/snooze"
        const val PATH_DISMISS = "/alarmclockxtreme/action/dismiss"
        const val PATH_ENABLE = "/alarmclockxtreme/action/enable"
        const val PATH_DISABLE = "/alarmclockxtreme/action/disable"
        const val PATH_SKIP = "/alarmclockxtreme/action/skip"
    }
}
