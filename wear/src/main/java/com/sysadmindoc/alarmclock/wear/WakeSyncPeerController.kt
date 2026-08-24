package com.sysadmindoc.alarmclock.wear

import android.content.Context
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.Wearable
import org.json.JSONArray
import org.json.JSONObject

/** Wear-side mutation gateway. Persistent state uses DataClient; live actions use MessageClient. */
object WakeSyncPeerController {
    const val PATH_MUTATION = "/wakesync/alarm/mutation"
    const val PATH_ALARM_STATE = "/wakesync/alarm/state"
    private const val PROTOCOL_VERSION = 1

    fun sendAlarmMutation(context: Context, entry: WearAlarmListStore.Entry, operation: String) {
        val payload = buildPayload(entry, operation)
        if (operation == "SNOOZE" || operation == "DISMISS" || operation == "RINGING") {
            sendMessage(context, payload.toByteArray(Charsets.UTF_8))
        } else {
            sendDataItem(context, entry.syncId, payload)
        }
    }

    fun sendDelete(context: Context, syncId: String, revision: Long, timestamp: Long = System.currentTimeMillis()) {
        val payload = JSONObject()
            .put("protocolVersion", PROTOCOL_VERSION)
            .put("syncId", syncId)
            .put("operation", "DELETE")
            .put("source", "WATCH")
            .put("originDeviceId", deviceId(context))
            .put("revision", revision)
            .put("timestamp", timestamp)
            .toString()
        sendDataItem(context, syncId, payload)
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
            alarmToken = alarmToken ?: current.alarmToken,
            source = "WATCH",
            originDeviceId = deviceId(context)
        )
        WearAlarmListStore.upsert(context, updated)
        sendAlarmMutation(context, updated, operation)
    }

    fun sendDelete(context: Context, syncId: String) {
        val current = WearAlarmListStore.load(context).firstOrNull { it.syncId == syncId } ?: return
        val revision = current.revision + 1L
        val timestamp = System.currentTimeMillis()
        WearAlarmListStore.removeWithTombstone(context, syncId, revision, timestamp)
        sendDelete(context, syncId, revision, timestamp)
    }

    fun sendEnable(context: Context, syncId: String, alarmToken: String) =
        sendMutation(context, "ENABLE", syncId, alarmToken)

    fun sendDisable(context: Context, syncId: String, alarmToken: String) =
        sendMutation(context, "DISABLE", syncId, alarmToken)

    private fun buildPayload(entry: WearAlarmListStore.Entry, operation: String): String =
        JSONObject()
            .put("protocolVersion", PROTOCOL_VERSION)
            .put("syncId", entry.syncId)
            .put("operation", operation)
            .put("source", entry.source)
            .put("originDeviceId", entry.originDeviceId)
            .put("revision", entry.revision)
            .put("timestamp", entry.updatedAt)
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

    private fun sendDataItem(context: Context, syncId: String, payload: String) {
        val request = PutDataMapRequest.create("$PATH_ALARM_STATE/$syncId").apply {
            dataMap.putString(KEY_MUTATION, payload)
            dataMap.putLong(KEY_TIMESTAMP, System.currentTimeMillis())
        }.asPutDataRequest().setUrgent()
        Wearable.getDataClient(context.applicationContext).putDataItem(request)
    }

    private fun sendMessage(context: Context, payload: ByteArray) {
        Wearable.getNodeClient(context.applicationContext).connectedNodes.addOnSuccessListener { nodes ->
            nodes.forEach { node ->
                Wearable.getMessageClient(context.applicationContext).sendMessage(node.id, PATH_MUTATION, payload)
            }
        }
    }

    private fun deviceId(context: Context): String =
        context.getSharedPreferences("wakesync_identity", Context.MODE_PRIVATE)
            .getString("device_id", null)
            ?: "WATCH"

    private const val KEY_MUTATION = "mutation"
    private const val KEY_TIMESTAMP = "timestamp"
}
