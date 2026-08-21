package com.sysadmindoc.alarmclock.sync

import android.content.Context
import com.google.android.gms.tasks.OnFailureListener
import com.google.android.gms.tasks.OnSuccessListener
import com.google.android.gms.wearable.Node
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Play-flavor transport using the Wear OS Data Layer message API.
 *
 * F-Droid does not compile this source set, so the core sync layer remains free
 * of the proprietary Google Play Services dependency.
 */
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

        nodes.forEach { node ->
            awaitSendMessage(node, payload)
        }
    }

    private suspend fun awaitConnectedNodes(): List<Node> =
        suspendCancellableCoroutine { continuation ->
            Wearable.getNodeClient(appContext).connectedNodes
                .addOnSuccessListener(OnSuccessListener { nodes ->
                    if (continuation.isActive) continuation.resume(nodes)
                })
                .addOnFailureListener(OnFailureListener { error ->
                    if (continuation.isActive) continuation.resumeWithException(error)
                })
        }

    private suspend fun awaitSendMessage(node: Node, payload: ByteArray): Int =
        suspendCancellableCoroutine { continuation ->
            Wearable.getMessageClient(appContext)
                .sendMessage(node.id, AlarmSyncTransportPaths.ALARM_MUTATION, payload)
                .addOnSuccessListener(OnSuccessListener { result ->
                    if (continuation.isActive) continuation.resume(result)
                })
                .addOnFailureListener(OnFailureListener { error ->
                    if (continuation.isActive) continuation.resumeWithException(error)
                })
        }
}
