package com.sysadmindoc.alarmclock.wear

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import java.util.Locale

class WakeSyncAlarmListActivity : Activity() {
    private lateinit var list: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        buildUi()
    }

    override fun onResume() {
        super.onResume()
        render()
    }

    private fun buildUi() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(14, 12, 14, 12)
        }
        root.addView(TextView(this).apply {
            text = "WakeSync\nБудильники"
            textSize = 18f
            gravity = Gravity.CENTER
        })
        list = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(-1, 0, 1f)
        }
        root.addView(list)
        root.addView(Button(this).apply {
            text = "+ Новый будильник"
            setOnClickListener {
                startActivity(Intent(this@WakeSyncAlarmListActivity, WakeSyncAlarmEditorActivity::class.java))
            }
        })
        setContentView(root)
    }

    private fun render() {
        list.removeAllViews()
        val alarms = WearAlarmListStore.load(this)
        if (alarms.isEmpty()) {
            list.addView(TextView(this).apply {
                text = "Нет будильников"
                gravity = Gravity.CENTER
                setPadding(8, 30, 8, 30)
            })
            return
        }
        alarms.forEach { alarm ->
            val time = String.format(Locale.US, "%02d:%02d", alarm.hour, alarm.minute)
            list.addView(Button(this).apply {
                text = "$time  ${alarm.label.ifBlank { "Будильник" }}\n${if (alarm.enabled) "Включён" else "Выключен"}"
                isAllCaps = false
                setOnClickListener {
                    startActivity(Intent(this@WakeSyncAlarmListActivity, WakeSyncAlarmEditorActivity::class.java)
                        .putExtra("syncId", alarm.syncId)
                        .putExtra("alarmToken", alarm.alarmToken))
                }
            }, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = 6 })
        }
    }
}
