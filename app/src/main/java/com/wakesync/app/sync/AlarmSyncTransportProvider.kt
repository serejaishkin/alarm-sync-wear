package com.wakesync.app.sync

import android.content.Context
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Resolves the Wear Data Layer transport without making this file depend on
 * Google Play Services at compile time. Falls back to a no-op transport only
 * when Play Services is genuinely missing at runtime, so a device without GMS
 * degrades instead of crashing.
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
            val clazz = Class.forName("com.wakesync.app.sync.WearDataLayerTransport")
            val constructor = clazz.getConstructor(Context::class.java)
            constructor.newInstance(context) as AlarmSyncTransport
        }.getOrElse { e ->
            Log.w(TAG, "WearDataLayerTransport unavailable (Play Services missing?): ${e.message}")
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
