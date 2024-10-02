package com.example.facerecognitionimages.DB

import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.util.Log
import com.example.facerecognitionimages.face_recognition.FaceClassifier.Recognition
import com.example.imageclassificationlivefeed.data.entities.ImageEntity
import com.example.imageclassificationlivefeed.data.entities.KnownPersonEntity

class DBHelper(context: Context?) :
    SQLiteOpenHelper(context, DATABASE_NAME, null, 1) {
    override fun onCreate(db: SQLiteDatabase) {
        // TODO Auto-generated method stub
        db.execSQL(
            "create table knownPersons" +
                    "(id text primary key, firstName text, lastName text, description text)"
        )
        db.execSQL(
            "create table faces " +
                    "(id text primary key, embedding text, knownPersonId text," +
                    "FOREIGN KEY (knownPersonId) REFERENCES knownPerson(id))"
        )
    }



    init {
     INSTANCE = this
    }


    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // TODO Auto-generated method stub
        db.execSQL("DROP TABLE IF EXISTS faces")
        db.execSQL("DROP TABLE IF EXISTS knownPersons")

        onCreate(db)
    }

    fun insertKnownPerson(knownPerson: KnownPersonEntity){


        if(hasKnownPersonWithId(knownPerson.id))
            return

        val db = this.writableDatabase

        val contentValues = ContentValues()
        contentValues.put("firstName", knownPerson.firstName)
        contentValues.put("lastname", knownPerson.lastName)
        contentValues.put("description", knownPerson.description)
        contentValues.put("id", knownPerson.id)
        db.insert("knownPersons", null, contentValues)
    }

    fun insertFace(imageEntity: ImageEntity): Boolean {


        val db = this.writableDatabase



        val contentValues = ContentValues()
        contentValues.put(FACE_COLUMN_EMBEDDING, imageEntity.embedding)
        contentValues.put("knownPersonId", imageEntity.knownPersonId)
        db.insert("faces", null, contentValues)
        return true
    }

    private fun hasKnownPersonWithId(id: String) : Boolean {
        val db = this.readableDatabase
        val res = db.rawQuery("select * from knownPersons where id='$id'", null)
        return !res.isAfterLast
    }
//
//    fun getData(id: Int): Cursor {
//        val db = this.readableDatabase
//        return db.rawQuery("select * from faces where id=$id", null)
//    }
//
//    fun numberOfRows(): Int {
//        val db = this.readableDatabase
//        return DatabaseUtils.queryNumEntries(db, FACE_TABLE_NAME).toInt()
//    }
//
//    fun updateFace(id: Int?, name: String?, embedding: String?): Boolean {
//        val db = this.writableDatabase
//        val contentValues = ContentValues()
//        contentValues.put(FACE_COLUMN_NAME, name)
//        contentValues.put(FACE_COLUMN_EMBEDDING, embedding)
//        db.update(
//            FACE_TABLE_NAME, contentValues, "id = ? ", arrayOf(
//                Integer.toString(
//                    id!!
//                )
//            )
//        )
//        return true
//    }
//
//    fun deleteFace(id: Int?): Int {
//        val db = this.writableDatabase
//        return db.delete(
//            FACE_TABLE_NAME,
//            "id = ? ", arrayOf(Integer.toString(id!!))
//        )
//    }

    @SuppressLint("Range")
    fun getAllFaces(): List<Recognition>{
            //hp = new HashMap();
            val db = this.readableDatabase
            val res = db.rawQuery("select * from faces f JOIN knownPersons k on f.knownPersonId = k.id", null)
            res.moveToFirst()
            val registered: MutableList<Recognition> = mutableListOf()
            while (!res.isAfterLast) {

                val embeddingString = res.getString(res.getColumnIndex(FACE_COLUMN_EMBEDDING))
                val stringList = embeddingString.split(",".toRegex()).dropLastWhile { it.isEmpty() }
                    .toTypedArray()
                val embeddingFloat = ArrayList<Float>()
                for (s in stringList) {
                    embeddingFloat.add(s.toFloat())
                }
                val bigArray = Array(1) {
                    FloatArray(
                        1
                    )
                }
                val floatArray = FloatArray(embeddingFloat.size)
                for (i in embeddingFloat.indices) {
                    floatArray[i] = embeddingFloat[i]
                }
                bigArray[0] = floatArray
                // embeddingFloat.remove(embeddingFloat.size()-1);

                val recognition =
                    Recognition(res.getString(res.getColumnIndex("firstName")), res.getString(res.getColumnIndex("description")), bigArray)
                registered.add(recognition)
                res.moveToNext()
            }


            return registered
        }

    fun clear(){
        val db = this.writableDatabase
        db.execSQL("delete from faces")
        db.execSQL("delete from knownPersons")

    }

    companion object {
        const val DATABASE_NAME = "iface10.db"
        const val FACE_TABLE_NAME = "faces"
        const val FACE_COLUMN_ID = "id"
        const val FACE_COLUMN_NAME = "name"
        const val FACE_COLUMN_EMBEDDING = "embedding"
        private var INSTANCE: DBHelper? = null

    }
}