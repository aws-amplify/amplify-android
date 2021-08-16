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

package com.amplifyframework.geo;

import androidx.annotation.NonNull;

import com.amplifyframework.core.Consumer;
import com.amplifyframework.geo.models.MapStyle;

import java.util.Collection;

/**
 * Defines an interface for obtaining geo-spacial data to be
 * used in geocoding, routing, and geofencing.
 * TODO: Add description for Geo.
 */
public interface GeoCategoryBehavior {
    /**
     * Gets a collection of maps and their corresponding styles.
     *
     * @param onResult Called upon successfully fetching a collection of maps.
     * @param onError  Called upon failure to fetch a collection of maps.
     */
    void getAvailableMaps(
            @NonNull Consumer<Collection<MapStyle>> onResult,
            @NonNull Consumer<GeoException> onError
    );

    /**
     * Gets the default map and style from available maps.
     *
     * @param onResult Called upon successfully fetching default map.
     * @param onError  Called upon failure to fetch default map.
     */
    void getDefaultMap(
            @NonNull Consumer<MapStyle> onResult,
            @NonNull Consumer<GeoException> onError
    );
}
