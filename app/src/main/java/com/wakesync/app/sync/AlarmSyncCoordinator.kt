package com.wakesync.app.sync

import android.content.Context
import android.content.Intent
import android.util.Log
import com.wakesync.app.data.model.Alarm
import com.wakesync.app.data.local.entity.AlarmIncidentEvent
import com.wakesync.app.data.repository.AlarmRepository
import com.wakesync.app.domain.AlarmScheduler
import com.wakesync.app.service.AlarmService
import com.wakesync.app.service.AlarmFireDismissContract
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.security.MessageDigest
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/** Provenance of the last synced change for one alarm, for UI display. */
data class AlarmLastChange(val fromWatch: Boolean, val timestamp: Long)

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
    private val syncMutex = Mutex()
    private var observationJob: Job? = null

    private val deviceId: String
        get() = preferences.getString(KEY_DEVICE_ID, null)
            ?: UUID.randomUUID().toString().also { preferences.edit().putString(KEY_DEVICE_ID, it).apply() }

    fun start() {
        if (observationJob?.isActive == true) return
        android.util.Log.i("AlarmSync", "Starting sync coordinator")
        observationJob = scope.launch {
            // Do not use collectLatest here: cancelling an in-flight Data Layer
            // write can lose the first CREATE during app startup/reinstall.
            alarmRepository.observeAll().collect { syncMutex.withLock { synchronizeSnapshot(it) } }
        }
        scope.launch {
            requestWatchSnapshot()
            delay(3_000L)
            requestWatchSnapshot()
            delay(10_000L)
            requestWatchSnapshot()
        }
    }

    fun requestWatchSnapshot() {
        transportProvider.transport().requestWatchSnapshot()
            .onFailure { Log.w(TAG, "requestWatchSnapshot failed", it) }
    }

    suspend fun syncNow() {
        syncMutex.withLock {
            val alarms = alarmRepository.getAll()
            synchronizeSnapshot(alarms)
            publishFullSnapshot(alarms)
        }
    }
    fun stop() { observationJob?.cancel(); observationJob = null }

    suspend fun registerWearCreatedAlarm(syncId: String, alarmId: Long, revision: Long, timestamp: Long, alarmToken: String?) {
        require(syncId.isNotBlank()); require(alarmId > 0L)
        rememberIdentity(syncId, alarmId, revision, timestamp, alarmToken, AlarmSyncSource.WATCH, deviceId)
        val ids = preferences.getStringSet(KEY_KNOWN_ALARM_IDS, emptySet()).orEmpty().toMutableSet()
        ids.add(alarmId.toString())
        preferences.edit().remove(tokenKey(alarmId)).putStringSet(KEY_KNOWN_ALARM_IDS, ids).apply()
    }

    suspend fun updateFromWear(syncId: String, hour: Int, minute: Int, label: String, snoozeDurationMinutes: Int,
        vibrationEnabled: Boolean, volume: Int, repeatDays: Set<java.time.DayOfWeek> = emptySet()): Result<Unit> = runCatching {
        require(syncId.isNotBlank())
        val alarmId = preferences.getLong(alarmIdKey(syncId), 0L)
        require(alarmId != 0L) { "Unknown synchronized alarm: $syncId" }
        val current = alarmRepository.getById(alarmId) ?: error("Alarm $alarmId no longer exists")
        val updated = current.copy(hour = hour.coerceIn(0, 23), minute = minute.coerceIn(0, 59), label = label.take(120),
            snoozeDurationMinutes = snoozeDurationMinutes.coerceIn(1, 60), vibrationEnabled = vibrationEnabled,
            volume = volume.coerceIn(0, 100), repeatDays = repeatDays, nextTriggerTime = 0L).sanitized()
        alarmRepository.update(updated)
        alarmScheduler.schedule(updated, requestWidgetUpdate = true)
        start()
    }

    suspend fun applyRemote(payload: AlarmSyncPayload): Result<Unit> =
        syncMutex.withLock { applyRemoteUnlocked(payload) }

    private suspend fun applyRemoteUnlocked(payload: AlarmSyncPayload): Result<Unit> = runCatching {
        val localAlarmId = preferences.getLong(alarmIdKey(payload.syncId), 0L)
        Log.i(TAG, "applyRemote: op=${payload.operation} syncId=${payload.syncId} rev=${payload.revision} " +
            "ts=${payload.timestamp} src=${payload.source} origin=${payload.originDeviceId} localAlarmId=$localAlarmId " +
            "enabled=${payload.enabled} hasToken=${payload.alarmToken != null}")
        if (payload.operation == AlarmSyncOperation.DELETE) {
            val currentVersion = localVersion(payload.syncId)
            val incomingDelete = AlarmSyncEnvelope(
                deviceId = payload.originDeviceId,
                syncId = payload.syncId,
                alarmId = localAlarmId,
                operation = payload.operation,
                source = payload.source,
                revision = payload.revision,
                timestamp = payload.timestamp,
                payload = AlarmSyncCodec.encode(payload)
            )
            if (currentVersion != null && incomingDelete.compareVersion(currentVersion) <= 0) {
                Log.w(TAG, "applyRemote: VERSION GATE BLOCKED DELETE syncId=${payload.syncId} " +
                    "incoming(rev=${payload.revision},ts=${payload.timestamp}) " +
                    "current(rev=${currentVersion.revision},ts=${currentVersion.timestamp})")
                return@runCatching
            }
            Log.i(TAG, "applyRemote: DELETE accepted for syncId=${payload.syncId} localAlarmId=$localAlarmId")
            if (localAlarmId != 0L) {
                alarmRepository.deleteById(localAlarmId)
                val known = preferences.getStringSet(KEY_KNOWN_ALARM_IDS, emptySet()).orEmpty().toMutableSet()
                known.remove(localAlarmId.toString())
                preferences.edit().putStringSet(KEY_KNOWN_ALARM_IDS, known).remove(tokenKey(localAlarmId)).apply()
            }
            rememberVersion(payload.syncId, payload.revision, payload.timestamp, payload.source, payload.originDeviceId)
            Log.i(TAG, "applyRemote: DELETE completed for syncId=${payload.syncId}")
            return@runCatching
        }
        val current = localVersion(payload.syncId)
        val incoming = AlarmSyncEnvelope(deviceId = payload.originDeviceId, syncId = payload.syncId, alarmId = localAlarmId,
            operation = payload.operation, source = payload.source, revision = payload.revision, timestamp = payload.timestamp,
            payload = AlarmSyncCodec.encode(payload))
        if (current != null && incoming.compareVersion(current) <= 0) {
            Log.w(TAG, "applyRemote: VERSION GATE BLOCKED op=${payload.operation} syncId=${payload.syncId} " +
                "incoming(rev=${incoming.revision},ts=${incoming.timestamp},src=${incoming.source}) " +
                "current(rev=${current.revision},ts=${current.timestamp},src=${current.source})")
            return@runCatching
        }
        Log.i(TAG, "applyRemote: VERSION GATE PASSED op=${payload.operation} syncId=${payload.syncId}")
        when (payload.operation) {
            AlarmSyncOperation.CREATE, AlarmSyncOperation.UPDATE, AlarmSyncOperation.ENABLE, AlarmSyncOperation.DISABLE -> {
                val incomingAlarm = if (payload.source == AlarmSyncSource.WATCH &&
                    payload.operation == AlarmSyncOperation.UPDATE
                ) {
                    // Wear keeps the phone-compatible token for identity, but
                    // that token can contain an older alarm snapshot. The
                    // editable fields in the current WATCH mutation are the
                    // authoritative values for this update.
                    alarmFromPayload(payload)
                } else {
                    AlarmSyncCodec.decodeAlarm(payload)
                    .getOrElse {
                        // Older Wear builds send the editable fields with a local
                        // alarmToken, not a phone-compatible AlarmShare token.
                        // The fields are already part of the versioned envelope,
                        // so use them as the canonical CREATE/UPDATE payload.
                        Log.w(TAG, "applyRemote: using field payload for ${payload.syncId}; alarmToken is not share-compatible", it)
                        alarmFromPayload(payload)
                    }
                }
                Log.i(TAG, "applyRemote: decoded alarm hour=${incomingAlarm.hour} min=${incomingAlarm.minute} " +
                    "enabled=${incomingAlarm.isEnabled} label=${incomingAlarm.label}")
                val existingId = preferences.getLong(alarmIdKey(payload.syncId), 0L)
                val savedId = if (existingId != 0L && alarmRepository.getById(existingId) != null) {
                    val currentAlarm = alarmRepository.getById(existingId)!!
                    val updated = if (payload.operation == AlarmSyncOperation.ENABLE ||
                        payload.operation == AlarmSyncOperation.DISABLE
                    ) {
                        // ENABLE/DISABLE are state-only mutations. Do not copy
                        // stale editor fields from the watch over a newer
                        // phone-side time, label, or repeat schedule.
                        currentAlarm.copy(
                            isEnabled = payload.operation == AlarmSyncOperation.ENABLE,
                            nextTriggerTime = 0L
                        ).sanitized()
                    } else {
                        incomingAlarm.copy(id = existingId, nextTriggerTime = 0L).sanitized()
                    }
                    alarmRepository.update(updated); alarmScheduler.schedule(updated, requestWidgetUpdate = true); existingId
                } else {
                    val newId = alarmRepository.save(incomingAlarm.copy(id = 0L, nextTriggerTime = 0L).sanitized())
                    alarmRepository.getById(newId)?.let { alarmScheduler.schedule(it, requestWidgetUpdate = true) }; newId
                }
                Log.i(TAG, "applyRemote: DB updated savedId=$savedId op=${payload.operation} " +
                    "isEnabled=${if (payload.operation == AlarmSyncOperation.ENABLE) true else if (payload.operation == AlarmSyncOperation.DISABLE) false else incomingAlarm.isEnabled}")
                rememberIdentity(payload.syncId, savedId, payload.revision, payload.timestamp, payload.alarmToken, payload.source, payload.originDeviceId)
                // Fix: re-encode token from local DB state so hash matches what synchronizeSnapshot will produce
                val localAlarm = alarmRepository.getById(savedId)
                val localToken = localAlarm?.let { AlarmSyncCodec.create(it, payload.syncId, AlarmSyncOperation.UPDATE, AlarmSyncSource.PHONE, 0L, originDeviceId = deviceId).alarmToken }
                remoteSuppressions[savedId] = localToken?.let(::hash) ?: ""
                Log.i(TAG, "applyRemote: remoteSuppressions[$savedId] = ${remoteSuppressions[savedId]!!.take(16)}...")
                if (payload.operation == AlarmSyncOperation.DISABLE) alarmCommand(payload, AlarmService.ACTION_DISMISS)
            }
            // The watch plays its own feedback when the alarm fires; RINGING must
            // not start media playback on the phone. Snooze/dismiss still mirror.
            AlarmSyncOperation.RINGING -> {
                // The watch owns sound and haptics, but the phone must still
                // show its firing animation and challenge UI. Do not start
                // AlarmService here: that would play a second alarm locally.
                val id = preferences.getLong(alarmIdKey(payload.syncId), 0L)
                val alarm = id.takeIf { it > 0L }?.let { alarmRepository.getById(it) }
                if (id > 0L && alarm != null) {
                    context.startForegroundService(Intent(context, AlarmService::class.java).apply {
                        action = AlarmService.ACTION_REMOTE_RINGING
                        putExtra(AlarmScheduler.EXTRA_ALARM_ID, id)
                        putExtra(
                            AlarmScheduler.EXTRA_SCHEDULED_AT,
                            payload.timestamp.takeIf { it > 0L } ?: System.currentTimeMillis()
                        )
                        putExtra(
                            AlarmScheduler.EXTRA_ALARM_FIRE_ID,
                            AlarmIncidentEvent.fireIdFor(
                                id,
                                payload.timestamp.takeIf { it > 0L } ?: System.currentTimeMillis()
                            )
                        )
                    })
                    Log.i(TAG, "applyRemote: started phone remote-ring service for syncId=${payload.syncId}")
                } else {
                    Log.w(TAG, "applyRemote: cannot open phone firing UI; unknown alarm syncId=${payload.syncId}")
                }
                Unit
            }
            AlarmSyncOperation.SNOOZE -> {
                Log.i(TAG, "applyRemote: SNOOZE for syncId=${payload.syncId}")
                alarmCommand(payload, AlarmService.ACTION_SNOOZE)
            }
            AlarmSyncOperation.DISMISS -> {
                Log.i(TAG, "applyRemote: DISMISS for syncId=${payload.syncId}")
                alarmCommand(payload, AlarmService.ACTION_DISMISS)
            }
            AlarmSyncOperation.DELETE -> Unit // handled above before the version gate
        }
    }

    suspend fun applyWatchSnapshot(entries: List<AlarmSyncPayload>, snapshotTimestamp: Long): Result<Unit> = runCatching {
        // Snapshots are reconciliation hints only. They intentionally cannot imply DELETE:
        // a stale snapshot may omit a newer alarm that was created or edited on this device.
        // Persistent CREATE/UPDATE/DELETE mutations are authoritative.
        entries.forEach { applyRemote(it).getOrThrow() }
    }

    suspend fun sendWearAction(alarmId: Long, operation: AlarmSyncOperation): Result<Unit> = runCatching {
        require(operation == AlarmSyncOperation.SNOOZE || operation == AlarmSyncOperation.DISMISS || operation == AlarmSyncOperation.ENABLE || operation == AlarmSyncOperation.DISABLE)
        sendWearMutation(alarmId, operation)
        when (operation) {
            AlarmSyncOperation.SNOOZE -> alarmCommandForAlarm(alarmId, AlarmService.ACTION_SNOOZE)
            AlarmSyncOperation.DISMISS -> alarmCommandForAlarm(alarmId, AlarmService.ACTION_DISMISS)
            AlarmSyncOperation.ENABLE, AlarmSyncOperation.DISABLE -> {
                alarmRepository.setEnabled(alarmId, operation == AlarmSyncOperation.ENABLE, 0L)
                alarmRepository.getById(alarmId)?.let { alarmScheduler.schedule(it, requestWidgetUpdate = true) }
                if (operation == AlarmSyncOperation.DISABLE) alarmCommandForAlarm(alarmId, AlarmService.ACTION_DISMISS)
            }
            else -> Unit
        }
    }

    /** Sends a local phone action to Wear without applying it again on the phone. */
    suspend fun notifyWearAction(alarmId: Long, operation: AlarmSyncOperation): Result<Unit> = runCatching {
        require(
            operation == AlarmSyncOperation.RINGING ||
                operation == AlarmSyncOperation.SNOOZE ||
                operation == AlarmSyncOperation.DISMISS
        )
        sendWearMutation(alarmId, operation)
    }

    private suspend fun sendWearMutation(alarmId: Long, operation: AlarmSyncOperation) {
        val syncId = preferences.getString(syncIdKey(alarmId), null) ?: error("Alarm $alarmId is not synchronized yet")
        val revision = nextRevision(syncId)
        val alarm = alarmRepository.getById(alarmId) ?: error("Alarm not found")
        val payload = AlarmSyncCodec.create(alarm, syncId, operation, AlarmSyncSource.WATCH, revision, originDeviceId = deviceId)
        val envelope = AlarmSyncEnvelope(deviceId = deviceId, syncId = syncId, alarmId = alarmId, operation = operation,
            source = AlarmSyncSource.WATCH, revision = revision, timestamp = payload.timestamp, payload = AlarmSyncCodec.encode(payload))
        transportProvider.transport().send(envelope).getOrThrow()
        rememberVersion(syncId, revision, payload.timestamp, AlarmSyncSource.WATCH, deviceId)
    }

    /** Local changes are the only source of persistent outbound mutations. We deliberately do not
     * publish a full phone snapshot here: that old behaviour resurrected alarms deleted on Wear. */
    private suspend fun synchronizeSnapshot(alarms: List<Alarm>) {
        val currentIds = alarms.map { it.id }.filter { it != 0L }.toSet()
        val previousIds = preferences.getStringSet(KEY_KNOWN_ALARM_IDS, emptySet()).orEmpty().mapNotNull { it.toLongOrNull() }.toSet()
        for (alarm in alarms) {
            if (alarm.id == 0L) continue
            val syncId = ensureSyncId(alarm.id)
            val currentToken = AlarmSyncCodec.create(alarm, syncId, AlarmSyncOperation.UPDATE, AlarmSyncSource.PHONE, 0L, originDeviceId = deviceId).alarmToken ?: continue
            val tokenHash = hash(currentToken)
            val suppressed = remoteSuppressions.remove(alarm.id)
            if (suppressed == tokenHash) {
                preferences.edit().putString(tokenKey(alarm.id), tokenHash).apply()
                Log.i(TAG, "synchronizeSnapshot: SUPPRESSED echo for alarmId=${alarm.id} syncId=$syncId (remote suppression match)")
                continue
            }
            if (preferences.getString(tokenKey(alarm.id), null) == tokenHash) {
                Log.d(TAG, "synchronizeSnapshot: SKIP unchanged alarmId=${alarm.id} syncId=$syncId")
                continue
            }
            val operation = if (preferences.getString(tokenKey(alarm.id), null) == null) AlarmSyncOperation.CREATE else AlarmSyncOperation.UPDATE
            val revision = nextRevision(syncId)
            val payload = AlarmSyncCodec.create(alarm, syncId, operation, AlarmSyncSource.PHONE, revision, originDeviceId = deviceId)
            val envelope = AlarmSyncEnvelope(deviceId = deviceId, syncId = syncId, alarmId = alarm.id, operation = operation,
                source = AlarmSyncSource.PHONE, revision = revision, timestamp = payload.timestamp, payload = AlarmSyncCodec.encode(payload))
            Log.i(TAG, "synchronizeSnapshot: PUBLISHING $operation for alarmId=${alarm.id} syncId=$syncId rev=$revision " +
                "suppressed=${if (suppressed != null) "was:${suppressed.take(16)}" else "none"} storedHash=${preferences.getString(tokenKey(alarm.id), null)?.take(16)} newHash=${tokenHash.take(16)}")
            val result = transportProvider.transport().send(envelope)
            if (result.isSuccess) {
                preferences.edit().putString(tokenKey(alarm.id), payload.alarmToken?.let(::hash) ?: tokenHash).apply()
                rememberVersion(syncId, revision, payload.timestamp, AlarmSyncSource.PHONE, deviceId)
            } else {
                Log.e(TAG, "Send failed for $operation $syncId: ${result.exceptionOrNull()?.message}")
            }
        }
        for (alarmId in previousIds - currentIds) {
            val syncId = preferences.getString(syncIdKey(alarmId), null) ?: continue
            val revision = nextRevision(syncId); val timestamp = System.currentTimeMillis()
            val envelope = AlarmSyncEnvelope(deviceId = deviceId, syncId = syncId, alarmId = alarmId, operation = AlarmSyncOperation.DELETE,
                source = AlarmSyncSource.PHONE, revision = revision, timestamp = timestamp, payload = null)
            if (transportProvider.transport().send(envelope).isSuccess) {
                rememberVersion(syncId, revision, timestamp, AlarmSyncSource.PHONE, deviceId)
                preferences.edit().remove(tokenKey(alarmId)).apply()
            }
        }
        preferences.edit().putStringSet(KEY_KNOWN_ALARM_IDS, currentIds.map(Long::toString).toSet()).apply()
    }

    /**
     * A snapshot request is a recovery operation, not an incremental sync.
     * Always publish every local alarm so a restarted or cleared watch can
     * rebuild its list even when the phone-side token cache is unchanged.
     */
    private suspend fun publishFullSnapshot(alarms: List<Alarm>) {
        val entries = alarms.mapNotNull { alarm ->
            if (alarm.id == 0L) return@mapNotNull null
            val syncId = ensureSyncId(alarm.id)
            val version = localVersion(syncId)
            AlarmSyncSnapshotEntry(
                alarm = alarm,
                syncId = syncId,
                revision = version?.revision ?: 0L,
                updatedAt = version?.timestamp ?: alarm.createdAt,
                source = AlarmSyncSource.PHONE,
                originDeviceId = deviceId
            )
        }
        transportProvider.transport().publishFullSnapshot(entries)
            .onSuccess { Log.i(TAG, "publishFullSnapshot: sent ${entries.size} alarms to Wear") }
            .onFailure { Log.e(TAG, "publishFullSnapshot: failed for ${entries.size} alarms", it) }
    }

    private fun alarmCommand(payload: AlarmSyncPayload, action: String) {
        val id = preferences.getLong(alarmIdKey(payload.syncId), 0L); if (id == 0L) return
        alarmCommandForAlarm(id, action, payload.timestamp)
    }

    private fun alarmCommandForAlarm(id: Long, action: String, scheduledAt: Long = System.currentTimeMillis()) {
        context.startService(Intent(context, AlarmService::class.java).apply {
            this.action = action
            putExtra(AlarmScheduler.EXTRA_ALARM_ID, id)
            putExtra(AlarmScheduler.EXTRA_SCHEDULED_AT, scheduledAt)
        })
    }

    private fun alarmFromPayload(payload: AlarmSyncPayload): Alarm {
        val days = payload.repeatDays.mapNotNull { day ->
            java.time.DayOfWeek.entries.getOrNull(day - 1)
        }.toSet()
        return Alarm(
            hour = (payload.hour ?: 7).coerceIn(0, 23),
            minute = (payload.minute ?: 0).coerceIn(0, 59),
            label = (payload.label ?: "Alarm").take(120),
            isEnabled = payload.enabled ?: true,
            repeatDays = days,
            vibrationEnabled = payload.vibrationEnabled ?: true,
            volume = (payload.volume ?: 100).coerceIn(0, 100),
            snoozeDurationMinutes = (payload.snoozeDurationMinutes ?: 10).coerceIn(1, 60)
        ).sanitized()
    }

    /** Who made the last synchronized change to this alarm and when. Null if never synced. */
    fun lastChangeFor(alarmId: Long): AlarmLastChange? {
        val syncId = preferences.getString(syncIdKey(alarmId), null) ?: return null
        val timestamp = preferences.getLong(timestampKey(syncId), 0L); if (timestamp == 0L) return null
        val source = preferences.getString(sourceKey(syncId), null) ?: return null
        val parsed = runCatching { AlarmSyncSource.valueOf(source) }.getOrNull() ?: return null
        return AlarmLastChange(fromWatch = parsed == AlarmSyncSource.WATCH, timestamp = timestamp)
    }

    private fun ensureSyncId(alarmId: Long) = preferences.getString(syncIdKey(alarmId), null)
        ?: UUID.randomUUID().toString().also { preferences.edit().putString(syncIdKey(alarmId), it).apply() }
    private fun rememberIdentity(syncId: String, alarmId: Long, revision: Long, timestamp: Long, token: String?, source: AlarmSyncSource, originDeviceId: String) {
        preferences.edit().putLong(alarmIdKey(syncId), alarmId).putString(syncIdKey(alarmId), syncId).apply()
        rememberVersion(syncId, revision, timestamp, source, originDeviceId); token?.let { preferences.edit().putString(tokenKey(alarmId), hash(it)).apply() }
    }
    private fun rememberVersion(syncId: String, revision: Long, timestamp: Long, source: AlarmSyncSource, originDeviceId: String) {
        preferences.edit().putLong(revisionKey(syncId), revision).putLong(timestampKey(syncId), timestamp).putString(sourceKey(syncId), source.name).putString(originDeviceKey(syncId), originDeviceId).apply()
    }
    private fun nextRevision(syncId: String): Long = preferences.getLong(revisionKey(syncId), 0L) + 1L
    private fun localVersion(syncId: String): AlarmSyncEnvelope? {
        val revision = preferences.getLong(revisionKey(syncId), 0L); val timestamp = preferences.getLong(timestampKey(syncId), 0L)
        if (revision == 0L && timestamp == 0L) return null
        return AlarmSyncEnvelope(deviceId = preferences.getString(originDeviceKey(syncId), "") ?: "", syncId = syncId,
            alarmId = preferences.getLong(alarmIdKey(syncId), 0L), operation = AlarmSyncOperation.UPDATE,
            source = runCatching { AlarmSyncSource.valueOf(preferences.getString(sourceKey(syncId), AlarmSyncSource.PHONE.name)!!) }.getOrDefault(AlarmSyncSource.PHONE),
            revision = revision, timestamp = timestamp)
    }
    private fun hash(value: String) = MessageDigest.getInstance("SHA-256").digest(value.toByteArray(Charsets.UTF_8)).joinToString("") { "%02x".format(it) }
    private fun syncIdKey(id: Long) = "sync_id_$id"
    private fun alarmIdKey(id: String) = "alarm_id_$id"
    private fun tokenKey(id: Long) = "token_$id"
    private fun revisionKey(id: String) = "revision_$id"
    private fun timestampKey(id: String) = "timestamp_$id"
    private fun sourceKey(id: String) = "source_$id"
    private fun originDeviceKey(id: String) = "origin_device_$id"
    companion object {
        private const val TAG = "AlarmSync"
        private const val PREFS_NAME = "wakesync_state"
        private const val KEY_DEVICE_ID = "device_id"
        private const val KEY_KNOWN_ALARM_IDS = "known_alarm_ids"
    }
}
