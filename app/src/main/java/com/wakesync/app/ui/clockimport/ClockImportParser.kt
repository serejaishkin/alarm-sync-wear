package com.wakesync.app.ui.clockimport

/**
 * Forgiving time parser for the "move alarms from Clock" screen, where the user
 * retypes times they read off another clock app. Accepts "7:30", "07.30",
 * "730", "0730", "7" (hour only) and 12-hour forms such as "7:30 pm".
 */
object ClockImportParser {

    private val separated = Regex("""^(\d{1,2})[:.,\s-]+(\d{1,2})$""")

    /** @return hour (0..23) to minute (0..59), or null when the text is not a valid time. */
    fun parseTime(raw: String): Pair<Int, Int>? {
        var text = raw.trim().lowercase()
        if (text.isEmpty()) return null

        var meridiem: String? = null
        for (suffix in listOf("am", "pm")) {
            if (text.endsWith(suffix)) {
                meridiem = suffix
                text = text.removeSuffix(suffix).trim()
                break
            }
        }
        if (text.isEmpty()) return null

        val hour: Int
        val minute: Int
        val match = separated.matchEntire(text)
        if (match != null) {
            hour = match.groupValues[1].toInt()
            minute = match.groupValues[2].toInt()
        } else {
            if (!text.all { it.isDigit() }) return null
            when (text.length) {
                1, 2 -> { hour = text.toInt(); minute = 0 }
                3 -> { hour = text.substring(0, 1).toInt(); minute = text.substring(1).toInt() }
                4 -> { hour = text.substring(0, 2).toInt(); minute = text.substring(2).toInt() }
                else -> return null
            }
        }
        if (minute !in 0..59) return null

        val resolvedHour = when (meridiem) {
            null -> hour
            else -> {
                if (hour !in 1..12) return null
                val base = hour % 12
                if (meridiem == "pm") base + 12 else base
            }
        }
        if (resolvedHour !in 0..23) return null
        return resolvedHour to minute
    }
}
