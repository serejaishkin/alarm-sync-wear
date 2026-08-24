package com.sysadmindoc.alarmclock.wear

import android.content.Context
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.Wearable
import org.json.JSONArray
import org.json.JSONObject

/** Wear-side mutation gateway. Persistent state uses DataClient; MessageClient is the low-latency path. */
object WakeSyncPeerController {
    const val PATH_MUTATION = "/wakesync/alarm/mutation"
    const val PATH_ALARM_STATE = "/wakesync/alarm/state"
    const val PATH_REQUEST_SNAPSHOT = "/wakesync/alarm/request_snapshot"
    const val PATH_REQUEST_WATCH_SNAPSHOT = "/wakesync/alarm/request_watch_snapshot"
    const val PATH_WATCH_SNAPSHOT = "/wakesync/alarm/watch_snapshot"
    const val KEY_MUTATION = "mutation"
    const val KEY_TIMESTAMP = "timestamp"
    private const val PROTOCOL_VERSION = 1
    private const val KEY_ALARM_LIST = "alarm_list"
    private const val KEY_UPDATED_AT = "updated_at"
    private const val KEY_HAS_ALARM = "has_alarm"

    fun requestSnapshot(context: Context) {
        sendRawMessage(context, PATH_REQUEST_SNAPSHOT, ByteArray(0))
    }

    fun requestPhoneSnapshot(context: Context) {
        sendRawMessage(context, PATH_REQUEST_SNAPSHOT, ByteArray(0))
    }

    /**
     * Publish one complete Wear snapshot on a stable Data Layer path, following
     * the same communication pattern as the reference project. Deleted alarms
     * remain in the snapshot as tombstones so the phone can converge to zero.
     */
    fun sendWatchSnapshot(context: Context) {
        val appContext = context.applicationContext
        val list = JSONArray()
        WearAlarmListStore.load(appContext).forEach { entry ->
            list.put(buildPayload(entry, "UPDATE"))
        }
        WearAlarmListStore.tombstones(appContext).forEach { tombstone ->
            list.put(JSONObject()
                .put("protocolVersion", PROTOCOL_VERSION)
                .put("syncId", tombstone.syncId)
                .put("operation", "DELETE")
                .put("source", tombstone.source)
                .put("originDeviceId", tombstone.deviceId)
                .put("revision", tombstone.revision)
                .put("timestamp", tombstone.timestamp))
        }

        val request = PutDataMapRequest.create(WearAlarmData.PATH_NEXT_ALARM).apply {
            dataMap.putString(KEY_ALARM_LIST, list.toString())
            dataMap.putLong(KEY_UPDATED_AT, System.currentTimeMillis())
            dataMap.putBoolean(KEY_HAS_ALARM, WearAlarmListStore.load(appContext).isNotEmpty())
        }.asPutDataRequest().setUrgent()
        Wearable.getDataClient(appContext).putDataItem(request)
    }

    fun sendAlarmMutation(context: Context, entry: WearAlarmListStore.Entry, operation: String) {
        val payload = buildPayload(entry, operation)
        sendRawMessage(context, PATH_MUTATION, payload.toByteArray(Charsets.UTF_8))
        if (operation != "SNOOZE" && operation != "DISMISS" && operation != "RINGING") {
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
        sendRawMessage(context, PATH_MUTATION, payload.toByteArray(Charsets.UTF_8))
        sendDataItem(context, syncId, payload)
    }

    fun sendMutation(context: Context, operation: String, syncId: String, alarmToken: String?) {
        val current = WearAlarmListStore.load(context).firstOrNull { it.syncId == syncId } ?: return
        val updated = current.copy(
            enabled = when (operation) { "ENABLE" -> true; "DISABLE" -> false; else -> current.enabled },
            revision = current.revision + 1L,
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
        WearAlarmListStore.removeWithTombstone(context, syncId, revision, timestamp, "WATCH", deviceId(context))
        sendDelete(context, syncId, revision, timestamp)
    }

    fun sendEnable(context: Context, syncId: String, alarmToken: String) = sendMutation(context, "ENABLE", syncId, alarmToken)
    fun sendDisable(context: Context, syncId: String, alarmToken: String) = sendMutation(context, "DISABLE", syncId, alarmToken)

    private fun buildPayload(entry: WearAlarmListStore.Entry, operation: String): JSONObject = JSONObject()
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

    private fun sendDataItem(context: Context, syncId: String, payload: String) {
        val request = PutDataMapRequest.create("$PATH_ALARM_STATE/$syncId").apply {
            dataMap.putString(KEY_MUTATION, payload)
            dataMap.putLong(KEY_TIMESTAMP, System.currentTimeMillis())
        }.asPutDataRequest().setUrgent()
        Wearable.getDataClient(context.applicationContext).putDataItem(request)
    }

    private fun sendRawMessage(context: Context, path: String, payload: ByteArray) {
        Wearable.getNodeClient(context.applicationContext).connectedNodes.addOnSuccessListener { nodes ->
            nodes.forEach { node -> Wearable.getMessageClient(context.applicationContext).sendMessage(node.id, path, payload) }
        }
    }

    private fun deviceId(context: Context): String = context
        .getSharedPreferences("wakesync_identity", Context.MODE_PRIVATE)
        .getString("device_id", null) ?: "WATCH"
}
