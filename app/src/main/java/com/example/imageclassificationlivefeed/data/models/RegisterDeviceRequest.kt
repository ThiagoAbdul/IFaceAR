package com.example.imageclassificationlivefeed.data.models

import kotlinx.serialization.Serializable

@Serializable
data class RegisterDeviceRequest(val carefulToken: String, val deviceToken: String) {
}