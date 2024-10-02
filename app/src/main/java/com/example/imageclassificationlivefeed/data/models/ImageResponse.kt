package com.example.imageclassificationlivefeed.data.models

import kotlinx.serialization.Serializable

@Serializable
data class ImageResponse(val id: String, val url: String?, val embedding: String, val knownPersonId: String)