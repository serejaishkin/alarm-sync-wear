package com.sysadmindoc.alarmclock.wear

import android.app.Activity
import android.graphics.Typeface
import android.widget.Button
import android.widget.TextView

/** Shared styling helpers mirroring the phone app's dark blue palette. */
object WearUi {
    fun color(activity: Activity, id: Int): Int = activity.getColor(id)
    fun drawable(activity: Activity, id: Int): android.graphics.drawable.Drawable? =
        activity.getDrawable(id)

    fun styleRoot(activity: Activity, view: android.view.View) {
        view.setBackgroundColor(color(activity, R.color.surface_dark))
    }

    fun styleHeader(activity: Activity, view: TextView) {
        view.textSize = 20f
        view.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD))
        view.setTextColor(color(activity, R.color.text_primary))
    }

    fun styleSectionLabel(activity: Activity, view: TextView) {
        view.textSize = 14f
        view.setTextColor(color(activity, R.color.text_secondary))
    }

    /** Big rounded alarm-row / primary-action button, like AlarmCard on the phone. */
    fun styleCardButton(activity: Activity, view: Button, textColorRes: Int = R.color.text_primary) {
        view.isAllCaps = false
        view.textSize = 15f
        view.background = drawable(activity, R.drawable.bg_card_button)
        view.setTextColor(color(activity, textColorRes))
    }

    fun styleActionButton(activity: Activity, view: Button, textColorRes: Int) {
        view.isAllCaps = false
        view.textSize = 14f
        view.background = drawable(activity, R.drawable.bg_accent_button)
        view.setTextColor(color(activity, textColorRes))
    }
}
