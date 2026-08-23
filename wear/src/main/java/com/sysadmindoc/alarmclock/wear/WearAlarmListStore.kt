package com.sysadmindoc.alarmclock.wear

import android.content.Context
import androidx.core.content.edit
import org.json.JSONArray
import org.json.JSONObject

/** Local peer-side collection. The watch keeps the same logical sync IDs as the phone. */
object WearAlarmListStore {
    private const val PREFS = "wakesync_alarm_list"
    private const val KEY_ALARMS = "alarms"

    data class Entry(
        val syncId: String,
        val label: String,
        val hour: Int,
        val minute: Int,
        val enabled: Boolean,
        val revision: Long,
        val updatedAt: Long,
        val alarmToken: String
    )

    fun load(context: Context): List<Entry> {
        val raw = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_ALARMS, null) ?: return emptyList()
        return runCatching {
            val array = JSONArray(raw)
            buildList(array.length()) {
                for (i in 0 until array.length()) {
                    val o = array.getJSONObject(i)
                    add(
                        Entry(
                            syncId = o.optString("syncId"),
                            label = o.optString("label"),
                            hour = o.optInt("hour"),
                            minute = o.optInt("minute"),
                            enabled = o.optBoolean("enabled", true),
                            revision = o.optLong("revision"),
                            updatedAt = o.optLong("updatedAt"),
                            alarmToken = o.optString("alarmToken")
                        )
                    )
                }
            }
        }.getOrDefault(emptyList())
    }

    fun save(context: Context, entries: List<Entry>) {
        val array = JSONArray()
        entries.sortedBy { it.hour * 60 + it.minute }.forEach { e ->
            array.put(JSONObject()
                .put("syncId", e.syncId)
                .put("label", e.label)
                .put("hour", e.hour)
                .put("minute", e.minute)
                .put("enabled", e.enabled)
                .put("revision", e.revision)
                .put("updatedAt", e.updatedAt)
                .put("alarmToken", e.alarmToken))
        }
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit {
            putString(KEY_ALARMS, array.toString())
        }
    }

    fun upsert(context: Context, entry: Entry) {
        save(context, load(context).filterNot { it.syncId == entry.syncId } + entry)
    }

    fun remove(context: Context, syncId: String) {
        save(context, load(context).filterNot { it.syncId == syncId })
    }
}
