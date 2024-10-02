package com.example.imageclassificationlivefeed.data.repositories

import com.example.facerecognitionimages.DB.DBHelper
import com.example.imageclassificationlivefeed.data.entities.ImageEntity
import com.example.imageclassificationlivefeed.data.models.ImageResponse

class ImageRepository(private val dbHelper: DBHelper) {

    fun save(image: ImageResponse){
        dbHelper.insertFace(image.toEntity())
    }



}

fun ImageResponse.toEntity(): ImageEntity = ImageEntity(
    id = this.id,
    embedding = this.embedding,
    knownPersonId = this.knownPersonId
)