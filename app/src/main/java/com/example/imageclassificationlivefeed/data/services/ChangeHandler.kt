package com.example.imageclassificationlivefeed.data.services

import com.example.imageclassificationlivefeed.data.models.AppEntities
import com.example.imageclassificationlivefeed.data.models.ChangeOperations
import com.example.imageclassificationlivefeed.data.models.ChangeResponse

interface ChangeHandler {
    fun applyChange(change: ChangeResponse)
}

class KnownPersonChangeHandler() : ChangeHandler {
    override fun applyChange(change: ChangeResponse){
        when(change.operation){
            ChangeOperations.CREATE -> {

            }
            ChangeOperations.UPDATE -> {

            }
            ChangeOperations.DELETE -> {

            }
        }
    }
}

class ImageChangeHandler() : ChangeHandler {
    override fun applyChange(change: ChangeResponse) {
        TODO("Not yet implemented")
    }

}

fun changeHandlerFactory(change: ChangeResponse) : ChangeHandler{
    return when(change.entity) {
        AppEntities.KNOWN_PERSON -> KnownPersonChangeHandler()
        AppEntities.IMAGE -> ImageChangeHandler()
        else -> throw Exception("AppEntity not found")
    }
}


