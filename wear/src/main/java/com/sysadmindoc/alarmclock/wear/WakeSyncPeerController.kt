package com.sysadmindoc.alarmclock.wear

import android.content.Context
import com.google.android.gms.wearable.Wearable
import org.json.JSONObject

/** Wear-side peer mutations using the same AlarmSyncPayload wire contract as phone. */
object WakeSyncPeerController {
    const val PATH_MUTATION = "/wakesync/alarm/mutation"
    private const val PROTOCOL_VERSION = 1

    fun sendMutation(context: Context, operation: String, syncId: String, alarmToken: String?) {
        val current = WearAlarmListStore.load(context).firstOrNull { it.syncId == syncId }
        val revision = (current?.revision ?: 0L) + 1L
        val timestamp = System.currentTimeMillis()
        val payload = JSONObject()
            .put("protocolVersion", PROTOCOL_VERSION)
            .put("syncId", syncId)
            .put("operation", operation)
            .put("source", "WATCH")
            .put("revision", revision)
            .put("timestamp", timestamp)
            .putOpt("alarmToken", alarmToken)
            .toString()
            .toByteArray(Charsets.UTF_8)

        Wearable.getNodeClient(context).connectedNodes.addOnSuccessListener { nodes ->
            nodes.forEach { node ->
                Wearable.getMessageClient(context)
                    .sendMessage(node.id, PATH_MUTATION, payload)
            }
        }
    }

    fun sendDelete(context: Context, syncId: String) =
        sendMutation(context, "DELETE", syncId, null)

    fun sendEnable(context: Context, syncId: String, alarmToken: String) =
        sendMutation(context, "ENABLE", syncId, alarmToken)

    fun sendDisable(context: Context, syncId: String, alarmToken: String) =
        sendMutation(context, "DISABLE", syncId, alarmToken)
}
