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

import androidx.annotation.Nullable;
import androidx.core.util.ObjectsCompat;

/**
 * Place represents a location on the map.
 * This class should be extended to represent a location with more details.
 */
public class Place {
    private final Geometry geometry;

    /**
     * Constructs a new place instance with location geometry.
     *
     * @param geometry the coordinates or area of this place.
     */
    public Place(Geometry geometry) {
        this.geometry = geometry;
    }

    /**
     * Returns the location geometry of this place.
     *
     * @return the location geometry.
     */
    @Nullable
    public Geometry getGeometry() {
        return geometry;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return "Place{" +
                "geometry=" + geometry +
                '}';
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        Place place = (Place) obj;
        return ObjectsCompat.equals(geometry, place.geometry);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        return ObjectsCompat.hash(geometry);
    }
}
