package com.tagflow.app.di

import android.content.Context
import androidx.room.Room
import com.tagflow.app.data.AppDatabase
import com.tagflow.app.data.LogDao
import com.tagflow.app.data.TagDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    @Provides
    @Singleton
    fun database(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, "tagflow.db")
            .fallbackToDestructiveMigration()
            .build()

    @Provides
    fun tagDao(db: AppDatabase): TagDao = db.tagDao()

    @Provides
    fun logDao(db: AppDatabase): LogDao = db.logDao()
}
