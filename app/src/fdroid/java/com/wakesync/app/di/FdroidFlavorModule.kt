package com.wakesync.app.di

import com.wakesync.app.data.health.FdroidHealthConnectSleepRepository
import com.wakesync.app.data.health.HealthConnectSleepRepository
import com.wakesync.app.service.FdroidYouTubeAudioDownloader
import com.wakesync.app.service.FdroidYouTubeDownloadInitializer
import com.wakesync.app.service.YouTubeAudioDownloader
import com.wakesync.app.service.YouTubeDownloadInitializer
import com.wakesync.app.ui.alarmfiring.challenges.DigitalInkChallengeRecognizer
import com.wakesync.app.ui.alarmfiring.challenges.FdroidDigitalInkChallengeRecognizer
import com.wakesync.app.wear.FdroidWearNextAlarmBridge
import com.wakesync.app.wear.WearNextAlarmBridge
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * F-Droid flavor - no proprietary dependencies allowed. The yt-dlp downloader
 * is stubbed out; the UI checks `isAvailable()` and hides the entry point.
 */
object FdroidFlavorModule {
    const val FLAVOR = "fdroid"
}

@Module
@InstallIn(SingletonComponent::class)
abstract class FdroidFlavorBindings {
    @Binds
    @Singleton
    abstract fun bindDownloader(impl: FdroidYouTubeAudioDownloader): YouTubeAudioDownloader

    @Binds
    @Singleton
    abstract fun bindInitializer(impl: FdroidYouTubeDownloadInitializer): YouTubeDownloadInitializer

    @Binds
    @Singleton
    abstract fun bindWearNextAlarmBridge(impl: FdroidWearNextAlarmBridge): WearNextAlarmBridge

    @Binds
    @Singleton
    abstract fun bindHealthConnectSleepRepository(
        impl: FdroidHealthConnectSleepRepository
    ): HealthConnectSleepRepository

    @Binds
    @Singleton
    abstract fun bindDigitalInkChallengeRecognizer(
        impl: FdroidDigitalInkChallengeRecognizer
    ): DigitalInkChallengeRecognizer
}
