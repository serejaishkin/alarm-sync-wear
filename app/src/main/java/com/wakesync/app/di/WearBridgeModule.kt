package com.wakesync.app.di

import com.wakesync.app.wear.DataLayerWearNextAlarmBridge
import com.wakesync.app.wear.WearNextAlarmBridge
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class WearBridgeModule {
    @Binds
    @Singleton
    abstract fun bindWearNextAlarmBridge(
        impl: DataLayerWearNextAlarmBridge
    ): WearNextAlarmBridge
}
