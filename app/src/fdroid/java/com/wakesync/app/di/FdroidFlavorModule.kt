package com.wakesync.app.di

import com.wakesync.app.data.health.FdroidHealthConnectSleepRepository
import com.wakesync.app.data.health.HealthConnectSleepRepository
import com.wakesync.app.ui.alarmfiring.challenges.DigitalInkChallengeRecognizer
import com.wakesync.app.ui.alarmfiring.challenges.FdroidDigitalInkChallengeRecognizer
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

object FdroidFlavorModule {
    const val FLAVOR = "fdroid"
}

@Module
@InstallIn(SingletonComponent::class)
abstract class FdroidFlavorBindings {
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
