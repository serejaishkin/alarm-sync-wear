package com.wakesync.app.di

import com.wakesync.app.data.health.HealthConnectSleepRepository
import com.wakesync.app.data.health.PlayHealthConnectSleepRepository
import com.wakesync.app.ui.alarmfiring.challenges.DigitalInkChallengeRecognizer
import com.wakesync.app.ui.alarmfiring.challenges.PlayDigitalInkChallengeRecognizer
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
    // The Wear OS Data Layer bridge moved to the shared source set
    // (WearBridgeModule) so every flavor can sync with the watch. Re-enabling
    // the play flavor must not add a second WearNextAlarmBridge binding here.

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
