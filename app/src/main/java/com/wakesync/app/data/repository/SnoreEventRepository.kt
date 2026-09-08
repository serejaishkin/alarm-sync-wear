package com.wakesync.app.data.repository

import com.wakesync.app.data.local.SnoreEventDao
import com.wakesync.app.data.local.entity.SnoreEvent
import com.wakesync.app.domain.SnoreEventCandidate
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SnoreEventRepository @Inject constructor(
    private val dao: SnoreEventDao
) {
    fun observeRecent(limit: Int = 12): Flow<List<SnoreEvent>> = dao.observeRecent(limit)

    suspend fun getRecent(limit: Int = 12): List<SnoreEvent> = dao.getRecent(limit)

    suspend fun recordAll(
        sessionStartedAt: Long,
        events: List<SnoreEventCandidate>,
        source: String = SOURCE_SONAR_MIC
    ): List<Long> {
        dao.deleteOlderThan(System.currentTimeMillis() - RETENTION_MS)
        if (events.isEmpty()) return emptyList()
        val rows = events.map { event ->
            SnoreEvent(
                sessionStartedAt = sessionStartedAt,
                startedAt = event.startedAt,
                endedAt = event.endedAt,
                durationMillis = event.durationMillis,
                peakDb = event.peakDb,
                averageDb = event.averageDb,
                windowCount = event.windowCount,
                source = source
            )
        }
        return dao.insertAll(rows)
    }

    private companion object {
        const val SOURCE_SONAR_MIC = "SONAR_MIC"
        const val RETENTION_MS = 30L * 24L * 60L * 60L * 1000L
    }
}
