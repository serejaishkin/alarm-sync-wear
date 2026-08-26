package com.sysadmindoc.alarmclock.wear

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import com.google.android.gms.wearable.Wearable
import java.util.Locale

/** Equal peer list: alarms received from phone and alarms created on Wear live here. */
class WakeSyncAlarmListActivity : Activity() {
    private lateinit var list: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        buildUi()
    }

    override fun onResume() {
        super.onResume()
        requestPhoneSnapshot()
        render()
    }

    private fun buildUi() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(14, 12, 14, 18)
        }
        WearUi.styleRoot(this, root)
        root.addView(TextView(this).apply {
            text = "WakeSync\nБудильники"
            gravity = Gravity.CENTER
            WearUi.styleHeader(this@WakeSyncAlarmListActivity, this)
        })
        val scroll = ScrollView(this)
        list = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        scroll.addView(list)
        root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
        root.addView(Button(this).apply {
            text = "+ Новый будильник"
            WearUi.styleActionButton(this@WakeSyncAlarmListActivity, this, R.color.accent_blue)
            setOnClickListener {
                startActivity(Intent(this@WakeSyncAlarmListActivity, WakeSyncAlarmEditorActivity::class.java))
            }
        })
        setContentView(root)
    }

    private fun requestPhoneSnapshot() {
        Wearable.getNodeClient(this).connectedNodes.addOnSuccessListener { nodes ->
            nodes.forEach { node ->
                Wearable.getMessageClient(this)
                    .sendMessage(node.id, PATH_REQUEST_SNAPSHOT, ByteArray(0))
            }
        }
    }

    private fun render() {
        list.removeAllViews()
        val alarms = WearAlarmListStore.load(this)
        if (alarms.isEmpty()) {
            list.addView(TextView(this).apply {
                text = "Нет будильников\nСинхронизация запрошена"
                gravity = Gravity.CENTER
                setPadding(8, 30, 8, 30)
                WearUi.styleSectionLabel(this@WakeSyncAlarmListActivity, this)
            })
            return
        }

        alarms.forEach { alarm ->
            val time = String.format(Locale.US, "%02d:%02d", alarm.hour, alarm.minute)
            val repeat = repeatLabel(alarm.repeatDays)
            val changedAt = android.text.format.DateFormat.format("HH:mm", alarm.updatedAt)
            val changedLabel = "Изменено на ${if (alarm.source == WearAlarmListStore.SOURCE_WATCH) "часах" else "телефоне"} в $changedAt"
            val row = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
            row.addView(Button(this).apply {
                text = "$time  ${alarm.label.ifBlank { "Будильник" }}\n$repeat · ${if (alarm.enabled) "Включён" else "Выключен"} · $changedLabel"
                WearUi.styleCardButton(this@WakeSyncAlarmListActivity, this)
                setOnClickListener {
                    startActivity(Intent(this@WakeSyncAlarmListActivity, WakeSyncAlarmEditorActivity::class.java)
                        .putExtra("syncId", alarm.syncId)
                        .putExtra("alarmToken", alarm.alarmToken))
                }
            })
            val actions = LinearLayout(this).apply { gravity = Gravity.CENTER }
            actions.addView(Button(this).apply {
                text = if (alarm.enabled) "Выкл" else "Вкл"
                WearUi.styleActionButton(this@WakeSyncAlarmListActivity, this, R.color.accent_blue)
                setOnClickListener {
                    if (alarm.enabled) {
                        WakeSyncPeerController.sendDisable(this@WakeSyncAlarmListActivity, alarm.syncId, alarm.alarmToken)
                        WearAlarmListStore.upsert(this@WakeSyncAlarmListActivity, alarm.copy(enabled = false, revision = alarm.revision + 1, updatedAt = System.currentTimeMillis()))
                    } else {
                        WakeSyncPeerController.sendEnable(this@WakeSyncAlarmListActivity, alarm.syncId, alarm.alarmToken)
                        WearAlarmListStore.upsert(this@WakeSyncAlarmListActivity, alarm.copy(enabled = true, revision = alarm.revision + 1, updatedAt = System.currentTimeMillis()))
                    }
                    render()
                }
            })
            actions.addView(Button(this).apply {
                text = "Удалить"
                WearUi.styleActionButton(this@WakeSyncAlarmListActivity, this, R.color.accent_red)
                setOnClickListener {
                    WakeSyncPeerController.sendDelete(this@WakeSyncAlarmListActivity, alarm.syncId)
                    WearAlarmListStore.remove(this@WakeSyncAlarmListActivity, alarm.syncId)
                    render()
                }
            })
            row.addView(actions)
            list.addView(row, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = 6 })
        }
    }

    private fun repeatLabel(days: Set<Int>): String = when {
        days.size == 7 -> "Каждый день"
        days == setOf(1, 2, 3, 4, 5) -> "Будни"
        days == setOf(6, 7) -> "Выходные"
        days.isEmpty() -> "Один раз"
        else -> days.sorted().joinToString(" ") { DAY_NAMES[it - 1] ?: "" }.trim()
    }

    companion object {
        const val PATH_REQUEST_SNAPSHOT = "/wakesync/alarm/request_snapshot"
        private val DAY_NAMES = mapOf(1 to "Пн", 2 to "Вт", 3 to "Ср", 4 to "Чт", 5 to "Пт", 6 to "Сб", 7 to "Вс")
    }
}
