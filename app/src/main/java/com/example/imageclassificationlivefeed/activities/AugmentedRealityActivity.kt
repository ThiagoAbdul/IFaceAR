package com.example.imageclassificationlivefeed.activities

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
import com.example.facerecognitionimages.DB.DBHelper
import com.example.facerecognitionimages.face_recognition.FaceClassifier
import com.example.facerecognitionimages.face_recognition.TFLiteFaceRecognition
import com.example.imageclassificationlivefeed.CameraConnectionFragment
import com.example.imageclassificationlivefeed.Drawing.MultiBoxTracker

import com.example.imageclassificationlivefeed.ImageUtils.convertYUV420ToARGB8888
import com.example.imageclassificationlivefeed.ImageUtils.getTransformationMatrix
import com.example.imageclassificationlivefeed.R
import com.example.imageclassificationlivefeed.data.services.ChangeService
import com.example.imageclassificationlivefeed.dataStore
import com.example.imageclassificationlivefeed.userIdKey
import com.example.objectdetectionlivefeed.Drawing.BorderedText
import com.example.objectdetectionlivefeed.Drawing.OverlayView
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetector
import com.google.mlkit.vision.face.FaceDetectorOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject

class AugmentedRealityActivity : AppCompatActivity(), OnImageAvailableListener {
    var handler: Handler? = null
    private var frameToCropTransform: Matrix? = null
    private var sensorOrientation = 0
    private var cropToFrameTransform: Matrix? = null
    var trackingOverlay: OverlayView? = null
    private var borderedText: BorderedText? = null
    private var tracker: MultiBoxTracker? = null
    private var useFacing: Int? = null
    private val changeService: ChangeService by inject()
    private val dbHelper: DBHelper by inject()
    private var faceDetectionIsActive = false
    var pwadId: String? = null
    //TODO delcare face detector
    var detector: FaceDetector? = null

    //TODO declare face recognizer
    private var faceClassifier: FaceClassifier? = null
    var registerFace = false
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)


