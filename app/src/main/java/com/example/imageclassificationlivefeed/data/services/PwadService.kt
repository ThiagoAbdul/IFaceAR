package com.example.imageclassificationlivefeed.data.services

import com.example.imageclassificationlivefeed.apiUrl
import com.example.imageclassificationlivefeed.data.models.PwadResponse
import com.example.imageclassificationlivefeed.data.models.RegisterDeviceRequest
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.utils.EmptyContent.contentType
import io.ktor.http.ContentType
import io.ktor.http.contentType
import java.util.UUID

class PwadService(private val http: HttpClient) {


    val BASE_URL = apiUrl + "/Pwad"
    suspend fun registerDevice(carefulToken: String) : PwadResponse{
        val request = RegisterDeviceRequest(carefulToken, UUID.randomUUID().toString())
        return http.post(BASE_URL + "/RegisterDevice"){
            contentType(ContentType.Application.Json)
            setBody(request)
        }.body()
    }

    private fun setBody(request: Any) {

    }

}