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

/**
 * Full wrist-side editor for the common alarm settings that do not require a
 * phone-only resource (local ringtone URI, NFC tag, Hue scene, etc.).
 * The phone expands the request into the canonical Alarm model and sends the
 * canonical alarm back through the normal sync protocol.
 */
class WakeSyncAlarmEditorActivity : Activity() {
    private lateinit var hour: NumberPicker
    private lateinit var minute: NumberPicker
    private lateinit var label: EditText
    private lateinit var repeatChecks: List<CheckBox>
    private lateinit var snooze: NumberPicker
    private lateinit var vibration: CheckBox
    private lateinit var volume: SeekBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(20, 12, 20, 12)
        }
        root.addView(TextView(this).apply { text = "WakeSync — Новый будильник" })

        val time = LinearLayout(this).apply { gravity = Gravity.CENTER }
        hour = picker(0, 23, 7)
        minute = picker(0, 59, 0)
        time.addView(hour)
        time.addView(TextView(this).apply { text = ":" })
        time.addView(minute)
        root.addView(time)

        label = EditText(this).apply { hint = "Название"; singleLine = true }
        root.addView(label)

        root.addView(TextView(this).apply { text = "Повтор" })
        val days = listOf("Пн", "Вт", "Ср", "Чт", "Пт", "Сб", "Вс")
        repeatChecks = days.map { day ->
            CheckBox(this).apply { text = day }
        }
        repeatChecks.forEach { root.addView(it) }

        root.addView(TextView(this).apply { text = "Snooze (мин)" })
        snooze = picker(1, 60, 10)
        root.addView(snooze)

        vibration = CheckBox(this).apply { text = "Вибрация"; isChecked = true }
        root.addView(vibration)

        root.addView(TextView(this).apply { text = "Громкость" })
        volume = SeekBar(this).apply { max = 100; progress = 100 }
        root.addView(volume)

        root.addView(Button(this).apply {
            text = "Сохранить"
            setOnClickListener { sendCreate() }
        })
        setContentView(root)
    }

    private fun picker(min: Int, max: Int, value: Int) = NumberPicker(this).apply {
        minValue = min
        maxValue = max
        this.value = value
        wrapSelectorWheel = true
    }

    private fun sendCreate() {
        val repeat = JSONArray()
        repeatChecks.forEachIndexed { index, check -> if (check.isChecked) repeat.put(index + 1) }
        val payload = JSONObject()
            .put("operation", "CREATE_REQUEST")
            .put("hour", hour.value)
            .put("minute", minute.value)
            .put("label", label.text.toString().trim())
            .put("repeatDays", repeat)
            .put("snoozeDurationMinutes", snooze.value)
            .put("vibrationEnabled", vibration.isChecked)
            .put("volume", volume.progress)
            .toString().toByteArray(Charsets.UTF_8)
        Wearable.getNodeClient(this).connectedNodes.addOnSuccessListener { nodes ->
            nodes.forEach { node ->
                Wearable.getMessageClient(this).sendMessage(node.id, PATH_CREATE_REQUEST, payload)
            }
            finish()
        }
    }

    companion object {
        const val PATH_CREATE_REQUEST = "/wakesync/alarm/create_request"
    }
}
