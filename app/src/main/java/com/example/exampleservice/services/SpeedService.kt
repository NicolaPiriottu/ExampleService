package com.example.exampleservice.services

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.BitmapFactory
import android.location.LocationManager
import android.os.*
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.exampleservice.MainActivity
import com.example.exampleservice.broadcastreceivers.CheckGPS
import com.google.android.gms.location.*
import kotlin.math.roundToInt

/**
 * Created by Nicola Luigi Piriottu on 13/06/22.
 */
class SpeedService : Service() {


    //https://medium.com/koahealth/building-an-android-service-that-never-stops-running-5868f304724b

    enum class ActionsType {
        START,
        STOP
    }

    /**
     * Variables for Broadcast Service
     */
    companion object {
        const val ON_STATE_SERVICE = "ON_STATE_SERVICE"
        const val SET_CURRENT_POSITION = "SET_CURRENT_POSITION"
        const val CHECK_GPS = "CHECK_GPS"

        const val INTENT_ON_STATE_SERVICE = "intent.service.stop"
        const val INTENT_SET_CURRENT_POSITION = "intent.service.set.current.position"
        const val INTENT_CHECK_GPS = "intent.service.check.gps"
    }

    /**
     * Broadcast GPS
     */
    private lateinit var broadcastGps: CheckGPS

    /**
     * WakeLock
     */
    private var wakeLock: PowerManager.WakeLock? = null

    /**
     * Location
     */
    private lateinit var locationRequest: LocationRequest
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient

    private var actualSpeed: Int = 0

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        if (intent != null) {

            when (intent.action) {
                ActionsType.START.name -> startLocationUpdates()
                ActionsType.STOP.name -> stopLocationUpdates()
            }
        }

        // by returning this we make sure the service is restarted if the system kills the service
        return START_STICKY
    }

    override fun onBind(p0: Intent?): IBinder? {
        return null
    }

    override fun onCreate() {
        super.onCreate()

        fusedLocationProviderClient =
            LocationServices.getFusedLocationProviderClient(baseContext)
        locationRequest = LocationRequest.create()
        locationRequest.interval = 4000
        locationRequest.fastestInterval = 2000
        locationRequest.priority = Priority.PRIORITY_HIGH_ACCURACY


        showPushNotification(
            "Notification Service",
            "Service started",
            4000
        )

        broadcastGps = CheckGPS(object : CheckGPS.LocationCallBack {
            override fun gpsOn() {
                Log.d("Niko", "Service onCreate  stato GPS = gpsOn")
                //Send broadcast check GPS
                sendCheckGPS(true)
            }

            override fun gpsOff() {
                Log.d("Niko", "Service onCreate  stato GPS = gpsOff")
                //Send broadcast check GPS
                sendCheckGPS(false)

                showPushNotification(
                    "Notification Service",
                    "Service started but GPS is Off!",
                    4000
                )
            }

            override fun error() {
                Log.d("Niko", "Service onCreate errore stato GPS")
            }

        })

        /**
         * registerReceiver CheckGPS
         */
        baseContext.registerReceiver(
            broadcastGps,
            IntentFilter(LocationManager.MODE_CHANGED_ACTION)
        )
    }

    override fun onDestroy() {
        super.onDestroy()
        stopLocationUpdates()
    }

    private fun startLocationUpdates() {

        // we need this lock so our service gets not affected by Doze Mode
        wakeLock = (getSystemService(Context.POWER_SERVICE) as PowerManager).run {
            newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "SpeedService::lock").apply {
                acquire()
            }
        }

        fusedLocationProviderClient.requestLocationUpdates(
            locationRequest,
            locationCallback,
            Looper.getMainLooper()
        )

        //Send start service
        sendStateService(true)
    }

    private var locationCallback: LocationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult) {
            for (location in locationResult.locations) {
                actualSpeed = (location.speed * 3.6).roundToInt()
                Log.d("Niko", "Service onLocationResult : $actualSpeed km/h")
                showPushNotification(
                    "Notification Service",
                    "Your speed is $actualSpeed km/h",
                    4000
                )

                //SET CURRENT POSITION
                val intent =
                    Intent(INTENT_SET_CURRENT_POSITION).apply {
                        putExtra(
                            SET_CURRENT_POSITION, location
                        )
                    }
                // Post notification
                baseContext.sendBroadcast(intent)
            }
        }
    }

    private fun stopLocationUpdates() {

        //REMOVE WEKELOCK
        try {
            wakeLock?.let {
                if (it.isHeld) {
                    it.release()
                }
            }
            stopForeground(true)
            stopSelf()
        } catch (e: Exception) {
            Log.d("niKO", "Service : ERRORE dentro stopLocationUpdates = ${e.message}")
        }

        fusedLocationProviderClient.removeLocationUpdates(locationCallback)


        try {
            baseContext.unregisterReceiver(broadcastGps)

        } catch (e: IllegalArgumentException) {
            e.printStackTrace()
        }

        Log.d("Niko", "Service stopLocationUpdates")

        //Send stop service
        sendStateService(false)
    }

    private fun sendStateService(isStartService: Boolean) {
        val intent =
            Intent(INTENT_ON_STATE_SERVICE).apply {
                putExtra(
                    ON_STATE_SERVICE, isStartService
                )
            }
        // Post notification
        baseContext.sendBroadcast(intent)
    }

    private fun sendCheckGPS(isGps: Boolean) {
        val intent =
            Intent(INTENT_CHECK_GPS).apply {
                putExtra(
                    CHECK_GPS, isGps
                )
            }
        // Post notification
        baseContext.sendBroadcast(intent)
    }

    private fun showPushNotification(
        title: String,
        message: String,
        id: Int
    ) {

        val channelId =
            "service_" + baseContext.getString(com.example.exampleservice.R.string.app_name)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationChannel = NotificationChannel(
                channelId,
                channelId,
                NotificationManager.IMPORTANCE_HIGH
            )
            val manager: NotificationManager =
                baseContext.getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(notificationChannel)
        }
        //Send to SplashScreenActivity
        val pendingIntent: PendingIntent =
            Intent(this, MainActivity::class.java).apply {
                this.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }.let { notificationIntent ->
                PendingIntent.getActivity(
                    this,
                    0,
                    notificationIntent,
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                )
            }
        val builder = NotificationCompat.Builder(
            baseContext,
            channelId
        )
            .setSmallIcon(com.example.exampleservice.R.drawable.ic_launcher_background)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)

        builder.setLargeIcon(
            BitmapFactory.decodeResource(
                resources,
                com.example.exampleservice.R.drawable.speed
            )
        )

        with(NotificationManagerCompat.from(this)) {
            notify(id, builder.build())
        }

        //Set when Starting the service in >=26 Mode from a BroadcastReceiver
        startForeground(id, builder.build())
    }
}