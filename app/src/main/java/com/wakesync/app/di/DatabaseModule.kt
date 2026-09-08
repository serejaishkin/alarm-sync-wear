package com.wakesync.app.di

import android.content.Context
import androidx.room.Room
import com.wakesync.app.data.local.ActigraphySessionDao
import com.wakesync.app.data.local.AlarmDao
import com.wakesync.app.data.local.AlarmDatabase
import com.wakesync.app.data.local.AlarmEventDao
import com.wakesync.app.data.local.AlarmIncidentEventDao
import com.wakesync.app.data.local.PreSleepTagDao
import com.wakesync.app.data.local.SnoreEventDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AlarmDatabase {
        return Room.databaseBuilder(
            context,
            AlarmDatabase::class.java,
            "alarm_clock.db"
        )
            .addMigrations(*AlarmDatabase.ALL_MIGRATIONS)
            .build()
    }

    @Provides
    @Singleton
    fun provideAlarmDao(database: AlarmDatabase): AlarmDao {
        return database.alarmDao()
    }

    @Provides
    @Singleton
    fun provideAlarmEventDao(database: AlarmDatabase): AlarmEventDao {
        return database.alarmEventDao()
    }

    @Provides
    @Singleton
    fun provideActigraphySessionDao(database: AlarmDatabase): ActigraphySessionDao {
        return database.actigraphySessionDao()
    }

    @Provides
    @Singleton
    fun provideAlarmIncidentEventDao(database: AlarmDatabase): AlarmIncidentEventDao {
        return database.alarmIncidentEventDao()
    }

    @Provides
    @Singleton
    fun provideSnoreEventDao(database: AlarmDatabase): SnoreEventDao {
        return database.snoreEventDao()
    }

    @Provides
    @Singleton
    fun providePreSleepTagDao(database: AlarmDatabase): PreSleepTagDao {
        return database.preSleepTagDao()
    }
}
