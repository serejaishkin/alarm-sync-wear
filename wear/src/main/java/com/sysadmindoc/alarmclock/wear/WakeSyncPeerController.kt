package com.sysadmindoc.alarmclock.wear

import android.content.Context
import com.google.android.gms.wearable.Wearable
import org.json.JSONArray
import org.json.JSONObject

/**
 * Wear-side mutation gateway.
 *
 * The watch always persists the change locally first. The same full alarm
 * snapshot is then sent to the phone through the WakeSync Data Layer service.
 * The phone can therefore apply CREATE/UPDATE/DELETE/ENABLE/DISABLE without
 * depending on the editor Activity staying alive.
 */
object WakeSyncPeerController {
    const val PATH_MUTATION = "/wakesync/alarm/mutation"
    private const val PROTOCOL_VERSION = 1

    fun sendAlarmMutation(context: Context, entry: WearAlarmListStore.Entry, operation: String) {
        val timestamp = System.currentTimeMillis()
        val payload = JSONObject()
            .put("protocolVersion", PROTOCOL_VERSION)
            .put("syncId", entry.syncId)
            .put("operation", operation)
            .put("source", "WATCH")
            .put("revision", entry.revision)
            .put("timestamp", timestamp)
            .put("hour", entry.hour)
            .put("minute", entry.minute)
            .put("label", entry.label)
            .put("enabled", entry.enabled)
            .put("repeatDays", JSONArray().also { days -> entry.repeatDays.sorted().forEach(days::put) })
            .put("snoozeDurationMinutes", entry.snoozeDurationMinutes)
            .put("vibrationEnabled", entry.vibrationEnabled)
            .put("volume", entry.volume)
            .putOpt("alarmToken", entry.alarmToken.takeIf { it.isNotBlank() })
            .toString()
            .toByteArray(Charsets.UTF_8)

        Wearable.getNodeClient(context).connectedNodes.addOnSuccessListener { nodes ->
            nodes.forEach { node ->
                Wearable.getMessageClient(context).sendMessage(node.id, PATH_MUTATION, payload)
            }
        }
    }

    fun sendDelete(context: Context, syncId: String, revision: Long) {
        val timestamp = System.currentTimeMillis()
        val payload = JSONObject()
            .put("protocolVersion", PROTOCOL_VERSION)
            .put("syncId", syncId)
            .put("operation", "DELETE")
            .put("source", "WATCH")
            .put("revision", revision)
            .put("timestamp", timestamp)
            .toString()
            .toByteArray(Charsets.UTF_8)

        Wearable.getNodeClient(context).connectedNodes.addOnSuccessListener { nodes ->
            nodes.forEach { node ->
                Wearable.getMessageClient(context).sendMessage(node.id, PATH_MUTATION, payload)
            }
        }
    }

    fun sendMutation(context: Context, operation: String, syncId: String, alarmToken: String?) {
        val current = WearAlarmListStore.load(context).firstOrNull { it.syncId == syncId } ?: return
        val revision = current.revision + 1L
        val updated = current.copy(
            enabled = when (operation) {
                "ENABLE" -> true
                "DISABLE" -> false
                else -> current.enabled
            },
            revision = revision,
            updatedAt = System.currentTimeMillis(),
            alarmToken = alarmToken ?: current.alarmToken
        )
        WearAlarmListStore.upsert(context, updated)
        sendAlarmMutation(context, updated, operation)
    }

    fun sendDelete(context: Context, syncId: String) {
        val current = WearAlarmListStore.load(context).firstOrNull { it.syncId == syncId } ?: return
        val revision = current.revision + 1L
        sendDelete(context, syncId, revision)
        WearAlarmListStore.remove(context, syncId)
    }

    fun sendEnable(context: Context, syncId: String, alarmToken: String) =
        sendMutation(context, "ENABLE", syncId, alarmToken)

    fun sendDisable(context: Context, syncId: String, alarmToken: String) =
        sendMutation(context, "DISABLE", syncId, alarmToken)
}
