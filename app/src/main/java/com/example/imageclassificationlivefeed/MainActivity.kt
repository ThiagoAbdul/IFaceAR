package com.example.imageclassificationlivefeed

import android.Manifest
import android.content.ComponentName
import android.content.ContentValues.TAG
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import android.widget.Button
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.example.facerecognitionimages.DB.DBHelper

import com.example.imageclassificationlivefeed.activities.AugmentedRealityActivity
import com.example.imageclassificationlivefeed.activities.RegisterDeviceActivity
import com.example.imageclassificationlivefeed.data.services.LocationService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject

val userIdKey = stringPreferencesKey("user_id")

class MainActivity : AppCompatActivity() {


    private val dbHelper:DBHelper by inject()
    private var serviceBound = false
    private var isTracking = false
    private var service: LocationService? = null
    var pwadId: String? = null

    private val requestPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
        if (isGranted) {
            // Permissão concedida, inicie a captura de localização
            Log.d(TAG, "Location Permission Granted")
            service?.subscribeToLocationUpdates()
            isTracking = true // Atualiza o estado de rastreamento
        } else {
            // Permissão negada
            //Snackbar.make(findViewById(R.id.activity_main), "Permissão de localização negada", Snackbar.LENGTH_SHORT).show()
        }
    }
    private fun requestLocationPermission() {
        requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
    }

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            val localBinder = binder as LocationService.LocalBinder
            service = localBinder.service
            service?.subscribeToLocationUpdates()
            serviceBound = true

            // Inicia automaticamente a captura de localização
            if (!isTracking) {
                service?.subscribeToLocationUpdates()
                isTracking = true
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            Log.d(TAG, "Service Disconnected")
            serviceBound = false
        }
    }

        override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        isTracking = true
        requestLocationPermission()

        //TODO handling permissions

        if (checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_DENIED || checkSelfPermission(
                (Manifest.permission.WRITE_EXTERNAL_STORAGE)) == PackageManager.PERMISSION_DENIED ||
                (checkSelfPermission (Manifest.permission.ACCESS_FINE_LOCATION)) == PackageManager.PERMISSION_DENIED||
                (checkSelfPermission (Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_DENIED)
        ) {
            val permission =
                arrayOf(Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION)
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
                    pwadId = it[userIdKey]
                    if(pwadId != null){
                        try{

                        val intent = Intent(this@MainActivity, LocationService::class.java)
                        intent.putExtra("pwad", pwadId)
                        bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
                        startActivity(Intent(this@MainActivity, AugmentedRealityActivity::class.java))

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
                    dbHelper.clear()
                }
            }
        }

    }

}