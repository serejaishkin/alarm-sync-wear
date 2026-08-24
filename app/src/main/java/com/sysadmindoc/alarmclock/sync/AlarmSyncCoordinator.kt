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
        rememberIdentity(syncId, alarmId, revision, timestamp, alarmToken, AlarmSyncSource.WATCH, deviceId)
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
            hour = hour.coerceIn(0, 23), minute = minute.coerceIn(0, 59), label = label.take(120),
            snoozeDurationMinutes = snoozeDurationMinutes.coerceIn(1, 60), vibrationEnabled = vibrationEnabled,
            volume = volume.coerceIn(0, 100), repeatDays = repeatDays, nextTriggerTime = 0L
        ).sanitized()
        alarmRepository.update(updated)
        alarmScheduler.schedule(updated, requestWidgetUpdate = true)
        start()
    }

    suspend fun applyRemote(payload: AlarmSyncPayload): Result<Unit> = runCatching {
        val current = localVersion(payload.syncId)
        val localAlarmId = preferences.getLong(alarmIdKey(payload.syncId), 0L)
        val incoming = AlarmSyncEnvelope(
            deviceId = payload.originDeviceId,
            syncId = payload.syncId,
            alarmId = localAlarmId,
            operation = payload.operation,
            source = payload.source,
            revision = payload.revision,
            timestamp = payload.timestamp,
            payload = AlarmSyncCodec.encode(payload)
        )
        if (current != null && incoming.compareVersion(current) <= 0) return@runCatching

        when (payload.operation) {
            AlarmSyncOperation.DELETE -> {
                val id = preferences.getLong(alarmIdKey(payload.syncId), 0L)
                if (id != 0L) alarmRepository.deleteById(id)
                rememberVersion(payload.syncId, payload.revision, payload.timestamp, payload.source, payload.originDeviceId)
            }
            AlarmSyncOperation.CREATE, AlarmSyncOperation.UPDATE,
            AlarmSyncOperation.ENABLE, AlarmSyncOperation.DISABLE -> {
                val incomingAlarm = AlarmSyncCodec.decodeAlarm(payload).getOrThrow()
                val existingId = preferences.getLong(alarmIdKey(payload.syncId), 0L)
                val savedId = if (existingId != 0L && alarmRepository.getById(existingId) != null) {
                    val updated = incomingAlarm.copy(
                        id = existingId,
                        isEnabled = when (payload.operation) {
                            AlarmSyncOperation.ENABLE -> true
                            AlarmSyncOperation.DISABLE -> false
                            else -> incomingAlarm.isEnabled
                        }, nextTriggerTime = 0L
                    ).sanitized()
                    alarmRepository.update(updated)
                    alarmScheduler.schedule(updated, requestWidgetUpdate = true)
                    existingId
                } else {
                    val newAlarm = incomingAlarm.copy(id = 0L, nextTriggerTime = 0L).sanitized()
                    val newId = alarmRepository.save(newAlarm)
                    alarmRepository.getById(newId)?.let { alarmScheduler.schedule(it, requestWidgetUpdate = true) }
                    newId
                }
                rememberIdentity(payload.syncId, savedId, payload.revision, payload.timestamp, payload.alarmToken, payload.source, payload.originDeviceId)
                payload.alarmToken?.let { remoteSuppressions[savedId] = hash(it) }
            }
            AlarmSyncOperation.RINGING -> startAlarm(payload)
            AlarmSyncOperation.SNOOZE -> alarmCommand(payload, AlarmService.ACTION_SNOOZE)
            AlarmSyncOperation.DISMISS -> alarmCommand(payload, AlarmService.ACTION_DISMISS)
        }
    }

    suspend fun sendWearAction(alarmId: Long, operation: AlarmSyncOperation): Result<Unit> = runCatching {
        require(operation == AlarmSyncOperation.SNOOZE || operation == AlarmSyncOperation.DISMISS ||
            operation == AlarmSyncOperation.ENABLE || operation == AlarmSyncOperation.DISABLE)
        val syncId = preferences.getString(syncIdKey(alarmId), null) ?: error("Alarm $alarmId is not synchronized yet")
        val revision = nextRevision(syncId)
        val alarm = alarmRepository.getById(alarmId) ?: error("Alarm not found")
        val payload = AlarmSyncCodec.create(alarm, syncId, operation, AlarmSyncSource.WATCH, revision, originDeviceId = deviceId)
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
        rememberVersion(syncId, revision, payload.timestamp, AlarmSyncSource.WATCH, deviceId)
    }

    private suspend fun synchronizeSnapshot(alarms: List<Alarm>, force: Boolean) {
        val currentIds = alarms.map { it.id }.filter { it != 0L }.toSet()
        val previousIds = preferences.getStringSet(KEY_KNOWN_ALARM_IDS, emptySet()).orEmpty().mapNotNull { it.toLongOrNull() }.toSet()
        val entries = alarms.filter { it.id != 0L }.map { alarm ->
            val syncId = ensureSyncId(alarm.id)
            AlarmSyncSnapshotEntry(alarm, syncId, preferences.getLong(revisionKey(syncId), 0L))
        }
        transportProvider.transport().publishFullSnapshot(entries)

        for (alarm in alarms) {
            if (alarm.id == 0L) continue
            val syncId = ensureSyncId(alarm.id)
            val currentToken = AlarmSyncCodec.create(alarm, syncId, AlarmSyncOperation.UPDATE, AlarmSyncSource.PHONE, 0L, originDeviceId = deviceId).alarmToken ?: continue
            val tokenHash = hash(currentToken)
            if (!force && remoteSuppressions.remove(alarm.id) == tokenHash) {
                preferences.edit().putString(tokenKey(alarm.id), tokenHash).apply(); continue
            }
            if (!force && preferences.getString(tokenKey(alarm.id), null) == tokenHash) continue
            val operation = if (!force && preferences.getString(tokenKey(alarm.id), null) == null) AlarmSyncOperation.CREATE else AlarmSyncOperation.UPDATE
            val revision = nextRevision(syncId)
            val payload = AlarmSyncCodec.create(alarm, syncId, operation, AlarmSyncSource.PHONE, revision, originDeviceId = deviceId)
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
                preferences.edit().putString(tokenKey(alarm.id), payload.alarmToken?.let(::hash) ?: tokenHash).apply()
                rememberVersion(syncId, revision, payload.timestamp, AlarmSyncSource.PHONE, deviceId)
            }
        }

        for (alarmId in previousIds - currentIds) {
            val syncId = preferences.getString(syncIdKey(alarmId), null) ?: continue
            val revision = nextRevision(syncId)
            val timestamp = System.currentTimeMillis()
            val envelope = AlarmSyncEnvelope(
                deviceId = deviceId,
                syncId = syncId,
                alarmId = alarmId,
                operation = AlarmSyncOperation.DELETE,
                source = AlarmSyncSource.PHONE,
                revision = revision,
                timestamp = timestamp,
                payload = null
            )
            if (transportProvider.transport().send(envelope).isSuccess) {
                rememberVersion(syncId, revision, timestamp, AlarmSyncSource.PHONE, deviceId)
                forgetIdentity(syncId, alarmId, keepVersion = true)
            }
        }
        preferences.edit().putStringSet(KEY_KNOWN_ALARM_IDS, currentIds.map(Long::toString).toSet()).apply()
    }

    private fun startAlarm(payload: AlarmSyncPayload) {
        val id = preferences.getLong(alarmIdKey(payload.syncId), 0L); if (id == 0L) return
        ContextCompat.startForegroundService(context, Intent(context, AlarmService::class.java).apply {
            action = AlarmService.ACTION_START_ALARM
            putExtra(AlarmScheduler.EXTRA_ALARM_ID, id); putExtra(AlarmScheduler.EXTRA_SCHEDULED_AT, payload.timestamp)
        })
    }

    private fun alarmCommand(payload: AlarmSyncPayload, action: String) {
        val id = preferences.getLong(alarmIdKey(payload.syncId), 0L); if (id == 0L) return
        context.startService(Intent(context, AlarmService::class.java).apply {
            this.action = action
            putExtra(AlarmScheduler.EXTRA_ALARM_ID, id); putExtra(AlarmScheduler.EXTRA_SCHEDULED_AT, payload.timestamp)
        })
    }

    private fun ensureSyncId(alarmId: Long) = preferences.getString(syncIdKey(alarmId), null)
        ?: UUID.randomUUID().toString().also { preferences.edit().putString(syncIdKey(alarmId), it).apply() }

    private fun rememberIdentity(syncId: String, alarmId: Long, revision: Long, timestamp: Long, token: String?, source: AlarmSyncSource, originDeviceId: String) {
        preferences.edit().putLong(alarmIdKey(syncId), alarmId).putString(syncIdKey(alarmId), syncId).apply()
        rememberVersion(syncId, revision, timestamp, source, originDeviceId)
        token?.let { preferences.edit().putString(tokenKey(alarmId), hash(it)).apply() }
    }

    private fun rememberVersion(syncId: String, revision: Long, timestamp: Long, source: AlarmSyncSource, originDeviceId: String) {
        preferences.edit().putLong(revisionKey(syncId), revision).putLong(timestampKey(syncId), timestamp)
            .putString(sourceKey(syncId), source.name).putString(originDeviceKey(syncId), originDeviceId).apply()
    }

    private fun localVersion(syncId: String): AlarmSyncEnvelope? {
        val revision = preferences.getLong(revisionKey(syncId), 0L)
        val timestamp = preferences.getLong(timestampKey(syncId), 0L)
        if (revision == 0L && timestamp == 0L) return null
        return AlarmSyncEnvelope(
            deviceId = preferences.getString(originDeviceKey(syncId), "") ?: "",
            syncId = syncId,
            alarmId = preferences.getLong(alarmIdKey(syncId), 0L),
            operation = AlarmSyncOperation.UPDATE,
            source = runCatching { AlarmSyncSource.valueOf(preferences.getString(sourceKey(syncId), AlarmSyncSource.PHONE.name)!!) }.getOrDefault(AlarmSyncSource.PHONE),
            revision = revision,
            timestamp = timestamp
        )
    }

    private fun forgetIdentity(syncId: String, alarmId: Long, keepVersion: Boolean) {
        val editor = preferences.edit().remove(alarmIdKey(syncId)).remove(syncIdKey(alarmId)).remove(tokenKey(alarmId))
        if (!keepVersion) editor.remove(revisionKey(syncId)).remove(timestampKey(syncId)).remove(sourceKey(syncId)).remove(originDeviceKey(syncId))
        editor.apply()
    }

    private fun nextRevision(syncId: String) = preferences.getLong(revisionKey(syncId), 0L) + 1L
    private fun hash(value: String) = MessageDigest.getInstance("SHA-256").digest(value.toByteArray(Charsets.UTF_8)).joinToString("") { "%02x".format(it) }
    private fun syncIdKey(id: Long) = "sync_id_$id"
    private fun alarmIdKey(id: String) = "alarm_id_$id"
    private fun tokenKey(id: Long) = "token_$id"
    private fun revisionKey(id: String) = "revision_$id"
    private fun timestampKey(id: String) = "timestamp_$id"
    private fun sourceKey(id: String) = "source_$id"
    private fun originDeviceKey(id: String) = "origin_device_$id"

    companion object {
        private const val PREFS_NAME = "wakesync_state"
        private const val KEY_DEVICE_ID = "device_id"
        private const val KEY_KNOWN_ALARM_IDS = "known_alarm_ids"
    }
}
