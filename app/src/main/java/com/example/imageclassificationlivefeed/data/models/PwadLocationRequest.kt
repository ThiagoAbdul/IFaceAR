package com.example.imageclassificationlivefeed.data.models

import kotlinx.serialization.Serializable

@Serializable
data class PwadLocationRequest(val userId: String, val lat: Double, val lon: Double)