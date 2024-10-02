package com.example.imageclassificationlivefeed.di

import androidx.room.Room
import com.example.facerecognitionimages.DB.DBHelper
import com.example.imageclassificationlivefeed.data.repositories.ImageRepository
import com.example.imageclassificationlivefeed.data.repositories.KnownPersonRepository
import com.example.imageclassificationlivefeed.data.services.ChangeService
import com.example.imageclassificationlivefeed.data.services.PwadService
import io.ktor.client.HttpClient
import io.ktor.client.engine.android.Android
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.module

val appModule = module {

    singleOf(::PwadService)
    singleOf(::ChangeService)
}

val networkModule = module {
    single {
        HttpClient(Android) {
            install(Logging) {
                level = LogLevel.ALL
            }
            install(ContentNegotiation) {
                json(Json {
                    ignoreUnknownKeys = true
                })
            }
        }
    }
}

val storageModule = module {

    single {
        DBHelper(androidContext())
    }


    singleOf(::KnownPersonRepository)

    singleOf(::ImageRepository)
}