//        runBlocking {
//            dataStore.data.collect{
//                val pwadId = it[userIdKey]
//                if(pwadId != null){
//                    try{
//                        val changes = changeService.getChangesAndApply(pwadId)
//                        changeService.syncAllChanges(pwadId)
//                        faceClassifier?.updateDataSource(dbHelper.allFaces)
//                    }
//                    catch (exception: Exception){
//                        Log.d("ERRO", exception.message.toString())
//                    }
//                }
//            }
//        }

        CoroutineScope(Dispatchers.IO).launch {
            dataStore.data.collect {
                pwadId = it[userIdKey]
            }
        }


        setContentView(R.layout.activity_augmented_reality)
        handler = Handler()

        useFacing = CameraCharacteristics.LENS_FACING_FRONT




        //TODO initalize face detector
        // Multiple object detection in static images
        initializeFaceRecognition()

        findViewById<View>(R.id.imageView4).setOnClickListener {
            dbHelper.clear()
        }
        findViewById<View>(R.id.imageView3).setOnClickListener { switchCamera() }
    }

    private fun initializeFaceRecognition(){

        setFragment()


        //TODO initialize the tracker to draw rectangles
        tracker = MultiBoxTracker(this)
        Log.d("TESTE", "C")

        val highAccuracyOpts = FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
            .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_NONE)
            .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_NONE)
            .build()
        detector = FaceDetection.getClient(highAccuracyOpts)


        //TODO initialize FACE Recognition
        try {

            faceClassifier = TFLiteFaceRecognition.create(
                assets,
                "facenet.tflite",
                TF_OD_API_INPUT_SIZE2,
                false, dbHelper
            )
        } catch (e: Exception) {

            e.printStackTrace()
            val toast = Toast.makeText(
                applicationContext, "Classifier could not be initialized", Toast.LENGTH_SHORT
            )
            toast.show()
            finish()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 121 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            setFragment()
        }
    }

    //TODO fragment which show live footage from camera
    var previewHeight = 0
    var previewWidth = 0
    protected fun setFragment() {
        val manager = getSystemService(CAMERA_SERVICE) as CameraManager
        var cameraId: String? = null

        try {
            cameraId = manager.cameraIdList[(useFacing)!!]
        } catch (e: CameraAccessException) {
            e.printStackTrace()
        }
        catch (e: Exception){
            Log.d("TESTE", "F")
        }
        val fragment: Fragment
        val camera2Fragment = CameraConnectionFragment.newInstance(
            object : CameraConnectionFragment.ConnectionCallback {
                override fun onPreviewSizeChosen(size: Size?, cameraRotation: Int) {
                    previewHeight = size!!.height
                    previewWidth = size.width
                    val textSizePx = TypedValue.applyDimension(
                        TypedValue.COMPLEX_UNIT_DIP, TEXT_SIZE_DIP, resources.displayMetrics
                    )
                    borderedText = BorderedText(textSizePx)
                    borderedText!!.setTypeface(Typeface.MONOSPACE)
                    val cropSize = CROP_SIZE
                    previewWidth = size.width
                    previewHeight = size.height
                    sensorOrientation = cameraRotation - screenOrientation
                    rgbFrameBitmap =
                        Bitmap.createBitmap(previewWidth, previewHeight, Bitmap.Config.ARGB_8888)
                    croppedBitmap = Bitmap.createBitmap(cropSize, cropSize, Bitmap.Config.ARGB_8888)
                    frameToCropTransform = getTransformationMatrix(
                        previewWidth, previewHeight,
                        cropSize, cropSize,
                        sensorOrientation, MAINTAIN_ASPECT
                    )
                    cropToFrameTransform = Matrix()
                    frameToCropTransform!!.invert(cropToFrameTransform)
                    trackingOverlay = findViewById<View>(R.id.tracking_overlay) as OverlayView
                    trackingOverlay!!.addCallback(
                        object : OverlayView.DrawCallback {
                            override fun drawCallback(canvas: Canvas?) {
                                if (canvas != null) {
                                    tracker?.draw(canvas)
                                }
                                Log.d("tryDrawRect", "inside draw")
                            }
                        })
                    tracker!!.setFrameConfiguration(previewWidth, previewHeight, sensorOrientation)
                }
            },
            this,
            R.layout.camera_fragment,
            Size(640, 480)
        )
        camera2Fragment.setCamera(cameraId)
        fragment = camera2Fragment
        fragmentManager.beginTransaction().replace(R.id.ar_camera, fragment).commit()
    }

    //TODO getting frames of live camera footage and passing them to model
    private var isProcessingFrame = false
    private val yuvBytes = arrayOfNulls<ByteArray>(3)
    private var rgbBytes: IntArray? = null
    private var yRowStride = 0
    private var postInferenceCallback: Runnable? = null
    private var imageConverter: Runnable? = null
    private var rgbFrameBitmap: Bitmap? = null
    override fun onImageAvailable(reader: ImageReader) {
        // We need wait until we have some size from onPreviewSizeChosen
        if (previewWidth == 0 || previewHeight == 0) {
            return
        }
        if (rgbBytes == null) {
            rgbBytes = IntArray(previewWidth * previewHeight)
        }
        try {
            val image = reader.acquireLatestImage() ?: return
            if (isProcessingFrame) {
                image.close()
                return
            }
            isProcessingFrame = true
            val planes = image.planes
            fillBytes(planes, yuvBytes)
            yRowStride = planes[0].rowStride
            val uvRowStride = planes[1].rowStride
            val uvPixelStride = planes[1].pixelStride
            imageConverter = Runnable {
                convertYUV420ToARGB8888(
                    (yuvBytes[0])!!,
                    (yuvBytes[1])!!,
                    (yuvBytes[2])!!,
                    previewWidth,
                    previewHeight,
                    yRowStride,
                    uvRowStride,
                    uvPixelStride,
                    rgbBytes!!
                )
            }
            postInferenceCallback = Runnable {
                image.close()
                isProcessingFrame = false
            }

                performFaceDetection()
        } catch (e: Exception) {
            Log.d("TESTE", "G" + " " + e.message.toString())
            return
        }
    }

    protected fun fillBytes(planes: Array<Plane>, yuvBytes: Array<ByteArray?>) {
        // Because of the variable row stride it's not possible to know in
        // advance the actual necessary dimensions of the yuv planes.
        for (i in planes.indices) {
            val buffer = planes[i].buffer
            if (yuvBytes[i] == null) {
                yuvBytes[i] = ByteArray(buffer.capacity())
            }
            buffer[yuvBytes[i]]
        }
    }

    protected val screenOrientation: Int
        protected get() = when (windowManager.defaultDisplay.rotation) {
            Surface.ROTATION_270 -> 270
            Surface.ROTATION_180 -> 180
            Surface.ROTATION_90 -> 90
            else -> 0
        }
    var croppedBitmap: Bitmap? = null
    var mappedRecognitions: ArrayList<FaceClassifier.Recognition>? = null

    //TODO Perform face detection
    fun performFaceDetection() {

        Log.d("TESTE", "E")
        imageConverter!!.run()
        rgbFrameBitmap!!.setPixels(rgbBytes!!, 0, previewWidth, 0, 0, previewWidth, previewHeight)
        val canvas = Canvas(croppedBitmap!!)
        canvas.drawBitmap(rgbFrameBitmap!!, frameToCropTransform!!, null)
        Handler().post {
            mappedRecognitions = ArrayList()
            val image = InputImage.fromBitmap((croppedBitmap)!!, 0)
            detector!!.process(image)
                .addOnSuccessListener { faces ->
                    for (face: Face in faces) {
                        performFaceRecognition(face)
                    }
                    registerFace = false
                    tracker!!.trackResults(mappedRecognitions!!, 10)
                    trackingOverlay!!.postInvalidate()
                    postInferenceCallback!!.run()
                }
                .addOnFailureListener {
                    // Task failed with an exception
                    // ...
                }
        }
    }

    //TODO perform face recognition
    fun performFaceRecognition(face: Face) {
        if(!faceDetectionIsActive)
            return
        //TODO crop the face
        val bounds: Rect = face.boundingBox
        if (bounds.top < 0) {
            bounds.top = 0
        }
        if (bounds.left < 0) {
            bounds.left = 0
        }
        if (bounds.left + bounds.width() > croppedBitmap!!.width) {
            bounds.right = croppedBitmap!!.width - 1
        }
        if (bounds.top + bounds.height() > croppedBitmap!!.height) {
            bounds.bottom = croppedBitmap!!.height - 1
        }
        var crop: Bitmap = Bitmap.createBitmap(
            croppedBitmap!!,
            bounds.left,
            bounds.top,
            bounds.width(),
            bounds.height()
        )
        crop = Bitmap.createScaledBitmap(crop, TF_OD_API_INPUT_SIZE2, TF_OD_API_INPUT_SIZE2, false)
        val result: FaceClassifier.Recognition? = faceClassifier!!.recognizeImage(crop, registerFace)
        var title: String = "Unknown"
        var confidence = 0f
        var description: String? = ""
        if (result != null) {

            if (result.distance!! < 0.8f) {
                confidence = result.distance!!
                title = result.title.toString()
                description = result.description
            }

        }
        val location = RectF(bounds)
        if (bounds != null) {
            if (useFacing == CameraCharacteristics.LENS_FACING_BACK) {
                location.right = croppedBitmap!!.width - location.right
                location.left = croppedBitmap!!.width - location.left
            }
            cropToFrameTransform!!.mapRect(location)
            val recognition =
                FaceClassifier.Recognition(
                    face.trackingId.toString() + "",
                    title,
                    description,
                    confidence,
                    location
                )
            mappedRecognitions!!.add(recognition)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        detector!!.close()
//        applicationContext.deleteDatabase(DBHelper.DATABASE_NAME)
//        CoroutineScope(Dispatchers.Main).launch {
//            dataStore.edit {
//                it.remove(userIdKey)
//            }
//        }

    }


    //TODO switch camera
    fun switchCamera() {
//        val intent = intent
//        useFacing = if (useFacing == CameraCharacteristics.LENS_FACING_FRONT) {
//            CameraCharacteristics.LENS_FACING_BACK
//        } else {
//            CameraCharacteristics.LENS_FACING_FRONT
//        }
//        intent.putExtra(KEY_USE_FACING, useFacing)
//        intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION)
//        restartWith(intent)
        this.faceDetectionIsActive = false
        CoroutineScope(Dispatchers.IO).launch {
            dataStore.data.collect{
                if(pwadId != null){
                    try{
                        val changes = changeService.getChangesAndApply(pwadId!!)
                        if(changes){
                            changeService.syncAllChanges(pwadId!!)
                            faceClassifier?.updateDataSource()

                        }
                    }
                    catch (exception: Exception){
                        Log.d("ERRO", exception.message.toString())
                    }

                }
                faceDetectionIsActive = true
            }

        }

    }

    private fun restartWith(intent: Intent) {
        finish()
        overridePendingTransition(0, 0)
        startActivity(intent)
        overridePendingTransition(0, 0)
    }

    companion object {
        private const val MAINTAIN_ASPECT = false
        private const val TEXT_SIZE_DIP = 10f
        private const val KEY_USE_FACING = "use_facing"
        private const val CROP_SIZE = 1000
        private const val TF_OD_API_INPUT_SIZE2 = 160
    }
}