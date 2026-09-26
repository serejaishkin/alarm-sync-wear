package com.wakesync.app.sync

import android.content.Context
import android.util.Log
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.tasks.OnFailureListener
import com.google.android.gms.tasks.OnSuccessListener
import com.google.android.gms.wearable.Node
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.PutDataRequest
import com.google.android.gms.wearable.Wearable
import com.wakesync.app.data.model.Alarm
import kotlinx.coroutines.suspendCancellableCoroutine
import org.json.JSONArray
import org.json.JSONObject
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/** Play-flavor transport using the Wear OS Data Layer. */
class WearDataLayerTransport(context: Context) : AlarmSyncTransport {
    private val appContext = context.applicationContext

    init {
        val gmsStatus = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(appContext)
        if (gmsStatus != ConnectionResult.SUCCESS) {
            Log.e(TAG, "Google Play Services unavailable! status=$gmsStatus " +
                "(${GoogleApiAvailability.getInstance().getErrorString(gmsStatus)})")
        } else {
            Log.i(TAG, "Google Play Services available")
        }
        Wearable.getNodeClient(appContext).connectedNodes
            .addOnSuccessListener { nodes ->
                if (nodes.isEmpty()) {
                    Log.w(TAG, "No connected Wear OS nodes — Data Layer will not deliver")
                } else {
                    nodes.forEach { node ->
                        Log.i(TAG, "Connected node: ${node.displayName} id=${node.id}")
                    }
                }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Failed to list connected nodes: ${e.message}", e)
            }
    }

    override suspend fun send(envelope: AlarmSyncEnvelope): Result<Unit> = runCatching {
        val payload = envelope.payload ?: ""
        Log.d(TAG, "Sending ${envelope.operation} for ${envelope.syncId} rev=${envelope.revision}")
        // MessageClient wakes the Wear listener immediately. Keep the
        // DataClient write below as the durable retry/recovery path.
        awaitConnectedNodes().forEach { node ->
            runCatching {
                awaitSendMessage(node.id, payload.toByteArray(Charsets.UTF_8))
            }.onFailure { e ->
                Log.w(TAG, "MessageClient send failed for ${node.id}: ${e.message}")
            }
        }
        val request = PutDataMapRequest.create("$PATH_ALARM_STATE/${envelope.syncId}").apply {
            dataMap.putString(KEY_MUTATION, payload)
            dataMap.putLong(KEY_REVISION, envelope.revision)
            dataMap.putLong(KEY_TIMESTAMP, envelope.timestamp)
            dataMap.putString(KEY_OPERATION, envelope.operation.name)
            dataMap.putString(KEY_SOURCE, envelope.source.name)
            dataMap.putString(KEY_DEVICE_ID, envelope.deviceId)
        }.asPutDataRequest().setUrgent()
        awaitPutDataItem(request)
    }

    override fun requestWatchSnapshot(): Result<Unit> = runCatching {
        Wearable.getNodeClient(appContext).connectedNodes
            .addOnSuccessListener { nodes ->
                nodes.forEach { node ->
                    Wearable.getMessageClient(appContext)
                        .sendMessage(node.id, PATH_REQUEST_WATCH_SNAPSHOT, ByteArray(0))
                        .addOnFailureListener { e ->
                            Log.w(TAG, "requestWatchSnapshot failed for ${node.id}: ${e.message}")
                        }
                }
                Log.i(TAG, "Requested Watch snapshot from ${nodes.size} node(s)")
            }
            .addOnFailureListener { e ->
                Log.w(TAG, "Unable to find Wear nodes for snapshot request", e)
            }
    }

    override suspend fun publishSnapshot(alarms: List<Alarm>): Result<Unit> = publishDataItem(
        alarms.filter { it.id != 0L }.map {
            AlarmSyncSnapshotEntry(it, "snapshot-${it.id}", 0L, System.currentTimeMillis(), AlarmSyncSource.PHONE, "")
        }
    )

    override suspend fun publishFullSnapshot(entries: List<AlarmSyncSnapshotEntry>): Result<Unit> = publishDataItem(entries)

