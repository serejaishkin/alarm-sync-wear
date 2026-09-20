package com.wakesync.app.data.backup

import com.wakesync.app.data.model.Alarm
import com.wakesync.app.data.preferences.AppSettings
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BackupExportWarningTest {
    @Test
    fun `default settings and alarms do not warn`() {
        val warning = BackupManager.assessExportWarning(
            settings = AppSettings(),
            alarms = listOf(Alarm())
        )

        assertFalse(warning.shouldWarn)
        assertTrue(warning.categories.isEmpty())
    }

    @Test
    fun `settings secrets are disclosed before export`() {
        val warning = BackupManager.assessExportWarning(
            settings = AppSettings(
                hueBridgeIp = "192.168.1.50",
                hueApiKey = "bridge-secret",
                hueLightIds = "1,2"
            ),
            alarms = emptyList()
        )

        assertTrue(warning.shouldWarn)
        assertTrue(warning.categories.contains("Philips Hue bridge details and API key"))
    }

    @Test
    fun `alarm local references and challenge secrets are disclosed before export`() {
        val warning = BackupManager.assessExportWarning(
            settings = AppSettings(),
            alarms = listOf(
                Alarm(
                    ringtoneUri = "content://media/external/audio/media/42",
                    ringtonePool = "android.resource://system/alarm,C:\\Music\\local.mp3",
                    photoMatchUri = "file:///storage/emulated/0/Pictures/wake.jpg",
                    firingBackgroundImageUri = "content://media/external/images/media/77",
                    internetRadioUrl = "https://example.com/stream.m3u8?token=abc",
                    wifiDismissSsid = "Home WiFi",
                    guardianPhone = "+15551234567",
                    locationDismissEnabled = true,
                    locationDismissLat = 32.0,
                    locationDismissLng = -96.0,
                    nfcTagId = "04:a1:b2",
                    barcodeValue = "private-code"
                )
            )
        )

        assertTrue(warning.shouldWarn)
        assertTrue(warning.categories.contains("Internet radio stream URLs"))
        assertTrue(warning.categories.contains("Device-local ringtone or image URIs"))
        assertTrue(warning.categories.contains("Wi-Fi, location, or guardian contact details"))
        assertTrue(warning.categories.contains("NFC or barcode challenge values"))
    }
}
