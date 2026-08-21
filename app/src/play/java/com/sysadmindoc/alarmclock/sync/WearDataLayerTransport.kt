package com.sysadmindoc.alarmclock.sync

import android.content.Context
import com.google.android.gms.tasks.Task
import com.google.android.gms.wearable.Node
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

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

        val nodes = awaitTask<List<Node>> {
            Wearable.getNodeClient(appContext).connectedNodes
        }
        require(nodes.isNotEmpty()) { "No connected Wear OS node" }

        nodes.forEach { node ->
            awaitTask<Int> {
                Wearable.getMessageClient(appContext)
                    .sendMessage(node.id, AlarmSyncTransportPaths.ALARM_MUTATION, payload)
            }
        }
    }

    private suspend fun <T> awaitTask(factory: () -> Task<T>): T =
        suspendCancellableCoroutine { continuation ->
            factory()
                .addOnSuccessListener { value ->
                    if (continuation.isActive) continuation.resume(value)
                }
                .addOnFailureListener { error ->
                    if (continuation.isActive) continuation.resumeWith(Result.failure(error))
                }
        }
}
