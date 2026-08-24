package com.sysadmindoc.alarmclock.wear

import android.app.Activity
import android.os.Bundle
import android.view.Gravity
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.NumberPicker
import android.widget.ScrollView
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import java.util.UUID

class WakeSyncAlarmEditorActivity : Activity() {
    private lateinit var hour: NumberPicker
    private lateinit var minute: NumberPicker
    private lateinit var label: EditText
    private lateinit var repeatChecks: List<CheckBox>
    private lateinit var snooze: NumberPicker
    private lateinit var vibration: CheckBox
    private lateinit var volume: SeekBar
    private val syncId: String? get() = intent.getStringExtra(EXTRA_SYNC_ID)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        buildUi()
        loadExisting()
    }

    private fun buildUi() {
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(20, 12, 20, 24)
        }
        content.addView(TextView(this).apply {
            text = if (syncId == null) "WakeSync — Новый будильник" else "WakeSync — Редактирование"
            gravity = Gravity.CENTER
        })
        val time = LinearLayout(this).apply { gravity = Gravity.CENTER }
        hour = picker(0, 23, 7)
        minute = picker(0, 59, 0)
        time.addView(hour)
        time.addView(TextView(this).apply { text = ":" })
        time.addView(minute)
        content.addView(time)

        label = EditText(this).apply { hint = "Название"; setSingleLine(true) }
        content.addView(label)
        content.addView(TextView(this).apply { text = "Повтор" })
        repeatChecks = listOf("Пн", "Вт", "Ср", "Чт", "Пт", "Сб", "Вс").map { day ->
            CheckBox(this).apply { text = day }
        }
        repeatChecks.forEach(content::addView)

        content.addView(TextView(this).apply { text = "Отсрочка (мин)" })
        snooze = picker(1, 60, 10)
        content.addView(snooze)
        vibration = CheckBox(this).apply { text = "Вибрация"; isChecked = true }
        content.addView(vibration)
        content.addView(TextView(this).apply { text = "Громкость" })
        volume = SeekBar(this).apply { max = 100; progress = 100 }
        content.addView(volume)
        content.addView(Button(this).apply {
            text = "Сохранить"
            setOnClickListener { saveAlarm() }
        })

        setContentView(ScrollView(this).apply { addView(content) })
    }

    private fun picker(min: Int, max: Int, value: Int) = NumberPicker(this).apply {
        minValue = min
        maxValue = max
        this.value = value
        wrapSelectorWheel = true
    }

    private fun loadExisting() {
        val id = syncId ?: return
        val alarm = WearAlarmListStore.load(this).firstOrNull { it.syncId == id } ?: return
        hour.value = alarm.hour.coerceIn(hour.minValue, hour.maxValue)
        minute.value = alarm.minute.coerceIn(minute.minValue, minute.maxValue)
        label.setText(alarm.label)
        repeatChecks.forEachIndexed { index, check -> check.isChecked = index + 1 in alarm.repeatDays }
        snooze.value = alarm.snoozeDurationMinutes.coerceIn(snooze.minValue, snooze.maxValue)
        vibration.isChecked = alarm.vibrationEnabled
        volume.progress = alarm.volume.coerceIn(0, 100)
    }

    private fun collectEntry(): WearAlarmListStore.Entry {
        val existing = syncId?.let { id -> WearAlarmListStore.load(this).firstOrNull { it.syncId == id } }
        val now = System.currentTimeMillis()
        return WearAlarmListStore.Entry(
            syncId = existing?.syncId ?: UUID.randomUUID().toString(),
            label = label.text.toString().trim(),
            hour = hour.value,
            minute = minute.value,
            enabled = existing?.enabled ?: true,
            repeatDays = repeatChecks.mapIndexedNotNull { index, check ->
                if (check.isChecked) index + 1 else null
            }.toSet(),
            snoozeDurationMinutes = snooze.value,
            vibrationEnabled = vibration.isChecked,
            volume = volume.progress,
            revision = (existing?.revision ?: 0L) + 1L,
            updatedAt = now,
            alarmToken = existing?.alarmToken ?: UUID.randomUUID().toString()
        )
    }

    private fun saveAlarm() {
        val existing = syncId?.let { id -> WearAlarmListStore.load(this).firstOrNull { it.syncId == id } }
        val entry = collectEntry()

        // Local persistence is authoritative for the watch. The alarm is
        // scheduled even when the phone is unavailable.
        WearAlarmListStore.upsert(this, entry)

        val operation = if (existing == null) "CREATE" else "UPDATE"
        WakeSyncPeerController.sendAlarmMutation(this, entry, operation)

        Toast.makeText(
            this,
            if (existing == null) "Будильник сохранён" else "Изменения сохранены",
            Toast.LENGTH_SHORT
        ).show()
        finish()
    }

    companion object {
        const val EXTRA_SYNC_ID = "syncId"
    }
}
