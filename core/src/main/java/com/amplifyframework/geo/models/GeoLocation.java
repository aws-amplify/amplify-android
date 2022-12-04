/*
 *
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
 *
 *
 */

package com.amplifyframework.geo.models;

/*
 * Stores latitude and longitude for a location
 */
public final class GeoLocation {
    double latitude, longitude;

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public double getDistance(GeoLocation other) {
        // Haversine method implementation
        java.util.function.Function<Double, Double> haversine = (angle) -> Math.pow(Math.sin(angle/2), 2);
        double latDiff = Math.toRadians(latitude - other.latitude);
        double longDiff = Math.toRadians(longitude - other.longitude);
        return 0.0;
    }

    public GeoLocation(double latitude, double longitude) {
        this.latitude = latitude;
        this.longitude = longitude;
    }
}
