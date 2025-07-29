/*
 * Copyright 2022 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package com.amplifyframework.geo.maplibre.view

import android.graphics.Color
import androidx.core.graphics.toColorInt
import kotlin.math.min
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.geojson.Feature
import org.maplibre.geojson.Point

/**
 * Stores options to use when clustering.
 */
class ClusteringOptions private constructor(
    // The color for the cluster circles. Default is blue.
    val clusterColor: Int,
    // The color for cluster circles at different cluster steps (map from number of points
    // in the cluster to the cluster circle color). Default is no cluster color steps.
    val clusterColorSteps: Map<Int, Int>,
    // The color for the number shown in the cluster circles. Default is white.
    val clusterNumberColor: Int,
    // The behavior when a cluster (feature) on the map is clicked. Default is to zoom
    // in on the cluster clicked.
    val onClusterClicked: (MapLibreView, Feature) -> Unit,
    // The maximum zoom level to cluster points at. Default is 13.
    val maxClusterZoomLevel: Int,
    // The radius each cluster should cover on the map. Default is 75.
    val clusterRadius: Int
) {
    companion object {
        /**
         * Returns a new builder instance for constructing ClusteringOptions.
         * @return a new builder instance.
         */
        @JvmStatic fun builder() = Builder()

        /**
         * Returns a new ClusteringOptions instance with default values.
         * @return a default instance.
         */
        @JvmStatic fun defaults() = builder().build()
    }

    /**
     * Builder class for constructing a ClusteringOptions instance.
     */
    class Builder {
        var clusterColor: Int = "#1E88E5".toColorInt()
            private set
        var clusterColorSteps: Map<Int, Int> = mapOf(
            20 to "#EF5350".toColorInt(),
            50 to "#FFEB3B".toColorInt()
        )
            private set
        var clusterNumberColor: Int = Color.BLACK
            private set
        var onClusterClicked: (MapLibreView, Feature) -> Unit = { mapLibreView, feature ->
            mapLibreView.getMapAsync { map ->
                val featureLocation = feature.geometry() as Point
                val featureLatLng = LatLng(featureLocation.latitude(), featureLocation.longitude())
                val zoomLevel = min(map.maxZoomLevel, map.cameraPosition.zoom + 1)
                map.animateCamera(CameraUpdateFactory.newLatLngZoom(featureLatLng, zoomLevel))
            }
        }
            private set
        var maxClusterZoomLevel: Int = 13
            private set
        var clusterRadius: Int = 75
            private set

        /**
         * Sets the color for the cluster circles.
         * @param clusterColor the color for the cluster circles.
         * @return this builder instance.
         */
        fun clusterColor(clusterColor: Int) = apply { this.clusterColor = clusterColor }

        /**
         * Sets the color for cluster circles at different cluster steps (number of points in the cluster).
         * @param clusterColorSteps a map from number of points in the cluster to the cluster circle color.
         * @return this builder instance.
         */
        fun clusterColorSteps(clusterColorSteps: Map<Int, Int>) = apply { this.clusterColorSteps = clusterColorSteps }

        /**
         * Sets the color for the number shown in the cluster circles.
         * @param clusterNumberColor the color for the number in the cluster circles.
         * @return this builder instance.
         */
        fun clusterNumberColor(clusterNumberColor: Int) = apply { this.clusterNumberColor = clusterNumberColor }

        /**
         * Sets the behavior when a cluster (feature) on the map is clicked.
         * @param onClusterClicked the behavior when a cluster (feature) is clicked.
         * @return this builder instance.
         */
        fun onClusterClicked(onClusterClicked: (MapLibreView, Feature) -> Unit) =
            apply { this.onClusterClicked = onClusterClicked }

        /**
         * Sets the maximum zoom level to cluster points at.
         * @param maxClusterZoomLevel the maximum zoom level to cluster points at.
         * @return this builder instance.
         */
        fun maxClusterZoomLevel(maxClusterZoomLevel: Int) = apply { this.maxClusterZoomLevel = maxClusterZoomLevel }

        /**
         * Sets the radius each cluster should cover on the map.
         * @param clusterRadius the radius each cluster should cover on the map.
         * @return this builder instance.
         */
        fun clusterRadius(clusterRadius: Int) = apply { this.clusterRadius = clusterRadius }

        /**
         * Constructs a clustering options instance with the specified options.
         * @return an instance of ClusteringOptions.
         */
        fun build() = ClusteringOptions(
            clusterColor,
            clusterColorSteps,
            clusterNumberColor,
            onClusterClicked,
            maxClusterZoomLevel,
            clusterRadius
        )
    }
}
