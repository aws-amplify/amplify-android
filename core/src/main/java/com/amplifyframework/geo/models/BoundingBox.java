/*
 * Copyright 2021 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package com.amplifyframework.geo.models;

import androidx.core.util.ObjectsCompat;

/**
 * A rectangular bounding box on a 2D surface.
 */
public final class BoundingBox {
    private final double latitudeSW;
    private final double longitudeSW;
    private final double latitudeNE;
    private final double longitudeNE;

    /**
     * Constructs a bounding box bounded by two coordinates
     * in south-west corner and north-east corner.
     *
     * @param coordinatesSW the coordinates at south-west corner.
     * @param coordinatesNE the coordinates at north-east corner.
     */
    public BoundingBox(Coordinates coordinatesSW,
                       Coordinates coordinatesNE) {
        this(coordinatesSW.getLatitude(),
                coordinatesSW.getLongitude(),
                coordinatesNE.getLatitude(),
                coordinatesNE.getLongitude());
    }

    /**
     * Constructs a bounding box bounded by the latitudes and longitudes
     * of south-west corner and north-east corner.
     *
     * @param latitudeSW the latitude at south-west corner.
     * @param longitudeSW the longitude at south-west corner.
     * @param latitudeNE the latitude at north-east corner.
     * @param longitudeNE the longitude at north-east corner.
     */
    public BoundingBox(double latitudeSW,
                       double longitudeSW,
                       double latitudeNE,
                       double longitudeNE) {
        this.latitudeSW = latitudeSW;
        this.longitudeSW = longitudeSW;
        this.latitudeNE = latitudeNE;
        this.longitudeNE = longitudeNE;
    }

    /**
     * Returns the latitude at south-west corner.
     *
     * @return the latitude at south-west corner.
     */
    public double getLatitudeSW() {
        return latitudeSW;
    }

    /**
     * Returns the longitude at south-west corner.
     *
     * @return the longitude at south-west corner.
     */
    public double getLongitudeSW() {
        return longitudeSW;
    }

    /**
     * Returns the latitude at north-east corner.
     *
     * @return the latitude at north-east corner.
     */
    public double getLatitudeNE() {
        return latitudeNE;
    }

    /**
     * Returns the longitude at north-east corner.
     *
     * @return the longitude at north-east corner.
     */
    public double getLongitudeNE() {
        return longitudeNE;
    }

    @Override
    public String toString() {
        return "BoundingBox{" +
                "latitudeSW=" + latitudeSW +
                ", longitudeSW=" + longitudeSW +
                ", latitudeNE=" + latitudeNE +
                ", longitudeNE=" + longitudeNE +
                '}';
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        BoundingBox that = (BoundingBox) obj;
        return ObjectsCompat.equals(latitudeSW, that.latitudeSW)
                && ObjectsCompat.equals(longitudeSW, that.longitudeSW)
                && ObjectsCompat.equals(latitudeNE, that.latitudeNE)
                && ObjectsCompat.equals(longitudeNE, that.longitudeNE);
    }

    @Override
    public int hashCode() {
        return ObjectsCompat.hash(latitudeSW,
                longitudeSW,
                latitudeNE,
                longitudeNE);
    }
}
