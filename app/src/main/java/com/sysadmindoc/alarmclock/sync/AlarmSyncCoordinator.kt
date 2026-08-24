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
    private val alarmScheduler: AlarmScheduler,
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
            alarmRepository.observeAll().collectLatest { synchronizeSnapshot(it, false) }
        }
    }

    suspend fun syncNow() = synchronizeSnapshot(alarmRepository.getAll(), true)
    fun stop() { observationJob?.cancel(); observationJob = null }

    suspend fun registerWearCreatedAlarm(
        syncId: String, alarmId: Long, revision: Long, timestamp: Long, alarmToken: String?
    ) {
        require(syncId.isNotBlank())
        require(alarmId > 0L)
        rememberIdentity(syncId, alarmId, revision, timestamp, alarmToken)
        // The token identifies the alarm that came from Wear; it must NOT be
        // treated as already delivered by the phone. Otherwise the snapshot
        // publisher skips it and the alarm never makes the round trip back to
        // Wear, which makes Wear-created alarms appear only on the phone.
        preferences.edit().remove(tokenKey(alarmId)).apply()
        val ids = preferences.getStringSet(KEY_KNOWN_ALARM_IDS, emptySet()).orEmpty().toMutableSet()
        ids.add(alarmId.toString())
        preferences.edit().putStringSet(KEY_KNOWN_ALARM_IDS, ids).apply()
    }

    suspend fun updateFromWear(
        syncId: String,
        hour: Int,
        minute: Int,
        label: String,
        snoozeDurationMinutes: Int,
        vibrationEnabled: Boolean,
        volume: Int,
        repeatDays: Set<java.time.DayOfWeek> = emptySet()
    ): Result<Unit> = runCatching {
        val alarmId = preferences.getLong(alarmIdKey(syncId), 0L)
        require(alarmId != 0L) { "Unknown synchronized alarm: $syncId" }
        val current = alarmRepository.getById(alarmId) ?: error("Alarm $alarmId no longer exists")
        val updated = current.copy(
            hour = hour.coerceIn(0, 23),
            minute = minute.coerceIn(0, 59),
            label = label.take(120),
            snoozeDurationMinutes = snoozeDurationMinutes.coerceIn(1, 60),
            vibrationEnabled = vibrationEnabled,
            volume = volume.coerceIn(0, 100),
            repeatDays = repeatDays,
            nextTriggerTime = 0L
        ).sanitized()
        alarmRepository.update(updated)
        alarmScheduler.schedule(updated, requestWidgetUpdate = true)
        start()
    }

    suspend fun applyRemote(payload: AlarmSyncPayload): Result<Unit> = runCatching {
        val currentRevision = preferences.getLong(revisionKey(payload.syncId), 0L)
        val currentTimestamp = preferences.getLong(timestampKey(payload.syncId), 0L)
        if (payload.revision < currentRevision ||
            payload.revision == currentRevision && payload.timestamp <= currentTimestamp) return@runCatching

        when (payload.operation) {
            AlarmSyncOperation.DELETE -> {
                val id = preferences.getLong(alarmIdKey(payload.syncId), 0L)
                if (id != 0L) alarmRepository.deleteById(id)
                forgetIdentity(payload.syncId, id)
            }
            AlarmSyncOperation.CREATE, AlarmSyncOperation.UPDATE,
            AlarmSyncOperation.ENABLE, AlarmSyncOperation.DISABLE -> {
                val incoming = AlarmSyncCodec.decodeAlarm(payload).getOrThrow()
                val existingId = preferences.getLong(alarmIdKey(payload.syncId), 0L)
                val savedId = if (existingId != 0L && alarmRepository.getById(existingId) != null) {
                    val updated = incoming.copy(
                        id = existingId,
                        isEnabled = when (payload.operation) {
                            AlarmSyncOperation.ENABLE -> true
                            AlarmSyncOperation.DISABLE -> false
                            else -> incoming.isEnabled
                        },
                        nextTriggerTime = 0L
                    ).sanitized()
                    alarmRepository.update(updated)
                    alarmScheduler.schedule(updated, requestWidgetUpdate = true)
                    existingId
                } else {
                    val newAlarm = incoming.copy(id = 0L, nextTriggerTime = 0L).sanitized()
                    val newId = alarmRepository.save(newAlarm)
                    val savedAlarm = alarmRepository.getById(newId)
                    if (savedAlarm != null) alarmScheduler.schedule(savedAlarm, requestWidgetUpdate = true)
                    newId
                }
                rememberIdentity(payload.syncId, savedId, payload.revision, payload.timestamp, payload.alarmToken)
                payload.alarmToken?.let { remoteSuppressions[savedId] = hash(it) }
            }
            AlarmSyncOperation.RINGING -> startAlarm(payload)
            AlarmSyncOperation.SNOOZE -> alarmCommand(payload, AlarmService.ACTION_SNOOZE)
            AlarmSyncOperation.DISMISS -> alarmCommand(payload, AlarmService.ACTION_DISMISS)
        }
        preferences.edit().putLong(revisionKey(payload.syncId), payload.revision)
            .putLong(timestampKey(payload.syncId), payload.timestamp).apply()
    }

    suspend fun sendWearAction(alarmId: Long, operation: AlarmSyncOperation): Result<Unit> = runCatching {
        require(operation == AlarmSyncOperation.SNOOZE || operation == AlarmSyncOperation.DISMISS ||
            operation == AlarmSyncOperation.ENABLE || operation == AlarmSyncOperation.DISABLE)
        val syncId = preferences.getString(syncIdKey(alarmId), null) ?: error("Alarm $alarmId is not synchronized yet")
        val revision = nextRevision(syncId)
        val alarm = alarmRepository.getById(alarmId) ?: error("Alarm not found")
        val payload = AlarmSyncCodec.create(alarm, syncId, operation, AlarmSyncSource.WATCH, revision)
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
            AlarmSyncOperation.ENABLE, AlarmSyncOperation.DISABLE -> {
                alarmRepository.setEnabled(alarmId, operation == AlarmSyncOperation.ENABLE, 0L)
                alarmRepository.getById(alarmId)?.let { alarmScheduler.schedule(it, requestWidgetUpdate = true) }
            }
            else -> Unit
        }
        preferences.edit().putLong(revisionKey(syncId), revision).putLong(timestampKey(syncId), payload.timestamp).apply()
    }

    private suspend fun synchronizeSnapshot(alarms: List<Alarm>, force: Boolean) {
        val currentIds = alarms.map { it.id }.filter { it != 0L }.toSet()
        val previousIds = preferences.getStringSet(KEY_KNOWN_ALARM_IDS, emptySet()).orEmpty()
            .mapNotNull { it.toLongOrNull() }.toSet()
        val entries = alarms.filter { it.id != 0L }.map { alarm ->
            val syncId = ensureSyncId(alarm.id)
            AlarmSyncSnapshotEntry(alarm, syncId, preferences.getLong(revisionKey(syncId), 0L))
        }
        transportProvider.transport().publishFullSnapshot(entries)
        for (alarm in alarms) {
            if (alarm.id == 0L) continue
            val syncId = ensureSyncId(alarm.id)
            val token = AlarmSyncCodec.create(alarm, syncId, AlarmSyncOperation.UPDATE, AlarmSyncSource.PHONE, nextRevision(syncId)).alarmToken ?: continue
            val tokenHash = hash(token)
            if (!force && remoteSuppressions.remove(alarm.id) == tokenHash) {
                preferences.edit().putString(tokenKey(alarm.id), tokenHash).apply(); continue
            }
            if (!force && preferences.getString(tokenKey(alarm.id), null) == tokenHash) continue
            val operation = if (!force && preferences.getString(tokenKey(alarm.id), null) == null)
                AlarmSyncOperation.CREATE else AlarmSyncOperation.UPDATE
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
                preferences.edit().putString(tokenKey(alarm.id), tokenHash)
                    .putLong(revisionKey(syncId), revision).putLong(timestampKey(syncId), payload.timestamp).apply()
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
        val id = preferences.getLong(alarmIdKey(payload.syncId), 0L)
        if (id == 0L) return
        ContextCompat.startForegroundService(context, Intent(context, AlarmService::class.java).apply {
            action = AlarmService.ACTION_START_ALARM
            putExtra(AlarmScheduler.EXTRA_ALARM_ID, id)
            putExtra(AlarmScheduler.EXTRA_SCHEDULED_AT, payload.timestamp)
        })
    }

    private fun alarmCommand(payload: AlarmSyncPayload, action: String) {
        val id = preferences.getLong(alarmIdKey(payload.syncId), 0L)
        if (id == 0L) return
        context.startService(Intent(context, AlarmService::class.java).apply {
            this.action = action
            putExtra(AlarmScheduler.EXTRA_ALARM_ID, id)
            putExtra(AlarmScheduler.EXTRA_SCHEDULED_AT, payload.timestamp)
        })
    }

    private fun ensureSyncId(alarmId: Long) = preferences.getString(syncIdKey(alarmId), null)
        ?: UUID.randomUUID().toString().also { preferences.edit().putString(syncIdKey(alarmId), it).apply() }

    private fun rememberIdentity(syncId: String, alarmId: Long, revision: Long, timestamp: Long, token: String?) {
        preferences.edit().putLong(alarmIdKey(syncId), alarmId).putString(syncIdKey(alarmId), syncId)
            .putLong(revisionKey(syncId), revision).putLong(timestampKey(syncId), timestamp).apply()
        token?.let { preferences.edit().putString(tokenKey(alarmId), hash(it)).apply() }
    }

    private fun forgetIdentity(syncId: String, alarmId: Long) {
        preferences.edit().remove(alarmIdKey(syncId)).remove(revisionKey(syncId)).remove(timestampKey(syncId))
            .remove(syncIdKey(alarmId)).remove(tokenKey(alarmId)).apply()
    }

    private fun nextRevision(syncId: String) = preferences.getLong(revisionKey(syncId), 0L) + 1L
    private fun hash(value: String) = MessageDigest.getInstance("SHA-256").digest(value.toByteArray(Charsets.UTF_8))
        .joinToString("") { "%02x".format(it) }
    private fun syncIdKey(id: Long) = "sync_id_$id"
    private fun alarmIdKey(id: String) = "alarm_id_$id"
    private fun tokenKey(id: Long) = "token_$id"
    private fun revisionKey(id: String) = "revision_$id"
    private fun timestampKey(id: String) = "timestamp_$id"

    companion object {
        private const val PREFS_NAME = "wakesync_state"
        private const val KEY_DEVICE_ID = "device_id"
        private const val KEY_KNOWN_ALARM_IDS = "known_alarm_ids"
    }
}
