package com.example.imageclassificationlivefeed.data.services

import android.util.Log
import com.example.imageclassificationlivefeed.apiUrl
import com.example.imageclassificationlivefeed.data.models.AppEntities
import com.example.imageclassificationlivefeed.data.models.ChangeOperations
import com.example.imageclassificationlivefeed.data.models.ChangeResponse
import com.example.imageclassificationlivefeed.data.models.ImageResponse
import com.example.imageclassificationlivefeed.data.models.KnownPersonResponse
import com.example.imageclassificationlivefeed.data.repositories.ImageRepository
import com.example.imageclassificationlivefeed.data.repositories.KnownPersonRepository
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.statement.HttpResponse
import kotlinx.serialization.json.Json

class ChangeService(
    private val http: HttpClient,
    private val knownPersonRepository: KnownPersonRepository,
    private val imageRepository: ImageRepository) {
    private val BASE_RUL = apiUrl + "/Change"
    private val jsonSerializer: Json = Json{ ignoreUnknownKeys = true }

    suspend fun getChangesAndApply(pwadId: String) : Boolean{
        val changes = getRemainingChanges(pwadId)
        val hasChanges  = changes.isNotEmpty()
        if(!hasChanges)
            return false
        changes.filter {
            it.entity == AppEntities.KNOWN_PERSON
        }.forEach {
            applyChange(it)
        }

        changes.filter {
            it.entity == AppEntities.IMAGE
        }.forEach {
            applyChange(it)
        }

        return true

    }

    suspend fun getAllChangesAndApply(pwadId: String) : Boolean{
        val changes = getAllChanges(pwadId)
        val hasChanges  = changes.isNotEmpty()
        if(!hasChanges)
            return false
        changes.filter {
            it.entity == AppEntities.KNOWN_PERSON
        }.forEach {
            applyChange(it)
        }

        changes.filter {
            it.entity == AppEntities.IMAGE
        }.forEach {
            applyChange(it)
        }

        return true

    }


    private suspend fun getAllChanges(pwadId: String) : List<ChangeResponse> {
        return http.get("${BASE_RUL}/Pwad/${pwadId}/All").body()
    }


    private suspend fun getRemainingChanges(pwadId: String) : List<ChangeResponse> {
        return http.get("${BASE_RUL}/Pwad/${pwadId}").body()
    }

    private suspend fun applyChange(change: ChangeResponse){
        when(change.entity){
            AppEntities.KNOWN_PERSON -> {
                when(change.operation){
                    ChangeOperations.CREATE -> {
                        val value = jsonSerializer.decodeFromString<KnownPersonResponse>(change.newValue)
                        knownPersonRepository.save(value)

                    }
                    ChangeOperations.UPDATE -> {
                        val value = jsonSerializer.decodeFromString<KnownPersonResponse>(change.newValue)
                        knownPersonRepository.save(value)

                    }
                    ChangeOperations.DELETE -> {

                    }
                }

            }
            AppEntities.IMAGE -> {
                when(change.operation){
                    ChangeOperations.CREATE -> {
                        Log.d("TESTE", change.newValue)

                        val value = jsonSerializer.decodeFromString<ImageResponse>(change.newValue)
                        imageRepository.save(value)
                    }
                    ChangeOperations.UPDATE -> {
                        val value = jsonSerializer.decodeFromString<ImageResponse>(change.newValue)

                    }
                    ChangeOperations.DELETE -> {

                    }
                }
            }
        }
    }

    suspend fun syncAllChanges(pwadId: String) : HttpResponse{
        return http.post(BASE_RUL + "/Pwad/${pwadId}/SyncAll")
    }

}