    private suspend fun publishDataItem(entries: List<AlarmSyncSnapshotEntry>): Result<Unit> = runCatching {
        val alarms = entries.map { it.alarm }
        val alarm = alarms.filter { it.isEnabled && it.nextTriggerTime > 0L }.minByOrNull { it.nextTriggerTime }
        val list = JSONArray()
        entries.forEach { entry ->
            val item = entry.alarm
            list.put(JSONObject()
                .put("syncId", entry.syncId).put("operation", "UPDATE").put("source", entry.source.name)
                .put("originDeviceId", entry.originDeviceId).put("revision", entry.revision).put("timestamp", entry.updatedAt)
                .put("label", item.label).put("hour", item.hour).put("minute", item.minute).put("enabled", item.isEnabled)
                .put("repeatDays", JSONArray().also { days -> item.repeatDays.map { it.value }.sorted().forEach(days::put) })
                .put("snoozeDurationMinutes", item.snoozeDurationMinutes).put("vibrationEnabled", item.vibrationEnabled)
                .put("volume", item.volume).put("alarmToken", com.wakesync.app.data.share.AlarmShareCodec.encodeToken(item)))
        }
        val snapshotTimestamp = System.currentTimeMillis()
        val request = PutDataMapRequest.create(PATH_ALARM_SNAPSHOT).apply {
            dataMap.putLong(KEY_UPDATED_AT, snapshotTimestamp)
            dataMap.putBoolean(KEY_HAS_ALARM, alarm != null)
            dataMap.putLong(KEY_ALARM_ID, alarm?.id ?: -1L)
            dataMap.putString(KEY_LABEL, alarm?.label.orEmpty())
            dataMap.putString(KEY_TIME_LABEL, alarm?.time?.toString().orEmpty())
            dataMap.putLong(KEY_TRIGGER_TIME, alarm?.nextTriggerTime ?: 0L)
            dataMap.putString(KEY_TIMEZONE_POLICY, alarm?.timezonePolicy ?: "LOCAL")
            dataMap.putString(KEY_FIXED_TIMEZONE_ID, alarm?.fixedTimezoneId.orEmpty())
            dataMap.putString(KEY_ALARM_LIST, list.toString())
        }
        awaitPutDataItem(request.asPutDataRequest().setUrgent())
    }

    private suspend fun awaitConnectedNodes(): List<Node> = suspendCancellableCoroutine { c ->
        Wearable.getNodeClient(appContext).connectedNodes
            .addOnSuccessListener(OnSuccessListener { nodes -> if (c.isActive) c.resume(nodes) })
            .addOnFailureListener(OnFailureListener { e -> if (c.isActive) c.resumeWithException(e) })
    }

    private suspend fun awaitPutDataItem(request: PutDataRequest) = suspendCancellableCoroutine<com.google.android.gms.wearable.DataItem> { c ->
        Wearable.getDataClient(appContext).putDataItem(request)
            .addOnSuccessListener(OnSuccessListener { item -> if (c.isActive) c.resume(item) })
            .addOnFailureListener(OnFailureListener { e ->
                Log.e(TAG, "putDataItem failed: ${e.message}", e)
                if (c.isActive) c.resumeWithException(e)
            })
    }

    private suspend fun awaitSendMessage(nodeId: String, payload: ByteArray) =
        suspendCancellableCoroutine<Int> { c ->
            Wearable.getMessageClient(appContext)
                .sendMessage(nodeId, PATH_MUTATION, payload)
                .addOnSuccessListener { result -> if (c.isActive) c.resume(result) }
                .addOnFailureListener { error -> if (c.isActive) c.resumeWithException(error) }
        }

    companion object {
        private const val TAG = "WearDataLayer"
        const val PATH_ALARM_STATE = "/wakesync/alarm/state"
        const val PATH_MUTATION = "/wakesync/alarm/mutation"
        const val PATH_ALARM_SNAPSHOT = "/alarms/next"
        const val PATH_REQUEST_WATCH_SNAPSHOT = "/wakesync/alarm/request_watch_snapshot"
        const val KEY_MUTATION = "mutation"
        const val KEY_REVISION = "revision"
        const val KEY_TIMESTAMP = "timestamp"
        const val KEY_OPERATION = "operation"
        const val KEY_SOURCE = "source"
        const val KEY_DEVICE_ID = "device_id"
        const val KEY_ALARM_LIST = "alarm_list"
        private const val KEY_HAS_ALARM = "has_alarm"
        private const val KEY_ALARM_ID = "alarm_id"
        private const val KEY_LABEL = "label"
        private const val KEY_TIME_LABEL = "time_label"
        private const val KEY_TRIGGER_TIME = "trigger_time"
        private const val KEY_UPDATED_AT = "updated_at"
        private const val KEY_TIMEZONE_POLICY = "timezone_policy"
        private const val KEY_FIXED_TIMEZONE_ID = "fixed_timezone_id"
    }
}
