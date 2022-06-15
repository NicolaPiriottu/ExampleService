package com.example.exampleservice.viewmodels

import android.location.Location
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.exampleservice.Utils.Event
import kotlin.math.roundToInt

/**
 * Created by Nicola Luigi Piriottu on 15/06/22.
 */
class MainViewModel : ViewModel() {

    //region UseCase
    sealed class UseCaseLiveData {
        data class ShowSpeed(val speed: String) : UseCaseLiveData()
        data class IsRunService(val isRunService: Boolean) : UseCaseLiveData()
        data class GoToNoGpsFragment(val isGps: Boolean) : UseCaseLiveData()
    }

    //region LiveData
    val useCaseLiveData = MutableLiveData<Event<UseCaseLiveData>>()


    fun showSpeed(location: Location?) {

        var speed: String = "0 km/h"

        if (location != null) {
            speed = "${(location.speed * 3.6).roundToInt()} km/h"
        }
        useCaseLiveData.value =
            Event(UseCaseLiveData.ShowSpeed(speed))
    }

    fun isRunService(isRunService: Boolean) {
        useCaseLiveData.value =
            Event(UseCaseLiveData.IsRunService(isRunService))
    }

    fun goToNoGpsFragment(isGPS: Boolean) {
        useCaseLiveData.value =
            Event(UseCaseLiveData.GoToNoGpsFragment(isGPS))
    }

}