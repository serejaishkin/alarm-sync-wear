package com.wakesync.app.di

import com.wakesync.app.data.health.HealthConnectSleepRepository
import com.wakesync.app.data.health.PlayHealthConnectSleepRepository
import com.wakesync.app.ui.alarmfiring.challenges.DigitalInkChallengeRecognizer
import com.wakesync.app.ui.alarmfiring.challenges.PlayDigitalInkChallengeRecognizer
import com.wakesync.app.wear.PlayWearNextAlarmBridge
import com.wakesync.app.wear.WearNextAlarmBridge
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

object PlayFlavorModule {
    const val FLAVOR = "play"
}

@Module
@InstallIn(SingletonComponent::class)
abstract class PlayFlavorBindings {
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
