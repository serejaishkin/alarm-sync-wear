package com.wakesync.app.sync

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.google.android.gms.wearable.Wearable
import com.wakesync.app.data.repository.AlarmRepository
import com.wakesync.app.domain.AlarmScheduler
import dagger.hilt.EntryPoint
import dagger.hilt.EntryPoints
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.UUID

/** Best-effort phone -> Wear propagation of a local alarm user action. */
class PhoneLocalAlarmActionReceiver : BroadcastReceiver() {
    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface EntryPointAccess {
        fun repository(): AlarmRepository
    }

    override fun onReceive(context: Context, intent: Intent) {
        val alarmId = intent.getLongExtra(AlarmScheduler.EXTRA_ALARM_ID, -1L)
        if (alarmId <= 0L) return
        val operation = intent.getStringExtra(EXTRA_OPERATION) ?: return
        if (operation != "SNOOZE" && operation != "DISMISS") return
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val app = context.applicationContext
                val repository = EntryPoints.get(app, EntryPointAccess::class.java).repository()
                val alarm = repository.getById(alarmId) ?: return@launch
                val prefs = app.getSharedPreferences("wakesync_state", Context.MODE_PRIVATE)
                val syncId = prefs.getString("sync_id_$alarmId", null)
                    ?: UUID.randomUUID().toString().also {
                        prefs.edit().putString("sync_id_$alarmId", it).apply()
                    }
                val revision = prefs.getLong("revision_$syncId", 0L) + 1L
                val payload = AlarmSyncCodec.create(
                    alarm = alarm,
                    syncId = syncId,
                    operation = AlarmSyncOperation.valueOf(operation),
                    source = AlarmSyncSource.PHONE,
                    revision = revision
                )
                val bytes = AlarmSyncCodec.encode(payload).toByteArray(Charsets.UTF_8)
                Wearable.getNodeClient(app).connectedNodes.addOnSuccessListener { nodes ->
                    nodes.forEach { node ->
                        Wearable.getMessageClient(app)
                            .sendMessage(node.id, "/wakesync/alarm/mutation", bytes)
                    }
                }
                prefs.edit()
                    .putLong("revision_$syncId", revision)
                    .putLong("timestamp_$syncId", payload.timestamp)
                    .apply()
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        const val EXTRA_OPERATION = "operation"
    }
}
