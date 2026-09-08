package com.wakesync.app.di

import com.wakesync.app.data.health.HealthConnectSleepRepository
import com.wakesync.app.data.health.PlayHealthConnectSleepRepository
import com.wakesync.app.service.PlayYouTubeAudioDownloader
import com.wakesync.app.service.PlayYouTubeDownloadInitializer
import com.wakesync.app.service.YouTubeAudioDownloader
import com.wakesync.app.service.YouTubeDownloadInitializer
import com.wakesync.app.ui.alarmfiring.challenges.DigitalInkChallengeRecognizer
import com.wakesync.app.ui.alarmfiring.challenges.PlayDigitalInkChallengeRecognizer
import com.wakesync.app.wear.PlayWearNextAlarmBridge
import com.wakesync.app.wear.WearNextAlarmBridge
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Play Store flavor — wires the real yt-dlp-backed YouTube downloader.
 * The f-droid flavor binds stubs that return "not available in this build".
 */
object PlayFlavorModule {
    const val FLAVOR = "play"
}

@Module
@InstallIn(SingletonComponent::class)
abstract class PlayFlavorBindings {
    @Binds
    @Singleton
    abstract fun bindDownloader(impl: PlayYouTubeAudioDownloader): YouTubeAudioDownloader

    @Binds
    @Singleton
    abstract fun bindInitializer(impl: PlayYouTubeDownloadInitializer): YouTubeDownloadInitializer

    @Binds
    @Singleton
    abstract fun bindWearNextAlarmBridge(impl: PlayWearNextAlarmBridge): WearNextAlarmBridge

    @Binds
    @Singleton
    abstract fun bindHealthConnectSleepRepository(
        impl: PlayHealthConnectSleepRepository
    ): HealthConnectSleepRepository

    @Binds
    @Singleton
    abstract fun bindDigitalInkChallengeRecognizer(
        impl: PlayDigitalInkChallengeRecognizer
    ): DigitalInkChallengeRecognizer
}
