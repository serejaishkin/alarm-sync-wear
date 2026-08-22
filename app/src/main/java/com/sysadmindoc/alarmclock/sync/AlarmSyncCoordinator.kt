package com.sysadmindoc.alarmclock.sync

import android.content.Context
import com.sysadmindoc.alarmclock.data.model.Alarm
import com.sysadmindoc.alarmclock.data.repository.AlarmRepository
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

/**
 * Owns the phone-side synchronization lifecycle.
 *
 * Alarm.id remains local. A stable syncId is persisted in private preferences
 * and follows the logical alarm across local Room changes and app restarts.
 */
@Singleton
class AlarmSyncCoordinator @Inject constructor(
    private val context: Context,
    private val alarmRepository: AlarmRepository,
    private val transportProvider: AlarmSyncTransportProvider
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val remoteSuppressions = mutableMapOf<Long, String>()
    private var observationJob: Job? = null

    private val deviceId: String
        get() = preferences.getString(KEY_DEVICE_ID, null) ?: UUID.randomUUID().toString().also {
            preferences.edit().putString(KEY_DEVICE_ID, it).apply()
        }

    fun start() {
        if (observationJob?.isActive == true) return
        observationJob = scope.launch {
            alarmRepository.observeAll().collectLatest { alarms ->
                synchronizeSnapshot(alarms)
            }
        }
    }

    fun stop() {
        observationJob?.cancel()
        observationJob = null
    }

    /** Apply one mutation received from the Wear companion. */
    suspend fun applyRemote(payload: AlarmSyncPayload): Result<Unit> = runCatching {
        val currentRevision = preferences.getLong(revisionKey(payload.syncId), 0L)
        if (payload.revision < currentRevision) return@runCatching
        if (payload.revision == currentRevision && payload.timestamp <= preferences.getLong(timestampKey(payload.syncId), 0L)) {
            return@runCatching
        }

        when (payload.operation) {
            AlarmSyncOperation.DELETE -> {
                val alarmId = preferences.getLong(alarmIdKey(payload.syncId), 0L)
                if (alarmId != 0L) alarmRepository.deleteById(alarmId)
                forgetIdentity(payload.syncId, alarmId)
            }

            AlarmSyncOperation.CREATE,
            AlarmSyncOperation.UPDATE,
            AlarmSyncOperation.ENABLE,
            AlarmSyncOperation.DISABLE -> {
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
                    val prepared = incoming.copy(
                        id = 0L,
                        isEnabled = when (payload.operation) {
                            AlarmSyncOperation.ENABLE -> true
                            AlarmSyncOperation.DISABLE -> false
                            else -> incoming.isEnabled
                        },
                        nextTriggerTime = 0L
                    ).sanitized()
                    alarmRepository.save(prepared)
                }
                rememberIdentity(payload.syncId, savedId, payload.revision, payload.timestamp, payload.alarmToken)
                if (payload.alarmToken != null) {
                    remoteSuppressions[savedId] = hash(payload.alarmToken)
                }
            }

            AlarmSyncOperation.SNOOZE,
            AlarmSyncOperation.DISMISS,
            AlarmSyncOperation.RINGING -> {
                // Firing-state commands are handled by the dedicated alarm
                // runtime layer. The phone-side data model is already synced.
            }
        }

        preferences.edit()
            .putLong(revisionKey(payload.syncId), payload.revision)
            .putLong(timestampKey(payload.syncId), payload.timestamp)
            .apply()
    }

    private suspend fun synchronizeSnapshot(alarms: List<Alarm>) {
        val currentIds = alarms.map { it.id }.filter { it != 0L }.toSet()
        val previousIds = preferences.getStringSet(KEY_KNOWN_ALARM_IDS, emptySet())
            ?.mapNotNull { it.toLongOrNull() }
            ?.toSet()
            ?: emptySet()

        for (alarm in alarms) {
            if (alarm.id == 0L) continue
            val syncId = ensureSyncId(alarm.id)
            val token = AlarmSyncCodec.create(
                alarm = alarm,
                syncId = syncId,
                operation = AlarmSyncOperation.UPDATE,
                source = AlarmSyncSource.PHONE,
                revision = nextRevision(syncId)
            ).alarmToken ?: continue
            val tokenHash = hash(token)

            if (remoteSuppressions.remove(alarm.id) == tokenHash) {
                preferences.edit().putString(tokenKey(alarm.id), tokenHash).apply()
                continue
            }

            val previousHash = preferences.getString(tokenKey(alarm.id), null)
            if (previousHash == tokenHash) continue

            val operation = if (previousHash == null) AlarmSyncOperation.CREATE else AlarmSyncOperation.UPDATE
            val revision = nextRevision(syncId)
            val payload = AlarmSyncCodec.create(
                alarm = alarm,
                syncId = syncId,
                operation = operation,
                source = AlarmSyncSource.PHONE,
                revision = revision
            )
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
                preferences.edit()
                    .putString(tokenKey(alarm.id), tokenHash)
                    .putLong(revisionKey(syncId), revision)
                    .putLong(timestampKey(syncId), payload.timestamp)
                    .apply()
            }
        }

        val deletedIds = previousIds - currentIds
        for (alarmId in deletedIds) {
            val syncId = preferences.getString(syncIdKey(alarmId), null) ?: continue
            val revision = nextRevision(syncId)
            val payload = AlarmSyncCodec.create(
                alarm = Alarm(id = alarmId),
                syncId = syncId,
                operation = AlarmSyncOperation.DELETE,
                source = AlarmSyncSource.PHONE,
                revision = revision
            )
            val envelope = AlarmSyncEnvelope(
                deviceId = deviceId,
                syncId = syncId,
                alarmId = alarmId,
                operation = AlarmSyncOperation.DELETE,
                source = AlarmSyncSource.PHONE,
                revision = revision,
                timestamp = payload.timestamp,
                payload = null
            )
            if (transportProvider.transport().send(envelope).isSuccess) forgetIdentity(syncId, alarmId)
        }

        preferences.edit().putStringSet(KEY_KNOWN_ALARM_IDS, currentIds.map(Long::toString).toSet()).apply()
    }

    private fun ensureSyncId(alarmId: Long): String {
        preferences.getString(syncIdKey(alarmId), null)?.let { return it }
        val id = UUID.randomUUID().toString()
        preferences.edit().putString(syncIdKey(alarmId), id).apply()
        return id
    }

    private fun rememberIdentity(syncId: String, alarmId: Long, revision: Long, timestamp: Long, token: String?) {
        preferences.edit()
            .putLong(alarmIdKey(syncId), alarmId)
            .putString(syncIdKey(alarmId), syncId)
            .putLong(revisionKey(syncId), revision)
            .putLong(timestampKey(syncId), timestamp)
            .apply()
        token?.let { preferences.edit().putString(tokenKey(alarmId), hash(it)).apply() }
    }

    private fun forgetIdentity(syncId: String, alarmId: Long) {
        preferences.edit()
            .remove(alarmIdKey(syncId))
            .remove(revisionKey(syncId))
            .remove(timestampKey(syncId))
            .remove(syncIdKey(alarmId))
            .remove(tokenKey(alarmId))
            .apply()
    }

    private fun nextRevision(syncId: String): Long =
        preferences.getLong(revisionKey(syncId), 0L) + 1L

    private fun hash(value: String): String = MessageDigest.getInstance("SHA-256")
        .digest(value.toByteArray(Charsets.UTF_8))
        .joinToString("") { "%02x".format(it) }

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
