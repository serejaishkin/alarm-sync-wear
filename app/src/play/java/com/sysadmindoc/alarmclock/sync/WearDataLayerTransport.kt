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
class WearDataLayerTransport(context: Context) : AlarmSyncTransport {
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

    override suspend fun publishSnapshot(alarms: List<Alarm>): Result<Unit> = runCatching {
        val request = PutDataMapRequest.create(PATH_ALARM_SNAPSHOT).apply {
            dataMap.putLong(KEY_UPDATED_AT, System.currentTimeMillis())
            dataMap.putString(KEY_SNAPSHOT, AlarmSyncCodec.encodeSnapshot(alarms))
        }
        awaitPutDataItem(request.asPutDataRequest().setUrgent())
    }

    private suspend fun awaitConnectedNodes(): List<Node> = suspendCancellableCoroutine { c ->
        Wearable.getNodeClient(appContext).connectedNodes
            .addOnSuccessListener(OnSuccessListener { nodes -> if (c.isActive) c.resume(nodes) })
            .addOnFailureListener(OnFailureListener { e -> if (c.isActive) c.resumeWithException(e) })
    }

    private suspend fun awaitSendMessage(node: Node, payload: ByteArray): Int = suspendCancellableCoroutine { c ->
        Wearable.getMessageClient(appContext).sendMessage(node.id, ALARM_MUTATION_PATH, payload)
            .addOnSuccessListener(OnSuccessListener { result -> if (c.isActive) c.resume(result) })
            .addOnFailureListener(OnFailureListener { e -> if (c.isActive) c.resumeWithException(e) })
    }

    private suspend fun awaitPutDataItem(request: com.google.android.gms.wearable.PutDataRequest) =
        suspendCancellableCoroutine<com.google.android.gms.wearable.DataItem> { c ->
            Wearable.getDataClient(appContext).putDataItem(request)
                .addOnSuccessListener(OnSuccessListener { item -> if (c.isActive) c.resume(item) })
                .addOnFailureListener(OnFailureListener { e -> if (c.isActive) c.resumeWithException(e) })
        }

    companion object {
        const val ALARM_MUTATION_PATH = "/wakesync/alarm/mutation"
        const val PATH_ALARM_SNAPSHOT = "/wakesync/alarm/snapshot"
        private const val KEY_UPDATED_AT = "updated_at"
        private const val KEY_SNAPSHOT = "snapshot"
    }
}
