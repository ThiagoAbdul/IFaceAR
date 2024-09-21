package com.example.imageclassificationlivefeed

import android.app.Application
import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import com.example.imageclassificationlivefeed.di.appModule
import com.example.imageclassificationlivefeed.di.networkModule
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_data")


class IFaceARApplication: Application() {

    override fun onCreate() {
        super.onCreate()

        // At the top level of your kotlin file:
        // At the top level of your kotlin file:

        startKoin{
            androidLogger()
            androidContext(this@IFaceARApplication)
            modules(appModule, networkModule)
        }

    }
}