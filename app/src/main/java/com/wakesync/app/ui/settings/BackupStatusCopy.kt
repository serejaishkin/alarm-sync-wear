package com.wakesync.app.ui.settings

import android.content.res.Resources
import com.wakesync.app.R
import java.io.FileNotFoundException
import java.io.IOException
import javax.crypto.AEADBadTagException

internal enum class BackupStatusKind {
    PlainExport,
    EncryptedExport,
    PlainImport,
    EncryptedImport,
    ImportPreview,
    EncryptedImportPreview,
    SupportExport
}

internal data class BackupStatus(
    val message: String,
    val isFailure: Boolean
)

internal fun backupSuccessMessage(resources: Resources, kind: BackupStatusKind, count: Int): BackupStatus {
    val message = when (kind) {
        BackupStatusKind.PlainExport ->
            resources.getQuantityString(R.plurals.backup_exported, count, count)
        BackupStatusKind.EncryptedExport ->
            resources.getQuantityString(R.plurals.backup_exported_encrypted, count, count)
        BackupStatusKind.PlainImport ->
            resources.getQuantityString(R.plurals.backup_imported, count, count)
        BackupStatusKind.EncryptedImport ->
            resources.getQuantityString(R.plurals.backup_imported_encrypted, count, count)
        BackupStatusKind.ImportPreview,
        BackupStatusKind.EncryptedImportPreview,
        BackupStatusKind.SupportExport -> resources.getString(R.string.backup_complete)
    }
    return BackupStatus(message, isFailure = false)
}

internal fun backupFailureMessage(
    resources: Resources,
    kind: BackupStatusKind,
    cause: Throwable? = null
): BackupStatus {
    val prefix = when (kind) {
        BackupStatusKind.PlainExport -> R.string.backup_fail_export
        BackupStatusKind.EncryptedExport -> R.string.backup_fail_export_encrypted
        BackupStatusKind.PlainImport -> R.string.backup_fail_import
        BackupStatusKind.EncryptedImport -> R.string.backup_fail_import_encrypted
        BackupStatusKind.ImportPreview -> R.string.backup_fail_preview
        BackupStatusKind.EncryptedImportPreview -> R.string.backup_fail_preview_encrypted
        BackupStatusKind.SupportExport -> R.string.backup_fail_support
    }
    val message = resources.getString(prefix) + " " + backupRecoveryHint(resources, cause)
    return BackupStatus(message, isFailure = true)
}

private fun backupRecoveryHint(resources: Resources, cause: Throwable?): String {
    val message = cause?.message.orEmpty()
    return when {
        cause is AEADBadTagException ||
            message.contains("passphrase", ignoreCase = true) ||
            message.contains("decrypt", ignoreCase = true) ->
            resources.getString(R.string.backup_hint_passphrase)
        cause is SecurityException ->
            resources.getString(R.string.backup_hint_storage)
        cause is FileNotFoundException ->
            resources.getString(R.string.backup_hint_file_location)
        cause is IOException ->
            resources.getString(R.string.backup_hint_access)
        message.contains("version", ignoreCase = true) ->
            resources.getString(R.string.backup_hint_version)
        message.contains("json", ignoreCase = true) ||
            message.contains("malformed", ignoreCase = true) ||
            message.contains("parse", ignoreCase = true) ->
            resources.getString(R.string.backup_hint_invalid_file)
        else ->
            resources.getString(R.string.backup_hint_destination)
    }
}