package com.sysadmindoc.alarmclock.sync

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.google.android.gms.wearable.Wearable
import com.sysadmindoc.alarmclock.data.repository.AlarmRepository
import com.sysadmindoc.alarmclock.domain.AlarmScheduler
import dagger.hilt.EntryPoint
import dagger.hilt.EntryPoints
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.UUID

/** Publishes a local phone fire immediately when a peer is connected. */
class PhoneLocalAlarmTriggerReceiver : BroadcastReceiver() {
    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface EntryPointAccess {
        fun repository(): AlarmRepository
    }

    override fun onReceive(context: Context, intent: Intent) {
        val alarmId = intent.getLongExtra(AlarmScheduler.EXTRA_ALARM_ID, -1L)
        if (alarmId <= 0L) return
        val scheduledAt = intent.getLongExtra(AlarmScheduler.EXTRA_SCHEDULED_AT, System.currentTimeMillis())
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
                val timestamp = System.currentTimeMillis()
                val payload = AlarmSyncCodec.create(
                    alarm = alarm,
                    syncId = syncId,
                    operation = AlarmSyncOperation.RINGING,
                    source = AlarmSyncSource.PHONE,
                    revision = revision,
                    timestamp = timestamp
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
                    .putLong("timestamp_$syncId", timestamp)
                    .apply()
            } finally {
                pendingResult.finish()
            }
        }
    }
}
