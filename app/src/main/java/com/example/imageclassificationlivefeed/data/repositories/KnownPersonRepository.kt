package com.example.imageclassificationlivefeed.data.repositories

import com.example.facerecognitionimages.DB.DBHelper
import com.example.imageclassificationlivefeed.data.entities.KnownPersonEntity
import com.example.imageclassificationlivefeed.data.models.KnownPersonResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class KnownPersonRepository(private val dbHelper: DBHelper) {
    suspend fun save(knownPerson: KnownPersonResponse) = withContext(Dispatchers.IO){
        dbHelper.insertKnownPerson(knownPerson.toEntity())
    }
}

fun KnownPersonResponse.toEntity() = KnownPersonEntity(
        id = this.id,
        firstName = this.person.firstName,
        lastName = this.person.lastName,
        description = this.description
 )