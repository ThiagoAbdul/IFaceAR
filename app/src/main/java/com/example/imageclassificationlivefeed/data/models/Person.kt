package com.example.imageclassificationlivefeed.data.models

import kotlinx.serialization.Serializable

@Serializable
data class Person(val firstName: String, val lastName: String) : java.io.Serializable
