package com.sysadmindoc.alarmclock.sync

import android.content.Context
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Resolves the Play-flavor Wear Data Layer transport without making the core
 * source set depend on Google Play Services. F-Droid simply gets a no-op
 * transport until a direct BLE transport is installed.
 */
@Singleton
class AlarmSyncTransportProvider @Inject constructor(
    @ApplicationContext private val context: Context
) {
    @Volatile
    private var cached: AlarmSyncTransport? = null

    fun transport(): AlarmSyncTransport {
        cached?.let { return it }
        return synchronized(this) {
            cached ?: createTransport().also { cached = it }
        }
    }

    private fun createTransport(): AlarmSyncTransport {
        return runCatching {
            val clazz = Class.forName("com.sysadmindoc.alarmclock.sync.WearDataLayerTransport")
            val constructor = clazz.getConstructor(Context::class.java)
            constructor.newInstance(context) as AlarmSyncTransport
        }.getOrElse { e ->
            Log.w(TAG, "WearDataLayerTransport not available (fdroid build?): ${e.message}")
            NoOpAlarmSyncTransport
        }
    }

    private object NoOpAlarmSyncTransport : AlarmSyncTransport {
        override suspend fun send(envelope: AlarmSyncEnvelope): Result<Unit> =
            Result.failure(IllegalStateException("Wear Data Layer transport is unavailable"))
    }

    companion object {
        private const val TAG = "AlarmSync"
    }
}
