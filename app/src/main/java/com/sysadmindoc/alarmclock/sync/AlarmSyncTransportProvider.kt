package com.sysadmindoc.alarmclock.sync

import android.content.Context
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Resolves the Play-flavor Wear Data Layer transport without making the core
 * source set depend on Google Play Services. F-Droid simply gets a no-op
 * transport until a direct BLE transport is installed.
 */
@Singleton
class AlarmSyncTransportProvider @Inject constructor(
    private val context: Context
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
        }.getOrElse { NoOpAlarmSyncTransport }
    }

    private object NoOpAlarmSyncTransport : AlarmSyncTransport {
        override suspend fun send(envelope: AlarmSyncEnvelope): Result<Unit> =
            Result.failure(IllegalStateException("Wear Data Layer transport is unavailable"))
    }
}
