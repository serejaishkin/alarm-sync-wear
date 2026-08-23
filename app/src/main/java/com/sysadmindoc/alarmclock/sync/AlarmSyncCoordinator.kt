package com.sysadmindoc.alarmclock.sync

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import com.sysadmindoc.alarmclock.data.model.Alarm
import com.sysadmindoc.alarmclock.data.repository.AlarmRepository
import com.sysadmindoc.alarmclock.domain.AlarmScheduler
import com.sysadmindoc.alarmclock.service.AlarmService
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.security.MessageDigest
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AlarmSyncCoordinator @Inject constructor(
    @ApplicationContext private val context: Context,
    private val alarmRepository: AlarmRepository,
    private val transportProvider: AlarmSyncTransportProvider
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val remoteSuppressions = mutableMapOf<Long, String>()
    private var observationJob: Job? = null

    private val deviceId: String
        get() = preferences.getString(KEY_DEVICE_ID, null)
            ?: UUID.randomUUID().toString().also { preferences.edit().putString(KEY_DEVICE_ID, it).apply() }

    fun start() {
        if (observationJob?.isActive == true) return
        observationJob = scope.launch {
            alarmRepository.observeAll().collectLatest { alarms -> synchronizeSnapshot(alarms) }
        }
    }

    fun stop() { observationJob?.cancel(); observationJob = null }

    suspend fun updateFromWear(
        syncId: String,
        hour: Int,
        minute: Int,
        label: String,
        snoozeDurationMinutes: Int,
        vibrationEnabled: Boolean,
        volume: Int
    ): Result<Unit> = runCatching {
        val alarmId = preferences.getLong(alarmIdKey(syncId), 0L)
        require(alarmId != 0L) { "Unknown synchronized alarm: $syncId" }
        val current = alarmRepository.getById(alarmId)
            ?: throw IllegalStateException("Alarm $alarmId no longer exists")
        alarmRepository.update(
            current.copy(
                hour = hour.coerceIn(0, 23),
                minute = minute.coerceIn(0, 59),
                label = label.take(120),
                snoozeDurationMinutes = snoozeDurationMinutes.coerceIn(1, 60),
                vibrationEnabled = vibrationEnabled,
                volume = volume.coerceIn(0, 100)
            ).sanitized()
        )
        start()
    }

    suspend fun applyRemote(payload: AlarmSyncPayload): Result<Unit> = runCatching {
        val currentRevision = preferences.getLong(revisionKey(payload.syncId), 0L)
        if (payload.revision < currentRevision) return@runCatching
        if (payload.revision == currentRevision && payload.timestamp <= preferences.getLong(timestampKey(payload.syncId), 0L)) return@runCatching
        when (payload.operation) {
            AlarmSyncOperation.DELETE -> {
                val alarmId = preferences.getLong(alarmIdKey(payload.syncId), 0L)
                if (alarmId != 0L) alarmRepository.deleteById(alarmId)
                forgetIdentity(payload.syncId, alarmId)
            }
            AlarmSyncOperation.CREATE, AlarmSyncOperation.UPDATE, AlarmSyncOperation.ENABLE, AlarmSyncOperation.DISABLE -> {
                val incoming = AlarmSyncCodec.decodeAlarm(payload).getOrThrow()
                val existingId = preferences.getLong(alarmIdKey(payload.syncId), 0L)
                val savedId = if (existingId != 0L && alarmRepository.getById(existingId) != null) {
                    val updated = incoming.copy(
                        id = existingId,
                        isEnabled = when (payload.operation) {
                            AlarmSyncOperation.ENABLE -> true
                            AlarmSyncOperation.DISABLE -> false
                            else -> incoming.isEnabled
                        }
                    ).sanitized()
                    alarmRepository.update(updated)
                    existingId
                } else {
                    alarmRepository.save(incoming.copy(id = 0L, nextTriggerTime = 0L).sanitized())
                }
                rememberIdentity(payload.syncId, savedId, payload.revision, payload.timestamp, payload.alarmToken)
                if (payload.alarmToken != null) remoteSuppressions[savedId] = hash(payload.alarmToken)
            }
            AlarmSyncOperation.RINGING -> startAlarm(payload)
            AlarmSyncOperation.SNOOZE -> alarmCommand(payload, AlarmService.ACTION_SNOOZE)
            AlarmSyncOperation.DISMISS -> alarmCommand(payload, AlarmService.ACTION_DISMISS)
        }
        preferences.edit()
            .putLong(revisionKey(payload.syncId), payload.revision)
            .putLong(timestampKey(payload.syncId), payload.timestamp)
            .apply()
    }

    suspend fun sendWearAction(alarmId: Long, operation: AlarmSyncOperation): Result<Unit> = runCatching {
        require(
            operation == AlarmSyncOperation.SNOOZE ||
                operation == AlarmSyncOperation.DISMISS ||
                operation == AlarmSyncOperation.ENABLE ||
                operation == AlarmSyncOperation.DISABLE
        )
        val syncId = preferences.getString(syncIdKey(alarmId), null)
            ?: throw IllegalStateException("Alarm $alarmId is not synchronized yet")
        val revision = nextRevision(syncId)
        val payload = AlarmSyncCodec.create(
            alarmRepository.getById(alarmId) ?: throw IllegalArgumentException("Alarm not found"),
            syncId,
            operation,
            AlarmSyncSource.WATCH,
            revision
        )
        val envelope = AlarmSyncEnvelope(
            deviceId = deviceId,
            syncId = syncId,
            alarmId = alarmId,
            operation = operation,
            source = AlarmSyncSource.WATCH,
            revision = revision,
            timestamp = payload.timestamp,
            payload = AlarmSyncCodec.encode(payload)
        )
        transportProvider.transport().send(envelope).getOrThrow()
        when (operation) {
            AlarmSyncOperation.SNOOZE -> alarmCommand(payload, AlarmService.ACTION_SNOOZE)
            AlarmSyncOperation.DISMISS -> alarmCommand(payload, AlarmService.ACTION_DISMISS)
            AlarmSyncOperation.ENABLE, AlarmSyncOperation.DISABLE -> alarmRepository.setEnabled(alarmId, operation == AlarmSyncOperation.ENABLE, 0L)
            else -> Unit
        }
        preferences.edit().putLong(revisionKey(syncId), revision).putLong(timestampKey(syncId), payload.timestamp).apply()
    }

    private suspend fun synchronizeSnapshot(alarms: List<Alarm>) {
        val currentIds = alarms.map { it.id }.filter { it != 0L }.toSet()
        val previousIds = preferences.getStringSet(KEY_KNOWN_ALARM_IDS, emptySet())?.mapNotNull { it.toLongOrNull() }?.toSet() ?: emptySet()
        transportProvider.transport().publishSnapshot(alarms)
        for (alarm in alarms) {
            if (alarm.id == 0L) continue
            val syncId = ensureSyncId(alarm.id)
            val token = AlarmSyncCodec.create(alarm, syncId, AlarmSyncOperation.UPDATE, AlarmSyncSource.PHONE, nextRevision(syncId)).alarmToken ?: continue
            val tokenHash = hash(token)
            if (remoteSuppressions.remove(alarm.id) == tokenHash) {
                preferences.edit().putString(tokenKey(alarm.id), tokenHash).apply()
                continue
            }
            if (preferences.getString(tokenKey(alarm.id), null) == tokenHash) continue
            val operation = if (preferences.getString(tokenKey(alarm.id), null) == null) AlarmSyncOperation.CREATE else AlarmSyncOperation.UPDATE
            val revision = nextRevision(syncId)
            val payload = AlarmSyncCodec.create(alarm, syncId, operation, AlarmSyncSource.PHONE, revision)
            val envelope = AlarmSyncEnvelope(
                deviceId = deviceId,
                syncId = syncId,
                alarmId = alarm.id,
                operation = operation,
                source = AlarmSyncSource.PHONE,
                revision = revision,
                timestamp = payload.timestamp,
                payload = AlarmSyncCodec.encode(payload)
            )
            if (transportProvider.transport().send(envelope).isSuccess) {
                preferences.edit().putString(tokenKey(alarm.id), tokenHash).putLong(revisionKey(syncId), revision).putLong(timestampKey(syncId), payload.timestamp).apply()
            }
        }
        for (alarmId in previousIds - currentIds) {
            val syncId = preferences.getString(syncIdKey(alarmId), null) ?: continue
            val revision = nextRevision(syncId)
            val envelope = AlarmSyncEnvelope(
                deviceId = deviceId,
                syncId = syncId,
                alarmId = alarmId,
                operation = AlarmSyncOperation.DELETE,
                source = AlarmSyncSource.PHONE,
                revision = revision,
                timestamp = System.currentTimeMillis(),
                payload = null
            )
            if (transportProvider.transport().send(envelope).isSuccess) forgetIdentity(syncId, alarmId)
        }
        preferences.edit().putStringSet(KEY_KNOWN_ALARM_IDS, currentIds.map(Long::toString).toSet()).apply()
    }

    private fun startAlarm(payload: AlarmSyncPayload) {
        val alarmId = preferences.getLong(alarmIdKey(payload.syncId), 0L)
        if (alarmId == 0L) return
        ContextCompat.startForegroundService(context, Intent(context, AlarmService::class.java).apply {
            action = AlarmService.ACTION_START_ALARM
            putExtra(AlarmScheduler.EXTRA_ALARM_ID, alarmId)
            putExtra(AlarmScheduler.EXTRA_SCHEDULED_AT, payload.timestamp)
        })
    }

    private fun alarmCommand(payload: AlarmSyncPayload, action: String) {
        val alarmId = preferences.getLong(alarmIdKey(payload.syncId), 0L)
        if (alarmId == 0L) return
        context.startService(Intent(context, AlarmService::class.java).apply {
            this.action = action
            putExtra(AlarmScheduler.EXTRA_ALARM_ID, alarmId)
            putExtra(AlarmScheduler.EXTRA_SCHEDULED_AT, payload.timestamp)
        })
    }

    private fun ensureSyncId(alarmId: Long): String = preferences.getString(syncIdKey(alarmId), null)
        ?: UUID.randomUUID().toString().also { preferences.edit().putString(syncIdKey(alarmId), it).apply() }

    private fun rememberIdentity(syncId: String, alarmId: Long, revision: Long, timestamp: Long, token: String?) {
        preferences.edit().putLong(alarmIdKey(syncId), alarmId).putString(syncIdKey(alarmId), syncId).putLong(revisionKey(syncId), revision).putLong(timestampKey(syncId), timestamp).apply()
        token?.let { preferences.edit().putString(tokenKey(alarmId), hash(it)).apply() }
    }

    private fun forgetIdentity(syncId: String, alarmId: Long) {
        preferences.edit().remove(alarmIdKey(syncId)).remove(revisionKey(syncId)).remove(timestampKey(syncId)).remove(syncIdKey(alarmId)).remove(tokenKey(alarmId)).apply()
    }

    private fun nextRevision(syncId: String): Long = preferences.getLong(revisionKey(syncId), 0L) + 1L
    private fun hash(value: String): String = MessageDigest.getInstance("SHA-256").digest(value.toByteArray(Charsets.UTF_8)).joinToString("") { "%02x".format(it) }
    private fun syncIdKey(alarmId: Long) = "sync_id_$alarmId"
    private fun alarmIdKey(syncId: String) = "alarm_id_$syncId"
    private fun tokenKey(alarmId: Long) = "token_$alarmId"
    private fun revisionKey(syncId: String) = "revision_$syncId"
    private fun timestampKey(syncId: String) = "timestamp_$syncId"

    companion object {
        private const val PREFS_NAME = "wakesync_state"
        private const val KEY_DEVICE_ID = "device_id"
        private const val KEY_KNOWN_ALARM_IDS = "known_alarm_ids"
    }
}
