package com.wakesync.app.wear

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Typeface
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import com.google.android.gms.wearable.Wearable
import java.util.Locale

/**
 * Wear OS Alarm List Screen for WakeSync.
 * Responsive, circular-screen safe, with real-time sync observation and rotary scrolling.
 */
class WakeSyncAlarmListActivity : Activity() {
    private lateinit var listContainer: LinearLayout
    private lateinit var statusText: TextView
    private lateinit var scrollView: ScrollView
    private val syncHandler = Handler(Looper.getMainLooper())
    private var touchStartY = 0f
    private var touchStartedAtTop = false
    private var pullSyncTriggered = false
    private val periodicSync = object : Runnable {
        override fun run() {
            requestPhoneSnapshot()
            syncHandler.postDelayed(this, SYNC_INTERVAL_MS)
        }
    }

    private val prefsListener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        if (key == "alarms") {
            runOnUiThread { render() }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        buildUi()
    }

    override fun onStart() {
        super.onStart()
        getSharedPreferences(PREFS_ALARMS, Context.MODE_PRIVATE)
            .registerOnSharedPreferenceChangeListener(prefsListener)
        syncHandler.post(periodicSync)
    }

    override fun onStop() {
        super.onStop()
        syncHandler.removeCallbacks(periodicSync)
        getSharedPreferences(PREFS_ALARMS, Context.MODE_PRIVATE)
            .unregisterOnSharedPreferenceChangeListener(prefsListener)
    }

    override fun onResume() {
        super.onResume()
        requestPhoneSnapshot()
        render()
        scrollView.post {
            scrollView.requestFocus()
        }
    }

    private fun buildUi() {
        scrollView = ScrollView(this).apply {
            isFillViewport = true
            isFocusable = true
            isFocusableInTouchMode = true
            setOnTouchListener { _, event ->
                when (event.actionMasked) {
                    MotionEvent.ACTION_DOWN -> {
                        touchStartY = event.rawY
                        touchStartedAtTop = scrollY == 0
                        pullSyncTriggered = false
                    }
                    MotionEvent.ACTION_MOVE -> {
                        if (!pullSyncTriggered && touchStartedAtTop &&
                            event.rawY - touchStartY > PULL_TO_SYNC_DISTANCE
                        ) {
                            pullSyncTriggered = true
                            requestPhoneSnapshot()
                            statusText.text = getString(R.string.wear_syncing)
                        }
                    }
                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                        pullSyncTriggered = false
                    }
                }
                false
            }
            // Rotary knob / crown scroll support for Galaxy Watch and Pixel Watch
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
        WearUi.styleRoot(this, scrollView)

        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            // Ample top/bottom padding to account for circular watch face curvature
            setPadding(24, 24, 24, 36)
        }

        // Header Title
        content.addView(TextView(this).apply {
            text = "WakeSync"
            gravity = Gravity.CENTER
            WearUi.styleHeader(this@WakeSyncAlarmListActivity, this)
        })

        // Subtitle / Status
        statusText = TextView(this).apply {
            text = "Alarms"
            gravity = Gravity.CENTER
            textSize = 12f
            setTextColor(WearUi.color(this@WakeSyncAlarmListActivity, R.color.text_secondary))
            setPadding(0, 2, 0, 10)
        }
        content.addView(statusText)

