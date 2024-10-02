package com.example.imageclassificationlivefeed.data.models

import kotlinx.serialization.Serializable

@Serializable
data class ChangeResponse(val id: String,
                          val entity: Int,
                          val registerId: String,
                          val operation: Int,
                          val newValue: String)