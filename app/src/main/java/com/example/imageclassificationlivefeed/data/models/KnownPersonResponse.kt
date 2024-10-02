package com.example.imageclassificationlivefeed.data.models

import kotlinx.serialization.Serializable

@Serializable
data class KnownPersonResponse(val id: String, val person: Person, val pwadId: String, val description: String)