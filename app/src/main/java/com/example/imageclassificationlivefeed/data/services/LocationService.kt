package com.example.imageclassificationlivefeed.data.services

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.location.Location
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.util.Supplier
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.example.imageclassificationlivefeed.MainActivity
import com.example.imageclassificationlivefeed.R
import com.example.imageclassificationlivefeed.data.models.PwadLocationRequest
import com.example.imageclassificationlivefeed.dataStore
import com.example.imageclassificationlivefeed.locationApiUrl
import com.example.imageclassificationlivefeed.toText
import com.google.android.gms.location.*
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.Priority.*
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject

// Classe de serviço para localização em tempo real, em foreground e background
class LocationService : Service() {
    private val http: HttpClient by inject()
    private var configurationChange = false
    private var serviceRunningInForeground = false
    private val localBinder = LocalBinder()

    private lateinit var notificationManager: NotificationManager
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    private lateinit var locationRequest: LocationRequest
    private lateinit var locationCallback: LocationCallback
    private var currentLocation: Location? = null
    var pwadId: String? = null


    private suspend fun sendLocation(lat: Double, lon: Double){
        val url = locationApiUrl
        if(pwadId != null) {
            val request = PwadLocationRequest(pwadId!!, lat, lon)
            http.post(url){
                contentType(ContentType.Application.Json)
                setBody(request)
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)

        // Configuração da requisição de localização
        locationRequest = LocationRequest.Builder(PRIORITY_HIGH_ACCURACY, 60_000)
            .setMinUpdateIntervalMillis(5_000)
            .build()



        // Callback para quando uma nova localização é recebida
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                super.onLocationResult(locationResult)
                for (location in locationResult.locations) {
                    CoroutineScope(Dispatchers.IO).launch {
                        sendLocation(location.latitude, location.longitude)

                    }
                }
            }

        }
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        Log.d(TAG, "onStartCommand()")
        val cancelLocationTrackingFromNotification =
            intent.getBooleanExtra(EXTRA_CANCEL_LOCATION_TRACKING_FROM_NOTIFICATION, false)

        if (cancelLocationTrackingFromNotification) {
            unsubscribeToLocationUpdates()
            stopSelf()
        }
        return START_STICKY // Reiniciar o serviço automaticamente após ser terminado pelo sistema
    }

    override fun onBind(intent: Intent): IBinder {
        pwadId = intent.extras?.getString("pwad")
        stopForeground(true)
        serviceRunningInForeground = false
        configurationChange = false
        return localBinder
    }

    override fun onRebind(intent: Intent) {
        stopForeground(true)
        serviceRunningInForeground = false
        configurationChange = false
        super.onRebind(intent)
    }

    @SuppressLint("ForegroundServiceType")
    override fun onUnbind(intent: Intent): Boolean {
        if (!configurationChange) {
            val notification = generateNotification(currentLocation)
            startForeground(NOTIFICATION_ID, notification)
            serviceRunningInForeground = true
        }
        return true
    }

    override fun onDestroy() {
        unsubscribeToLocationUpdates()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        configurationChange = true
    }

    // Inicia o serviço de localização em tempo real
    fun subscribeToLocationUpdates() {
        startService(Intent(applicationContext, LocationService::class.java))

        try {
            fusedLocationProviderClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                null /* Looper */
            )
        } catch (unlikely: SecurityException) {
            Log.e(TAG, "Permissões de localização perdidas. Não foi possível receber atualizações. $unlikely")
        }
    }

    // Para as atualizações de localização
    fun unsubscribeToLocationUpdates() {
        try {
            fusedLocationProviderClient.removeLocationUpdates(locationCallback)
        } catch (unlikely: SecurityException) {
            Log.e(TAG, "Permissões de localização perdidas. Não foi possível remover atualizações. $unlikely")
        }
    }

    // Gera uma notificação para exibir a localização em tempo real
    private fun generateNotification(location: Location?): Notification {
        val mainNotificationText = location?.toText() ?: getString(R.string.permission_denied_explanation)
        val titleText = getString(R.string.app_name)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationChannel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID, titleText, NotificationManager.IMPORTANCE_DEFAULT
            )
            notificationManager.createNotificationChannel(notificationChannel)
        }

        val bigTextStyle = NotificationCompat.BigTextStyle()
            .bigText(mainNotificationText)
            .setBigContentTitle(titleText)







        return NotificationCompat.Builder(applicationContext, NOTIFICATION_CHANNEL_ID)
            .setStyle(bigTextStyle)
            .setContentTitle(titleText)
            .setContentText(mainNotificationText)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setOngoing(true)
            .build()
    }

    inner class LocalBinder : Binder() {
        internal val service: LocationService
            get() = this@LocationService
    }

    companion object {
        private const val TAG = "LocationService"
        private const val NOTIFICATION_ID = 12345678
        private const val NOTIFICATION_CHANNEL_ID = "while_in_use_channel_01"
        const val LOCATION_BROADCAST = "com.example.imageclassificationlivefeed.lOCATION_BROADCAST"
        const val EXTRA_LOCATION = "extra_location"
        const val EXTRA_CANCEL_LOCATION_TRACKING_FROM_NOTIFICATION = "com.example.imageclassificationlivefeed.extra.CANCEL_LOCATION_TRACKING_FROM_NOTIFICATION"
    }
}
