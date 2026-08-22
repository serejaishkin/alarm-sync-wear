package com.sysadmindoc.alarmclock.sync

import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.WearableListenerService
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Receives one-way alarm mutations initiated by the Wear companion. */
@AndroidEntryPoint
class PhoneWakeSyncListenerService : WearableListenerService() {
    @Inject
    lateinit var coordinator: AlarmSyncCoordinator

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onMessageReceived(messageEvent: MessageEvent) {
        if (messageEvent.path != AlarmSyncTransportPaths.ALARM_MUTATION) return
        val encoded = messageEvent.data.toString(Charsets.UTF_8)
        scope.launch {
            AlarmSyncCodec.decode(encoded)
                .onSuccess { coordinator.applyRemote(it) }
        }
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }
}
