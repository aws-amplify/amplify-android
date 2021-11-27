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
 * The coordinates to represent space in 2D.
 */
public final class Coordinates implements Geometry {
    private double latitude;
    private double longitude;

    /**
     * Constructs a new set of coordinates at (0, 0).
     */
    public Coordinates() {
        this(0.0, 0.0);
    }

    /**
     * Constructs a new set of coordinates.
     *
     * @param latitude  the latitude.
     * @param longitude the longitude.
     */
    public Coordinates(double latitude, double longitude) {
        this.latitude = latitude;
        this.longitude = longitude;
    }

    /**
     * Returns the latitude.
     *
     * @return the latitude.
     */
    public double getLatitude() {
        return latitude;
    }

    /**
     * Sets the latitude.
     *
     * @param latitude the latitude.
     */
    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    /**
     * Returns the longitude.
     *
     * @return the longitude.
     */
    public double getLongitude() {
        return longitude;
    }

    /**
     * Sets the longitude.
     *
     * @param longitude the longitude.
     */
    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    /**
     * Uses haversine formula to calculate the central angle between this set of
     * coordinates and another set of coordinates. Multiply this value by the radius
     * of Earth to obtain the distance between these two points.
     *
     * @param coordinates the set of coordinates to central angle between.
     * @return the central angle in radians (unit-less).
     */
    public double centralAngle(Coordinates coordinates) {
        double dLat = Math.toRadians(latitude - coordinates.latitude);
        double dLng = Math.toRadians(longitude - coordinates.longitude);
        double angle = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.sin(dLng / 2) * Math.sin(dLng / 2) *
                Math.cos(Math.toRadians(latitude)) *
                Math.cos(Math.toRadians(coordinates.latitude));
        return 2 * Math.atan2(Math.sqrt(angle), Math.sqrt(1 - angle));
    }

    @Override
    public String toString() {
        return "Coordinates{" +
                "latitude=" + latitude +
                ", longitude=" + longitude +
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
        Coordinates that = (Coordinates) obj;
        return ObjectsCompat.equals(latitude, that.latitude)
                && ObjectsCompat.equals(longitude, that.longitude);
    }

    @Override
    public int hashCode() {
        return ObjectsCompat.hash(latitude,
                longitude);
    }
}
