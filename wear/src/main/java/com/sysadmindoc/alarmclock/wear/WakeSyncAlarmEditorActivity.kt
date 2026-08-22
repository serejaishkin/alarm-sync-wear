package com.sysadmindoc.alarmclock.wear

import android.app.Activity
import android.os.Bundle
import android.view.Gravity
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.NumberPicker
import android.widget.TextView
import com.google.android.gms.wearable.Wearable
import org.json.JSONObject

/** Wrist-side alarm creation. The watch is a peer and sends the mutation to the phone. */
class WakeSyncAlarmEditorActivity : Activity() {
    private lateinit var hour: NumberPicker
    private lateinit var minute: NumberPicker
    private lateinit var label: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(20, 16, 20, 16)
        }
        root.addView(TextView(this).apply { text = "WakeSync — Новый будильник" })
        val time = LinearLayout(this).apply { gravity = Gravity.CENTER }
        hour = picker(0, 23)
        minute = picker(0, 59)
        time.addView(hour)
        time.addView(TextView(this).apply { text = ":" })
        time.addView(minute)
        root.addView(time)
        label = EditText(this).apply { hint = "Название"; singleLine = true }
        root.addView(label)
        root.addView(Button(this).apply {
            text = "Сохранить"
            setOnClickListener { sendCreate() }
        })
        setContentView(root)
    }

    private fun picker(min: Int, max: Int) = NumberPicker(this).apply {
        minValue = min
        maxValue = max
        wrapSelectorWheel = true
    }

    private fun sendCreate() {
        val payload = JSONObject()
            .put("operation", "CREATE_REQUEST")
            .put("hour", hour.value)
            .put("minute", minute.value)
            .put("label", label.text.toString().trim())
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
