package com.sysadmindoc.alarmclock.sync

import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.net.Uri
import androidx.annotation.Keep
import dagger.hilt.EntryPoint
import dagger.hilt.EntryPointAccessors
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * Starts the phone-side sync observer as soon as the Play app process exists.
 * No UI activity is required for normal alarm edits made while the process is
 * alive; incoming Wear messages are handled independently by the listener.
 */
@Keep
class WakeSyncStartupProvider : ContentProvider() {
    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface EntryPoint {
        fun coordinator(): AlarmSyncCoordinator
    }

    override fun onCreate(): Boolean {
        val app = context?.applicationContext ?: return false
        EntryPointAccessors.fromApplication(app, EntryPoint::class.java)
            .coordinator()
            .start()
        return true
    }

    override fun query(uri: Uri, projection: Array<String>?, selection: String?, selectionArgs: Array<String>?, sortOrder: String?): Cursor? = null
    override fun getType(uri: Uri): String? = null
    override fun insert(uri: Uri, values: ContentValues?): Uri? = null
    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<String>?): Int = 0
    override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<String>?): Int = 0
}
