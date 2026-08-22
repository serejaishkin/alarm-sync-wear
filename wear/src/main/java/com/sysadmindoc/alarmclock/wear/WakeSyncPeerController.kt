package com.sysadmindoc.alarmclock.wear

import android.content.Context
import com.google.android.gms.wearable.Wearable
import org.json.JSONObject

/**
 * Wear-side peer operations. The watch can mutate an existing logical alarm;
 * the phone remains only the transport/persistence peer, never the UI owner.
 */
object WakeSyncPeerController {
    const val PATH_MUTATION = "/wakesync/alarm/mutation"

    fun sendMutation(context: Context, operation: String, syncId: String, alarmToken: String) {
        val payload = JSONObject()
            .put("operation", operation)
            .put("syncId", syncId)
            .put("alarmToken", alarmToken)
            .toString()
            .toByteArray(Charsets.UTF_8)
        Wearable.getNodeClient(context).connectedNodes.addOnSuccessListener { nodes ->
            nodes.forEach { node ->
                Wearable.getMessageClient(context).sendMessage(node.id, PATH_MUTATION, payload)
            }
        }
    }

    fun sendDelete(context: Context, syncId: String) =
        sendMutation(context, "DELETE", syncId, "")

    fun sendEnable(context: Context, syncId: String, alarmToken: String) =
        sendMutation(context, "ENABLE", syncId, alarmToken)

    fun sendDisable(context: Context, syncId: String, alarmToken: String) =
        sendMutation(context, "DISABLE", syncId, alarmToken)
}
