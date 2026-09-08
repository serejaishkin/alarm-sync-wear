package com.wakesync.app.wear

import android.app.Activity
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import java.util.Locale
import java.util.UUID

/**
 * Dedicated Wear OS Alarm Editor for WakeSync.
 * Uses freeze-proof, touch-first stepped controls specifically designed
 * to eliminate NumberPicker/ScrollView touch intercept deadlocks on Wear OS.
 */
class WakeSyncAlarmEditorActivity : Activity() {
    private var hourValue = 7
    private var minuteValue = 0
    private var labelValue = ""
    private val repeatDays = mutableSetOf<Int>()
    private var snoozeMinutes = 10
    private var vibrationEnabled = true
    private var volumePercent = 100

    private lateinit var timeDisplay: TextView
    private lateinit var labelDisplay: TextView
    private lateinit var snoozeDisplay: TextView
    private lateinit var vibrationButton: Button
    private lateinit var volumeDisplay: TextView
    private val dayButtons = mutableListOf<Button>()

    private val syncId: String? get() = intent.getStringExtra(EXTRA_SYNC_ID)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        labelValue = getString(R.string.wear_default_label)
        loadExisting()
        buildUi()
    }

    private fun loadExisting() {
        val id = syncId ?: return
        val alarm = WearAlarmListStore.load(this).firstOrNull { it.syncId == id } ?: return
        hourValue = alarm.hour.coerceIn(0, 23)
        minuteValue = alarm.minute.coerceIn(0, 59)
        labelValue = alarm.label.ifBlank { getString(R.string.wear_default_label) }
        repeatDays.clear()
        repeatDays.addAll(alarm.repeatDays)
        snoozeMinutes = alarm.snoozeDurationMinutes.coerceIn(1, 60)
        vibrationEnabled = alarm.vibrationEnabled
        volumePercent = alarm.volume.coerceIn(0, 100)
    }

    private fun buildUi() {
        val scroll = ScrollView(this).apply {
            isFillViewport = true
            isFocusable = true
            isFocusableInTouchMode = true
            setOnGenericMotionListener { _, event ->
                if (event.action == MotionEvent.ACTION_SCROLL) {
                    val axis = event.getAxisValue(MotionEvent.AXIS_SCROLL)
                    if (axis != 0f) {
                        scrollBy(0, (-axis * 64).toInt())
                        return@setOnGenericMotionListener true
                    }
                }
                false
            }
        }
        WearUi.styleRoot(this, scroll)

        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(16, 32, 16, 48)
        }

        // Title
        content.addView(TextView(this).apply {
            text = if (syncId == null) getString(R.string.wear_new_alarm_title) else getString(R.string.wear_editing_title)
            gravity = Gravity.CENTER
            WearUi.styleHeader(this@WakeSyncAlarmEditorActivity, this)
            setPadding(0, 0, 0, 12)
        })

        // 1. Time Display Card
        val timeCard = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            background = WearUi.drawable(this@WakeSyncAlarmEditorActivity, R.drawable.bg_card_button)
            setPadding(12, 12, 12, 12)
        }

        timeDisplay = TextView(this).apply {
            textSize = 32f
            gravity = Gravity.CENTER
            setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD))
            setTextColor(WearUi.color(this@WakeSyncAlarmEditorActivity, R.color.text_primary))
        }
        updateTimeText()
        timeCard.addView(timeDisplay)

        // Hours & Minutes Adjustment Row
        val adjustRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setPadding(0, 8, 0, 0)
        }

        adjustRow.addView(createStepButton(getString(R.string.wear_hours_minus)) {
            hourValue = if (hourValue <= 0) 23 else hourValue - 1
            updateTimeText()
        }, LinearLayout.LayoutParams(0, -2, 1f).apply { rightMargin = 4 })

        adjustRow.addView(createStepButton(getString(R.string.wear_hours_plus)) {
            hourValue = if (hourValue >= 23) 0 else hourValue + 1
            updateTimeText()
        }, LinearLayout.LayoutParams(0, -2, 1f).apply { rightMargin = 8 })

        adjustRow.addView(createStepButton(getString(R.string.wear_minutes_minus)) {
            minuteValue = if (minuteValue <= 0) 59 else minuteValue - 1
            updateTimeText()
        }, LinearLayout.LayoutParams(0, -2, 1f).apply { rightMargin = 4 })

        adjustRow.addView(createStepButton(getString(R.string.wear_minutes_plus)) {
            minuteValue = if (minuteValue >= 59) 0 else minuteValue + 1
            updateTimeText()
        }, LinearLayout.LayoutParams(0, -2, 1f))

        timeCard.addView(adjustRow)
        content.addView(timeCard, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = 12 })

        // 2. Quick Label Selector
        content.addView(TextView(this).apply {
            text = getString(R.string.wear_label)
            WearUi.styleSectionLabel(this@WakeSyncAlarmEditorActivity, this)
            setPadding(4, 0, 0, 4)
        })

        labelDisplay = TextView(this).apply {
            text = labelValue
            textSize = 14f
            setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD))
            setTextColor(WearUi.color(this@WakeSyncAlarmEditorActivity, R.color.accent_blue))
            setPadding(4, 0, 0, 6)
        }
        content.addView(labelDisplay)

        val labelRow1 = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
        }
        listOf(getString(R.string.wear_preset_morning), getString(R.string.wear_preset_work), getString(R.string.wear_preset_wake_up)).forEach { chip ->
            labelRow1.addView(createChipButton(chip) {
                labelValue = chip
                labelDisplay.text = labelValue
            }, LinearLayout.LayoutParams(0, -2, 1f).apply { rightMargin = 4 })
        }
        content.addView(labelRow1, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = 4 })

        val labelRow2 = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
        }
        listOf(getString(R.string.wear_preset_sport), getString(R.string.wear_preset_medicine), getString(R.string.wear_preset_event)).forEach { chip ->
            labelRow2.addView(createChipButton(chip) {
                labelValue = chip
                labelDisplay.text = labelValue
            }, LinearLayout.LayoutParams(0, -2, 1f).apply { rightMargin = 4 })
        }
        content.addView(labelRow2, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = 12 })

        // 3. Repeat Days (Пн-Вс)
        content.addView(TextView(this).apply {
            text = getString(R.string.wear_repeat_days)
            WearUi.styleSectionLabel(this@WakeSyncAlarmEditorActivity, this)
            setPadding(4, 0, 0, 6)
        })

        val daysRow1 = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
        }
        val daysRow2 = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
        }

        val dayLabels = listOf(
            1 to getString(R.string.wear_day_mon), 2 to getString(R.string.wear_day_tue),
            3 to getString(R.string.wear_day_wed), 4 to getString(R.string.wear_day_thu),
            5 to getString(R.string.wear_day_fri), 6 to getString(R.string.wear_day_sat),
            7 to getString(R.string.wear_day_sun)
        )
        dayButtons.clear()

        dayLabels.take(4).forEach { (dayIndex, name) ->
            val btn = createDayButton(dayIndex, name)
            dayButtons.add(btn)
            daysRow1.addView(btn, LinearLayout.LayoutParams(0, -2, 1f).apply { rightMargin = 3 })
        }
        content.addView(daysRow1, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = 4 })

        dayLabels.drop(4).forEach { (dayIndex, name) ->
            val btn = createDayButton(dayIndex, name)
            dayButtons.add(btn)
            daysRow2.addView(btn, LinearLayout.LayoutParams(0, -2, 1f).apply { rightMargin = 3 })
        }
        content.addView(daysRow2, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = 8 })

        // Presets for days
        val presetsRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
        }
        presetsRow.addView(createChipButton(getString(R.string.wear_preset_weekdays)) {
            repeatDays.clear()
            repeatDays.addAll(listOf(1, 2, 3, 4, 5))
            refreshDayButtons()
        }, LinearLayout.LayoutParams(0, -2, 1f).apply { rightMargin = 4 })

        presetsRow.addView(createChipButton(getString(R.string.wear_preset_all)) {
            repeatDays.clear()
            repeatDays.addAll(listOf(1, 2, 3, 4, 5, 6, 7))
            refreshDayButtons()
        }, LinearLayout.LayoutParams(0, -2, 1f).apply { rightMargin = 4 })

        presetsRow.addView(createChipButton(getString(R.string.wear_preset_reset)) {
            repeatDays.clear()
            refreshDayButtons()
        }, LinearLayout.LayoutParams(0, -2, 1f))
        content.addView(presetsRow, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = 14 })

        // 4. Snooze Selector
        val snoozeCard = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = WearUi.drawable(this@WakeSyncAlarmEditorActivity, R.drawable.bg_card_button)
            setPadding(10, 8, 10, 8)
        }
        val snoozeHeader = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        snoozeHeader.addView(TextView(this).apply {
            text = getString(R.string.wear_snooze)
            textSize = 12f
            setTextColor(WearUi.color(this@WakeSyncAlarmEditorActivity, R.color.text_secondary))
        }, LinearLayout.LayoutParams(0, -2, 1f))

        snoozeDisplay = TextView(this).apply {
            text = getString(R.string.wear_value_min, snoozeMinutes)
            textSize = 13f
            setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD))
            setTextColor(WearUi.color(this@WakeSyncAlarmEditorActivity, R.color.text_primary))
        }
        snoozeHeader.addView(snoozeDisplay)
        snoozeCard.addView(snoozeHeader)

        val snoozeAdjustRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setPadding(0, 6, 0, 0)
        }
        snoozeAdjustRow.addView(createStepButton(getString(R.string.wear_snooze_minus_5)) {
            snoozeMinutes = (snoozeMinutes - 5).coerceIn(5, 60)
            snoozeDisplay.text = getString(R.string.wear_value_min, snoozeMinutes)
        }, LinearLayout.LayoutParams(0, -2, 1f).apply { rightMargin = 6 })

        snoozeAdjustRow.addView(createStepButton(getString(R.string.wear_snooze_plus_5)) {
            snoozeMinutes = (snoozeMinutes + 5).coerceIn(5, 60)
            snoozeDisplay.text = getString(R.string.wear_value_min, snoozeMinutes)
        }, LinearLayout.LayoutParams(0, -2, 1f))
        snoozeCard.addView(snoozeAdjustRow)
        content.addView(snoozeCard, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = 10 })

        // 5. Vibration & Sound
        vibrationButton = Button(this).apply {
            isAllCaps = false
            textSize = 12f
            setOnClickListener {
                vibrationEnabled = !vibrationEnabled
                updateVibrationButtonText()
            }
        }
        updateVibrationButtonText()
        content.addView(vibrationButton, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = 10 })

        // Volume
        val volumeCard = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = WearUi.drawable(this@WakeSyncAlarmEditorActivity, R.drawable.bg_card_button)
            setPadding(10, 8, 10, 8)
        }
        val volumeHeader = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        volumeHeader.addView(TextView(this).apply {
            text = getString(R.string.wear_volume)
            textSize = 12f
            setTextColor(WearUi.color(this@WakeSyncAlarmEditorActivity, R.color.text_secondary))
        }, LinearLayout.LayoutParams(0, -2, 1f))

        volumeDisplay = TextView(this).apply {
            text = "$volumePercent%"
            textSize = 13f
            setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD))
            setTextColor(WearUi.color(this@WakeSyncAlarmEditorActivity, R.color.text_primary))
        }
        volumeHeader.addView(volumeDisplay)
        volumeCard.addView(volumeHeader)

        val volumeAdjustRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setPadding(0, 6, 0, 0)
        }
        volumeAdjustRow.addView(createStepButton("- 25%") {
            volumePercent = (volumePercent - 25).coerceIn(0, 100)
            volumeDisplay.text = "$volumePercent%"
        }, LinearLayout.LayoutParams(0, -2, 1f).apply { rightMargin = 6 })

        volumeAdjustRow.addView(createStepButton("+ 25%") {
            volumePercent = (volumePercent + 25).coerceIn(0, 100)
            volumeDisplay.text = "$volumePercent%"
        }, LinearLayout.LayoutParams(0, -2, 1f))
        volumeCard.addView(volumeAdjustRow)
        content.addView(volumeCard, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = 14 })

        // 6. Save Button
        val saveButton = Button(this).apply {
            text = getString(R.string.wear_save)
            WearUi.styleActionButton(this@WakeSyncAlarmEditorActivity, this, R.color.dismiss_green)
            setOnClickListener { saveAlarm() }
        }
        content.addView(saveButton, LinearLayout.LayoutParams(-1, -2))

        scroll.addView(content)
        setContentView(scroll)
        refreshDayButtons()
    }

    private fun updateTimeText() {
        timeDisplay.text = String.format(Locale.US, "%02d:%02d", hourValue, minuteValue)
    }

    private fun updateVibrationButtonText() {
        vibrationButton.text = if (vibrationEnabled) getString(R.string.wear_vibration_on) else getString(R.string.wear_vibration_off)
        val colorRes = if (vibrationEnabled) R.color.dismiss_green else R.color.text_muted
        WearUi.styleActionButton(this, vibrationButton, colorRes)
    }

    private fun createStepButton(title: String, onClick: () -> Unit): Button = Button(this).apply {
        text = title
        isAllCaps = false
        textSize = 11f
        WearUi.styleActionButton(this@WakeSyncAlarmEditorActivity, this, R.color.accent_blue)
        setPadding(4, 6, 4, 6)
        setOnClickListener { onClick() }
    }

    private fun createChipButton(title: String, onClick: () -> Unit): Button = Button(this).apply {
        text = title
        isAllCaps = false
        textSize = 11f
        WearUi.styleActionButton(this@WakeSyncAlarmEditorActivity, this, R.color.text_secondary)
        setPadding(4, 6, 4, 6)
        setOnClickListener { onClick() }
    }

    private fun createDayButton(dayIndex: Int, dayName: String): Button = Button(this).apply {
        text = dayName
        isAllCaps = false
        textSize = 12f
        tag = dayIndex
        setPadding(2, 6, 2, 6)
        setOnClickListener {
            if (dayIndex in repeatDays) repeatDays.remove(dayIndex) else repeatDays.add(dayIndex)
            refreshDayButtons()
        }
    }

    private fun refreshDayButtons() {
        dayButtons.forEach { btn ->
            val dayIndex = btn.tag as? Int ?: return@forEach
            val isActive = dayIndex in repeatDays
            val colorRes = if (isActive) R.color.accent_blue else R.color.text_muted
            WearUi.styleActionButton(this, btn, colorRes)
        }
    }

    private fun collectEntry(): WearAlarmListStore.Entry {
        val existing = syncId?.let { id -> WearAlarmListStore.load(this).firstOrNull { it.syncId == id } }
        val now = System.currentTimeMillis()
        return WearAlarmListStore.Entry(
            syncId = existing?.syncId ?: UUID.randomUUID().toString(),
            label = labelValue.trim(),
            hour = hourValue,
            minute = minuteValue,
            enabled = existing?.enabled ?: true,
            repeatDays = repeatDays.toSet(),
            snoozeDurationMinutes = snoozeMinutes,
            vibrationEnabled = vibrationEnabled,
            volume = volumePercent,
            revision = (existing?.revision ?: 0L) + 1L,
            updatedAt = now,
            alarmToken = existing?.alarmToken ?: UUID.randomUUID().toString(),
            source = WearAlarmListStore.SOURCE_WATCH,
            originDeviceId = getSharedPreferences("wakesync_identity", MODE_PRIVATE)
                .getString("device_id", null)
                ?: "WATCH"
        )
    }

    private fun saveAlarm() {
        val existing = syncId?.let { id -> WearAlarmListStore.load(this).firstOrNull { it.syncId == id } }
        val entry = collectEntry()
        WearAlarmListStore.upsert(this, entry)
        val operation = if (existing == null) "CREATE" else "UPDATE"
        WakeSyncPeerController.sendAlarmMutation(this, entry, operation)
        Toast.makeText(
            this,
            if (existing == null) getString(R.string.wear_alarm_saved) else getString(R.string.wear_alarm_updated),
            Toast.LENGTH_SHORT
        ).show()
        finish()
    }

    companion object {
        const val EXTRA_SYNC_ID = "syncId"
    }
}
