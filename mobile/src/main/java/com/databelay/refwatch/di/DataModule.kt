package com.databelay.refwatch.di

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DataModule {

    @Provides
    @Singleton
    fun provideSharedPreferences(app: Application): SharedPreferences {
        // Use the package name as a standard and safe way to name your prefs file.
        return app.getSharedPreferences(app.packageName + "_prefs", Context.MODE_PRIVATE)
    }
}