        // Add Alarm Button
        val addButton = Button(this).apply {
            text = getString(R.string.wear_add_new_alarm)
            WearUi.styleActionButton(this@WakeSyncAlarmListActivity, this, R.color.accent_blue)
            setOnClickListener {
                startActivity(Intent(this@WakeSyncAlarmListActivity, WakeSyncAlarmEditorActivity::class.java))
            }
        }
        content.addView(addButton, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = 12 })

        // Container for alarms
        listContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }
        content.addView(listContainer, LinearLayout.LayoutParams(-1, -2))

        scrollView.addView(content)
        setContentView(scrollView)
    }

    private fun requestPhoneSnapshot() {
        runCatching {
            Wearable.getNodeClient(this).connectedNodes.addOnSuccessListener { nodes ->
                if (nodes.isNotEmpty()) {
                    statusText.text = getString(R.string.wear_synced)
                }
                nodes.forEach { node ->
                    Wearable.getMessageClient(this)
                        .sendMessage(node.id, PATH_REQUEST_SNAPSHOT, ByteArray(0))
                }
            }.addOnFailureListener {
                statusText.text = getString(R.string.wear_offline_mode)
            }
        }
    }

    private fun render() {
        listContainer.removeAllViews()
        val alarms = WearAlarmListStore.load(this)

        if (alarms.isEmpty()) {
            listContainer.addView(TextView(this).apply {
                text = "No alarms\nTap «+ New alarm»"
                gravity = Gravity.CENTER
                setPadding(8, 20, 8, 20)
                textSize = 13f
                setTextColor(WearUi.color(this@WakeSyncAlarmListActivity, R.color.text_muted))
            })
            return
        }

        statusText.text = "Synced to Watch \u2022 ${alarms.size} alarms"

        alarms.forEach { alarm ->
            val time = String.format(Locale.US, "%02d:%02d", alarm.hour, alarm.minute)
            val repeat = repeatLabel(alarm.repeatDays)
            val sourceBadge = if (alarm.source == WearAlarmListStore.SOURCE_WATCH) getString(R.string.wear_source_watch) else getString(R.string.wear_source_phone)

            val card = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                background = WearUi.drawable(this@WakeSyncAlarmListActivity, R.drawable.bg_card_button)
                setPadding(12, 10, 12, 10)
                isClickable = true
                isFocusable = true
                setOnClickListener {
                    startActivity(
                        Intent(this@WakeSyncAlarmListActivity, WakeSyncAlarmEditorActivity::class.java)
                            .putExtra(WakeSyncAlarmEditorActivity.EXTRA_SYNC_ID, alarm.syncId)
                    )
                }
            }

            // Top Row: Time & State
            val topRow = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
            }

            val timeView = TextView(this).apply {
                this.text = time
                textSize = 22f
                setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD))
                setTextColor(
                    WearUi.color(
                        this@WakeSyncAlarmListActivity,
                        if (alarm.enabled) R.color.text_primary else R.color.text_muted
                    )
                )
            }
            topRow.addView(timeView, LinearLayout.LayoutParams(0, -2, 1f))

            val sourceView = TextView(this).apply {
                this.text = sourceBadge
                textSize = 10f
                setTextColor(WearUi.color(this@WakeSyncAlarmListActivity, R.color.text_muted))
            }
            topRow.addView(sourceView)
            card.addView(topRow)

            // Label & Days Row
            val labelText = alarm.label.ifBlank { getString(R.string.wear_default_label) }
            val subtitleView = TextView(this).apply {
                this.text = "$labelText · $repeat"
                textSize = 12f
                setTextColor(WearUi.color(this@WakeSyncAlarmListActivity, R.color.text_secondary))
                setPadding(0, 2, 0, 8)
            }
            card.addView(subtitleView)

            // Bottom Actions Row: Toggle & Delete
            val actions = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
            }

            val toggleBtn = Button(this).apply {
                this.text = if (alarm.enabled) "On" else "Off"
                isAllCaps = false
                textSize = 12f
                val colorRes = if (alarm.enabled) R.color.dismiss_green else R.color.text_muted
                WearUi.styleActionButton(this@WakeSyncAlarmListActivity, this, colorRes)
                setOnClickListener {
                    if (alarm.enabled) {
                        WakeSyncPeerController.sendDisable(this@WakeSyncAlarmListActivity, alarm.syncId, alarm.alarmToken)
                    } else {
                        WakeSyncPeerController.sendEnable(this@WakeSyncAlarmListActivity, alarm.syncId, alarm.alarmToken)
                    }
                    render()
                }
            }
            actions.addView(toggleBtn, LinearLayout.LayoutParams(0, -2, 1f).apply { rightMargin = 6 })

            val deleteBtn = Button(this).apply {
                this.text = "Delete"
                isAllCaps = false
                textSize = 12f
                WearUi.styleActionButton(this@WakeSyncAlarmListActivity, this, R.color.accent_red)
                setOnClickListener {
                    WakeSyncPeerController.sendDelete(this@WakeSyncAlarmListActivity, alarm.syncId)
                    render()
                }
            }
            actions.addView(deleteBtn, LinearLayout.LayoutParams(0, -2, 1f))

            card.addView(actions)
            listContainer.addView(card, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = 8 })
        }
    }

    private fun repeatLabel(days: Set<Int>): String = when {
        days.size == 7 -> getString(R.string.wear_repeat_every_day)
        days == setOf(1, 2, 3, 4, 5) -> getString(R.string.wear_repeat_weekdays)
        days == setOf(6, 7) -> getString(R.string.wear_repeat_weekends)
        days.isEmpty() -> getString(R.string.wear_repeat_once)
        else -> days.sorted().mapNotNull { dayId ->
            val resId = DAY_NAME_IDS[dayId]
            if (resId != null) getString(resId) else null
        }.joinToString(" ")
    }

    companion object {
        private const val PREFS_ALARMS = "wakesync_alarm_list"
        private const val SYNC_INTERVAL_MS = 2_000L
        private const val PULL_TO_SYNC_DISTANCE = 72f
        const val PATH_REQUEST_SNAPSHOT = "/wakesync/alarm/request_snapshot"
        private val DAY_NAME_IDS = mapOf(
            1 to R.string.wear_day_mon, 2 to R.string.wear_day_tue, 3 to R.string.wear_day_wed,
            4 to R.string.wear_day_thu, 5 to R.string.wear_day_fri, 6 to R.string.wear_day_sat,
            7 to R.string.wear_day_sun
        )
    }
}
