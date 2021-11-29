package com.amplifyframework.geo.maplibre.view

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.amplifyframework.geo.maplibre.Coordinate2D

class AmplifyMapViewModel(options: MapViewOptions = MapViewOptions()) : ViewModel() {

    val compassPosition: MutableLiveData<Double> = MutableLiveData(0.0)

    val currentCenter: LiveData<Coordinate2D> = MutableLiveData(options.center)

    val showCompassIndicator: LiveData<Boolean> = MutableLiveData(options.showCompassIndicator)

//    val markers: LiveData<AnnotationMarker> =

}