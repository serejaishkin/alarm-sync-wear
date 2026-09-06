package com.sysadmindoc.alarmclock.wear

import android.content.Context
import androidx.core.content.edit
import org.json.JSONArray
import org.json.JSONObject

object WearAlarmListStore {
    private const val PREFS = "wakesync_alarm_list"
    private const val KEY_ALARMS = "alarms"
    private const val KEY_TOMBSTONES = "tombstones"

    data class Entry(
        val syncId: String, val label: String, val hour: Int, val minute: Int,
        val enabled: Boolean, val repeatDays: Set<Int> = emptySet(),
        val snoozeDurationMinutes: Int = 10, val vibrationEnabled: Boolean = true,
        val volume: Int = 100, val revision: Long, val updatedAt: Long,
        val alarmToken: String, val source: String = SOURCE_WATCH,
        val originDeviceId: String = ""
    )

    data class Tombstone(
        val syncId: String,
        val revision: Long,
        val timestamp: Long,
        val source: String,
        val deviceId: String
    )

    private data class Version(val revision: Long, val timestamp: Long, val source: String, val deviceId: String)

    @Synchronized
    fun load(context: Context): List<Entry> {
        val raw = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY_ALARMS, null) ?: return emptyList()
        return runCatching {
            val array = JSONArray(raw)
            buildList(array.length()) {
                for (i in 0 until array.length()) {
                    val o = array.getJSONObject(i)
                    val days = o.optJSONArray("repeatDays")?.let { a -> buildSet { for (j in 0 until a.length()) add(a.optInt(j)) } } ?: emptySet()
                    add(Entry(
                        syncId = o.optString("syncId"),
                        label = o.optString("label"),
                        hour = o.optInt("hour"),
                        minute = o.optInt("minute"),
                        enabled = o.optBoolean("enabled", true),
                        repeatDays = days,
                        snoozeDurationMinutes = o.optInt("snoozeDurationMinutes", 10),
                        vibrationEnabled = o.optBoolean("vibrationEnabled", true),
                        volume = o.optInt("volume", 100),
                        revision = o.optLong("revision"),
                        updatedAt = o.optLong("updatedAt"),
                        alarmToken = o.optString("alarmToken"),
                        source = o.optString("source", SOURCE_WATCH),
                        originDeviceId = o.optString("originDeviceId")
                    ))
                }
            }
        }.getOrDefault(emptyList())
    }

    @Synchronized
    fun tombstones(context: Context): List<Tombstone> =
        readTombstones(context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)).map { (syncId, v) ->
            Tombstone(syncId, v.revision, v.timestamp, v.source, v.deviceId)
        }

    @Synchronized
    fun save(context: Context, entries: List<Entry>) {
        val array = JSONArray()
        entries.sortedWith(compareBy<Entry> { it.hour * 60 + it.minute }.thenBy { it.syncId }).forEach { e ->
            val days = JSONArray(); e.repeatDays.sorted().forEach(days::put)
            array.put(JSONObject()
                .put("syncId", e.syncId)
                .put("label", e.label)
                .put("hour", e.hour)
                .put("minute", e.minute)
                .put("enabled", e.enabled)
                .put("repeatDays", days)
                .put("snoozeDurationMinutes", e.snoozeDurationMinutes)
                .put("vibrationEnabled", e.vibrationEnabled)
                .put("volume", e.volume)
                .put("revision", e.revision)
                .put("updatedAt", e.updatedAt)
                .put("alarmToken", e.alarmToken)
                .put("source", e.source)
                .put("originDeviceId", e.originDeviceId))
        }
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit { putString(KEY_ALARMS, array.toString()) }
    }

    @Synchronized
    fun upsert(context: Context, entry: Entry) {
        val current = load(context).firstOrNull { it.syncId == entry.syncId }
        if (current != null && compareVersions(entry, current) <= 0) return
        val tombstone = readTombstones(context.getSharedPreferences(PREFS, Context.MODE_PRIVATE))[entry.syncId]
        if (tombstone != null && compareVersion(toVersion(entry), tombstone) <= 0) return
        save(context, load(context).filterNot { it.syncId == entry.syncId } + entry)
        clearTombstone(context, entry.syncId)
        WearAlarmScheduler.schedule(context, entry)
    }

    @Synchronized
    fun remove(context: Context, syncId: String) {
        WearAlarmScheduler.cancel(context, syncId)
        save(context, load(context).filterNot { it.syncId == syncId })
    }

    @Synchronized
    fun removeWithTombstone(context: Context, syncId: String, revision: Long, timestamp: Long, source: String = SOURCE_WATCH, deviceId: String = "") {
        remove(context, syncId)
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val tombstones = readTombstones(prefs)
        val candidate = Version(revision, timestamp, source, deviceId)
        if (tombstones[syncId] == null || compareVersion(candidate, tombstones.getValue(syncId)) > 0) {
            tombstones[syncId] = candidate
            writeTombstones(prefs, tombstones)
        }
    }

    @Synchronized
    fun revisionFor(context: Context, syncId: String): Long {
        val e = load(context).firstOrNull { it.syncId == syncId }
        val t = readTombstones(context.getSharedPreferences(PREFS, Context.MODE_PRIVATE))[syncId]
        return maxOf(e?.revision ?: 0L, t?.revision ?: 0L)
    }

    @Synchronized
    fun timestampFor(context: Context, syncId: String): Long {
        val e = load(context).firstOrNull { it.syncId == syncId }
        val t = readTombstones(context.getSharedPreferences(PREFS, Context.MODE_PRIVATE))[syncId]
        return maxOf(e?.updatedAt ?: 0L, t?.timestamp ?: 0L)
    }

    private fun toVersion(e: Entry) = Version(e.revision, e.updatedAt, e.source, e.originDeviceId)
    private fun compareVersions(a: Entry, b: Entry) = compareVersion(toVersion(a), toVersion(b))
    private fun compareVersion(a: Version, b: Version): Int = when {
        a.revision != b.revision -> a.revision.compareTo(b.revision)
        a.timestamp != b.timestamp -> a.timestamp.compareTo(b.timestamp)
        a.source != b.source -> sourcePriority(a.source).compareTo(sourcePriority(b.source))
        else -> a.deviceId.compareTo(b.deviceId)
    }
    private fun sourcePriority(source: String) = if (source == SOURCE_WATCH) 2 else 1

    private fun clearTombstone(context: Context, syncId: String) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val t = readTombstones(prefs)
        if (t.remove(syncId) != null) writeTombstones(prefs, t)
    }

    private fun readTombstones(prefs: android.content.SharedPreferences): MutableMap<String, Version> {
        val raw = prefs.getString(KEY_TOMBSTONES, null) ?: return mutableMapOf()
        return runCatching {
            val json = JSONObject(raw)
            buildMap {
                json.keys().forEach { key ->
                    val item = json.optJSONObject(key) ?: return@forEach
                    put(key, Version(item.optLong("revision"), item.optLong("timestamp"), item.optString("source", SOURCE_WATCH), item.optString("deviceId")))
                }
            }.toMutableMap()
        }.getOrDefault(mutableMapOf())
    }

    private fun writeTombstones(prefs: android.content.SharedPreferences, tombstones: Map<String, Version>) {
        val json = JSONObject()
        tombstones.forEach { (id, v) -> json.put(id, JSONObject().put("revision", v.revision).put("timestamp", v.timestamp).put("source", v.source).put("deviceId", v.deviceId)) }
        prefs.edit { putString(KEY_TOMBSTONES, json.toString()) }
    }

    const val SOURCE_WATCH = "WATCH"
}
