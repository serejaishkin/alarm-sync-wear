package com.sysadmindoc.alarmclock.wear

import android.app.Activity
import android.os.Bundle
import android.view.Gravity
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.NumberPicker
import android.widget.SeekBar
import android.widget.TextView
import com.google.android.gms.wearable.Wearable
import org.json.JSONArray
import org.json.JSONObject

/** Equal-peer alarm editor. New alarms use CREATE_REQUEST; existing alarms use UPDATE_REQUEST. */
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
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER_HORIZONTAL; setPadding(20, 12, 20, 12) }
        root.addView(TextView(this).apply { text = if (syncId == null) "WakeSync — Новый будильник" else "WakeSync — Редактирование" })
        val time = LinearLayout(this).apply { gravity = Gravity.CENTER }
        hour = picker(0, 23, 7); minute = picker(0, 59, 0)
        time.addView(hour); time.addView(TextView(this).apply { text = ":" }); time.addView(minute); root.addView(time)
        label = EditText(this).apply { hint = "Название"; singleLine = true }; root.addView(label)
        root.addView(TextView(this).apply { text = "Повтор" })
        repeatChecks = listOf("Пн", "Вт", "Ср", "Чт", "Пт", "Сб", "Вс").map { CheckBox(this).apply { text = it } }
        repeatChecks.forEach(root::addView)
        root.addView(TextView(this).apply { text = "Snooze (мин)" }); snooze = picker(1, 60, 10); root.addView(snooze)
        vibration = CheckBox(this).apply { text = "Вибрация"; isChecked = true }; root.addView(vibration)
        root.addView(TextView(this).apply { text = "Громкость" }); volume = SeekBar(this).apply { max = 100; progress = 100 }; root.addView(volume)
        root.addView(Button(this).apply { text = "Сохранить"; setOnClickListener { if (syncId == null) sendCreate() else sendUpdate() } })
        setContentView(root)
    }

    private fun picker(min: Int, max: Int, value: Int) = NumberPicker(this).apply { minValue = min; maxValue = max; this.value = value; wrapSelectorWheel = true }

    private fun loadExisting() {
        val id = syncId ?: return
        val alarm = WearAlarmListStore.load(this).firstOrNull { it.syncId == id } ?: return
        hour.value = alarm.hour.coerceIn(hour.minValue, hour.maxValue)
        minute.value = alarm.minute.coerceIn(minute.minValue, minute.maxValue)
        label.setText(alarm.label)
        vibration.isChecked = true
    }

    private fun commonJson(operation: String): JSONObject = JSONObject()
        .put("operation", operation)
        .put("hour", hour.value)
        .put("minute", minute.value)
        .put("label", label.text.toString().trim())
        .put("snoozeDurationMinutes", snooze.value)
        .put("vibrationEnabled", vibration.isChecked)
        .put("volume", volume.progress)

    private fun sendCreate() {
        val repeat = JSONArray()
        repeatChecks.forEachIndexed { index, check -> if (check.isChecked) repeat.put(index + 1) }
        val payload = commonJson("CREATE_REQUEST").put("repeatDays", repeat).toString().toByteArray(Charsets.UTF_8)
        send(PATH_CREATE_REQUEST, payload)
    }

    private fun sendUpdate() {
        val payload = commonJson("UPDATE_REQUEST").put("syncId", syncId).toString().toByteArray(Charsets.UTF_8)
        send(PATH_UPDATE_REQUEST, payload)
    }

    private fun send(path: String, payload: ByteArray) {
        Wearable.getNodeClient(this).connectedNodes.addOnSuccessListener { nodes ->
            nodes.forEach { node -> Wearable.getMessageClient(this).sendMessage(node.id, path, payload) }
            finish()
        }
    }

    companion object {
        const val PATH_CREATE_REQUEST = "/wakesync/alarm/create_request"
        const val PATH_UPDATE_REQUEST = "/wakesync/alarm/update_request"
        const val EXTRA_SYNC_ID = "syncId"
    }
}
