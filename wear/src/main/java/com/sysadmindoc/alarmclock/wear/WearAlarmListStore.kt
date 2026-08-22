package com.sysadmindoc.alarmclock.wear

import android.content.Context
import androidx.core.content.edit
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types

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

    private val moshi = Moshi.Builder().build()
    private val type = Types.newParameterizedType(List::class.java, Entry::class.java)
    private val adapter = moshi.adapter<List<Entry>>(type)

    fun load(context: Context): List<Entry> {
        val raw = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_ALARMS, null) ?: return emptyList()
        return runCatching { adapter.fromJson(raw).orEmpty() }.getOrDefault(emptyList())
    }

    fun save(context: Context, entries: List<Entry>) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit {
            putString(KEY_ALARMS, adapter.toJson(entries.sortedBy { it.hour * 60 + it.minute }))
        }
    }

    fun upsert(context: Context, entry: Entry) {
        val current = load(context).filterNot { it.syncId == entry.syncId }
        save(context, current + entry)
    }

    fun remove(context: Context, syncId: String) {
        save(context, load(context).filterNot { it.syncId == syncId })
    }
}
