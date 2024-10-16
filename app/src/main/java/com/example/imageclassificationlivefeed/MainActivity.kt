package com.example.imageclassificationlivefeed

import android.Manifest
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.ContentValues.TAG
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import android.view.View
import android.widget.Button
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.example.facerecognitionimages.DB.DBHelper

import com.example.imageclassificationlivefeed.activities.AugmentedRealityActivity
import com.example.imageclassificationlivefeed.activities.RegisterDeviceActivity
import com.example.imageclassificationlivefeed.data.services.ChangeService
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import java.util.Locale

val userIdKey = stringPreferencesKey("user_id")

class MainActivity : AppCompatActivity() {


    private val dbHelper:DBHelper by inject()
    private val changeService: ChangeService by inject ()
    private var serviceBound = false
    private var isTracking = false
    private var service: LocationService? = null

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
        requestPermissionLauncher.launch(android.Manifest.permission.ACCESS_FINE_LOCATION)
    }
    private fun getAddressFromLocation(latitude: Double, longitude: Double, context: Context): String {
        val geocoder = Geocoder(context, Locale.getDefault())
        return try {
            val addresses = geocoder.getFromLocation(latitude, longitude, 1)
            if (addresses != null && addresses.isNotEmpty()) {
                val address = addresses[0]
                val city = address.locality ?: "Cidade não encontrada"
                val neighborhood = address.subLocality ?: "Bairro não encontrado"
                "$neighborhood, $city"
            } else {
                "Endereço não encontrado"
            }
        } catch (e: Exception) {
            e.printStackTrace()
            "Erro ao buscar endereço"
        }
    }
    private val locationReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            Log.d(TAG, "Broadcast Received")
            val location = intent?.getParcelableExtra<Location>(LocationService.EXTRA_LOCATION)

            if (location != null) {
                val latitude = location.latitude
                val longitude = location.longitude
                Log.d(TAG, "Attempting to update TextView with Location: Lat: $latitude, Long: $longitude")

                runOnUiThread {
                    Log.d(TAG, "Lat: $latitude, Long: $longitude")
                    Log.d(TAG, "Update complete")
                }
            } else {
                Log.d(TAG, "No location found in the intent")
            }
        }
    }

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            Log.d(TAG, "Service Connected")
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
        Log.d(TAG, "Subscribed Sucessfully")

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
                    dbHelper.clear()
                }
            }
        }

    }
    override fun onStart() {
        super.onStart()
        // Registra o receiver
        val filter = IntentFilter(LocationService.LOCATION_BROADCAST)
        LocalBroadcastManager.getInstance(this).registerReceiver(locationReceiver, filter)
        Log.d(TAG, "BroadcastReceiver Registered")

        // Vincula o serviço
        val intent = Intent(this, LocationService::class.java)
        bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
    }

}