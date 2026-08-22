package com.sysadmindoc.alarmclock.sync

import android.content.Context
import com.google.android.gms.tasks.OnFailureListener
import com.google.android.gms.tasks.OnSuccessListener
import com.google.android.gms.wearable.Node
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.Wearable
import com.sysadmindoc.alarmclock.data.model.Alarm
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/** Play-flavor transport using the Wear OS Data Layer. */
class WearDataLayerTransport(
    context: Context
) : AlarmSyncTransport {
    private val appContext = context.applicationContext

    override suspend fun send(envelope: AlarmSyncEnvelope): Result<Unit> = runCatching {
        val payload = AlarmSyncCodec.encode(
            AlarmSyncPayload(
                protocolVersion = envelope.protocolVersion,
                syncId = envelope.syncId,
                operation = envelope.operation,
                source = envelope.source,
                revision = envelope.revision,
                timestamp = envelope.timestamp,
                alarmToken = envelope.payload
            )
        ).toByteArray(Charsets.UTF_8)
        val nodes = awaitConnectedNodes()
        require(nodes.isNotEmpty()) { "No connected Wear OS node" }
        nodes.forEach { node -> awaitSendMessage(node, payload) }
    }

    override suspend fun publishSnapshot(alarm: Alarm?): Result<Unit> = runCatching {
        val request = PutDataMapRequest.create(PATH_NEXT_ALARM).apply {
            dataMap.putBoolean(KEY_HAS_ALARM, alarm != null)
            dataMap.putLong(KEY_ALARM_ID, alarm?.id ?: -1L)
            dataMap.putString(KEY_LABEL, alarm?.label.orEmpty())
            dataMap.putString(KEY_TIME_LABEL, alarm?.let { "%02d:%02d".format(it.hour, it.minute) }.orEmpty())
            dataMap.putLong(KEY_TRIGGER_TIME, alarm?.nextTriggerTime ?: 0L)
            dataMap.putBoolean(KEY_IS_FIRING, false)
            dataMap.putLong(KEY_UPDATED_AT, System.currentTimeMillis())
            dataMap.putString(KEY_TIMEZONE_POLICY, alarm?.timezonePolicy ?: "LOCAL")
            dataMap.putString(KEY_FIXED_TIMEZONE_ID, alarm?.fixedTimezoneId.orEmpty())
        }
        awaitPutDataItem(request.asPutDataRequest().setUrgent())
    }

    private suspend fun awaitConnectedNodes(): List<Node> =
        suspendCancellableCoroutine<List<Node>> { continuation ->
            Wearable.getNodeClient(appContext).connectedNodes
                .addOnSuccessListener(OnSuccessListener { nodes ->
                    if (continuation.isActive) continuation.resume(nodes)
                })
                .addOnFailureListener(OnFailureListener { error ->
                    if (continuation.isActive) continuation.resumeWithException(error)
                })
        }

    private suspend fun awaitSendMessage(node: Node, payload: ByteArray): Int =
        suspendCancellableCoroutine<Int> { continuation ->
            Wearable.getMessageClient(appContext)
                .sendMessage(node.id, ALARM_MUTATION_PATH, payload)
                .addOnSuccessListener(OnSuccessListener { result ->
                    if (continuation.isActive) continuation.resume(result)
                })
                .addOnFailureListener(OnFailureListener { error ->
                    if (continuation.isActive) continuation.resumeWithException(error)
                })
        }

    private suspend fun awaitPutDataItem(
        request: com.google.android.gms.wearable.PutDataRequest
    ): com.google.android.gms.wearable.DataItem =
        suspendCancellableCoroutine<com.google.android.gms.wearable.DataItem> { continuation ->
            Wearable.getDataClient(appContext)
                .putDataItem(request)
                .addOnSuccessListener(OnSuccessListener { item ->
                    if (continuation.isActive) continuation.resume(item)
                })
                .addOnFailureListener(OnFailureListener { error ->
                    if (continuation.isActive) continuation.resumeWithException(error)
                })
        }

    companion object {
        const val ALARM_MUTATION_PATH = "/wakesync/alarm/mutation"
        const val PATH_NEXT_ALARM = "/alarmclockxtreme/next_alarm"
        private const val KEY_HAS_ALARM = "has_alarm"
        private const val KEY_ALARM_ID = "alarm_id"
        private const val KEY_LABEL = "label"
        private const val KEY_TIME_LABEL = "time_label"
        private const val KEY_TRIGGER_TIME = "trigger_time"
        private const val KEY_IS_FIRING = "is_firing"
        private const val KEY_UPDATED_AT = "updated_at"
        private const val KEY_TIMEZONE_POLICY = "timezone_policy"
        private const val KEY_FIXED_TIMEZONE_ID = "fixed_timezone_id"
    }
}
