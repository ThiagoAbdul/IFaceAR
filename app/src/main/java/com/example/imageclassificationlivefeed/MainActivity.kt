package com.example.imageclassificationlivefeed

import android.Manifest
import android.app.Dialog
import android.app.Fragment
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.media.Image.Plane
import android.media.ImageReader
import android.media.ImageReader.OnImageAvailableListener
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.util.Size
import android.util.TypedValue
import android.view.Surface
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.example.facerecognitionimages.DB.DBHelper
import com.example.facerecognitionimages.face_recognition.FaceClassifier
import com.example.facerecognitionimages.face_recognition.TFLiteFaceRecognition
import com.example.imageclassificationlivefeed.Drawing.MultiBoxTracker

import com.example.imageclassificationlivefeed.ImageUtils.convertYUV420ToARGB8888
import com.example.imageclassificationlivefeed.ImageUtils.getTransformationMatrix
import com.example.imageclassificationlivefeed.activities.AugmentedRealityActivity
import com.example.imageclassificationlivefeed.activities.RegisterDeviceActivity
import com.example.imageclassificationlivefeed.data.services.ChangeService
import com.example.objectdetectionlivefeed.Drawing.BorderedText
import com.example.objectdetectionlivefeed.Drawing.OverlayView
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetector
import com.google.mlkit.vision.face.FaceDetectorOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import java.io.IOException

val userIdKey = stringPreferencesKey("user_id")

class MainActivity : AppCompatActivity() {


//    private val dbHelper:DBHelper by inject()
    private val changeService: ChangeService by inject ()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        //TODO handling permissions
        if (checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_DENIED || checkSelfPermission(
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            )
            == PackageManager.PERMISSION_DENIED
        ) {
            val permission =
                arrayOf(Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE)
            requestPermissions(permission, 121)
        }


        val flow: Flow<String?> = dataStore.data.map {
            it[userIdKey]
        }

        CoroutineScope(Dispatchers.Main).launch {
            flow.collect{ userId ->
                if(userId == null){
                    startActivity(Intent(this@MainActivity, RegisterDeviceActivity::class.java))
                }
            }
        }


        setContentView(R.layout.activity_main)

        findViewById<Button>(R.id.init).setOnClickListener {
            CoroutineScope(Dispatchers.IO).launch {
                dataStore.data.collect{
                    val pwadId = it[userIdKey]
                    if(pwadId != null){
                        try{
//                            val hasChanges = changeService.getChangesAndApply(pwadId)
//                            if(hasChanges){
//                                changeService.syncAllChanges(pwadId)
//                            }
//                            delay(500)
                            startActivity(Intent(this@MainActivity, AugmentedRealityActivity::class.java))
//                            faceClassifier?.updateDataSource(dbHelper.allFaces)
                        }
                        catch (exception: Exception){
                            Log.d("ERRO", exception.message.toString())
                        }
                    }
                }
            }
        }

        findViewById<Button>(R.id.reset).setOnClickListener {
            CoroutineScope(Dispatchers.IO).launch {
                dataStore.edit {
                    it.remove(userIdKey)
                }
            }
        }

    }



}