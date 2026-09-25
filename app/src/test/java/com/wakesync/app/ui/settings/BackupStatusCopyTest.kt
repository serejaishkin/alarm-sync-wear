package com.wakesync.app.ui.settings

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import java.io.FileNotFoundException
import java.io.IOException
import javax.crypto.AEADBadTagException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [30])
class BackupStatusCopyTest {
    private val resources = ApplicationProvider.getApplicationContext<Context>().resources

    @Test
    fun successMessagesUsePlainHumanCounts() {
        val plainExport = backupSuccessMessage(resources, BackupStatusKind.PlainExport, 1)
        assertEquals("Backup exported: 1 alarm.", plainExport.message)
        assertFalse(plainExport.isFailure)

        val plainImport = backupSuccessMessage(resources, BackupStatusKind.PlainImport, 3)
        assertEquals("Backup imported: 3 alarms.", plainImport.message)
        assertFalse(plainImport.isFailure)

        val encryptedExport = backupSuccessMessage(resources, BackupStatusKind.EncryptedExport, 2)
        assertEquals("Encrypted backup exported: 2 alarms.", encryptedExport.message)
        assertFalse(encryptedExport.isFailure)
    }

    @Test
    fun failureMessagesAvoidRawExceptionDumping() {
        val passphrase = backupFailureMessage(resources, BackupStatusKind.EncryptedImportPreview, AEADBadTagException("mac check failed"))
        assertEquals(
            "Couldn\'t preview encrypted backup. Check the passphrase and choose the encrypted backup again.",
            passphrase.message
        )
        assertTrue(passphrase.isFailure)

        val fileLocation = backupFailureMessage(resources, BackupStatusKind.PlainImport, FileNotFoundException("/storage/raw/path"))
        assertEquals(
            "Couldn\'t import backup. Choose a file location this device can still access.",
            fileLocation.message
        )
        assertTrue(fileLocation.isFailure)

        val access = backupFailureMessage(resources, BackupStatusKind.PlainExport, IOException("disk full"))
        assertEquals(
            "Couldn\'t export backup. Check storage access and try again.",
            access.message
        )
        assertTrue(access.isFailure)
    }

    @Test
    fun failureFlagIsSet() {
        assertTrue(backupFailureMessage(resources, BackupStatusKind.PlainExport).isFailure)
        assertFalse(backupSuccessMessage(resources, BackupStatusKind.SupportExport, 0).isFailure)
    }
}