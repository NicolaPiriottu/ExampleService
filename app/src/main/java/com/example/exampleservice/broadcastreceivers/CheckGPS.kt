package com.example.exampleservice.broadcastreceivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.location.LocationManager

/**
 * Created by Nicola Luigi Piriottu on 15/06/22.
 */
class CheckGPS(private val locationCallBack: LocationCallBack) : BroadcastReceiver() {

    interface LocationCallBack {
        fun gpsOn()
        fun gpsOff()
        fun error()
    }

    override fun onReceive(context: Context, intent: Intent?) {
        val lm: LocationManager =
            context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        var isGpsEnabled = false

        try {
            isGpsEnabled = lm.isProviderEnabled(LocationManager.GPS_PROVIDER)
        } catch (ex: Exception) {
            //ERROR
            locationCallBack.error()
        }

        //If GPS is OFF
        if (!isGpsEnabled) {
            locationCallBack.gpsOff()
        } else {
            locationCallBack.gpsOn()
        }
    }
}