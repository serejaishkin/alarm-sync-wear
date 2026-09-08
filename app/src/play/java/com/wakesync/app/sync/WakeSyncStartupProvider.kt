package com.wakesync.app.sync

import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.net.Uri
import androidx.annotation.Keep
import dagger.hilt.EntryPoint
import dagger.hilt.EntryPoints
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Keep
class WakeSyncStartupProvider : ContentProvider() {
    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface StartupEntryPoint {
        fun coordinator(): AlarmSyncCoordinator
    }

    override fun onCreate(): Boolean {
        val app = context?.applicationContext ?: return false
        EntryPoints.get(app, StartupEntryPoint::class.java).coordinator().start()
        WakeSyncPeriodicReceiver.schedule(app)
        return true
    }

    override fun query(uri: Uri, projection: Array<String>?, selection: String?, selectionArgs: Array<String>?, sortOrder: String?): Cursor? = null
    override fun getType(uri: Uri): String? = null
    override fun insert(uri: Uri, values: ContentValues?): Uri? = null
    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<String>?): Int = 0
    override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<String>?): Int = 0
}
