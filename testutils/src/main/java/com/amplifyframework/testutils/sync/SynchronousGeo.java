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

package com.amplifyframework.testutils.sync;

import androidx.annotation.NonNull;

import com.amplifyframework.core.Amplify;
import com.amplifyframework.geo.GeoCategory;
import com.amplifyframework.geo.GeoCategoryBehavior;
import com.amplifyframework.geo.GeoException;
import com.amplifyframework.geo.models.Coordinates;
import com.amplifyframework.geo.models.MapStyle;
import com.amplifyframework.geo.models.MapStyleDescriptor;
import com.amplifyframework.geo.options.GeoSearchByCoordinatesOptions;
import com.amplifyframework.geo.options.GeoSearchByTextOptions;
import com.amplifyframework.geo.options.GetMapStyleDescriptorOptions;
import com.amplifyframework.geo.result.GeoSearchResult;
import com.amplifyframework.testutils.Await;

import java.util.Collection;
import java.util.Objects;

/**
 * A utility to perform synchronous calls to the {@link GeoCategory}.
 * This code is not well suited for production use, but is useful in test
 * code, where we want to make a series of sequential assertions after
 * performing various operations.
 */
public final class SynchronousGeo {
    private final GeoCategoryBehavior asyncDelegate;

    private SynchronousGeo(GeoCategoryBehavior asyncDelegate) {
        this.asyncDelegate = asyncDelegate;
    }

    /**
     * Creates a synchronous geo wrapper which delegates calls to the provided geo
     * category behavior.
     *
     * @param asyncDelegate Performs the actual geo operations.
     * @return A synchronous geo wrapper.
     */
    @NonNull
    public static SynchronousGeo delegatingTo(@NonNull GeoCategoryBehavior asyncDelegate) {
        Objects.requireNonNull(asyncDelegate);
        return new SynchronousGeo(asyncDelegate);
    }

    /**
     * Creates a synchronous geo wrapper which delegates to the {@link Amplify#Geo} facade.
     *
     * @return A synchronous geo wrapper.
     */
    @NonNull
    public static SynchronousGeo delegatingToAmplify() {
        return new SynchronousGeo(Amplify.Geo);
    }

    /**
     * Fetches the list of available map styles from configuration.
     *
     * @return a collection of map styles.
     * @throws GeoException if maps are not configured.
     */
    public Collection<MapStyle> getAvailableMaps() throws GeoException {
        return Await.result(asyncDelegate::getAvailableMaps);
    }

    /**
     * Fetches the default map style among configured map resources.
     *
     * @return the map style object.
     * @throws GeoException if default map is not configured.
     */
    public MapStyle getDefaultMap() throws GeoException {
        return Await.result(asyncDelegate::getDefaultMap);
    }

    /**
     * Fetches the map style descriptor document.
     *
     * @param options options to specify name of the map.
     * @return a map style descriptor.
     * @throws GeoException if maps are not configured.
     */
    public MapStyleDescriptor getMapStyleDescriptor(
            GetMapStyleDescriptorOptions options
    ) throws GeoException {
        return Await.<MapStyleDescriptor, GeoException>result((onResult, onError) ->
                asyncDelegate.getMapStyleDescriptor(options, onResult, onError));
    }

    /**
     * Fetches geocode that matches given string query.
     *
     * @param query string query to search.
     * @param options options to specify search parameters.
     * @return search result object containing a list of places.
     * @throws GeoException if search indexes are not configured.
     */
    public GeoSearchResult searchByText(
            String query,
            GeoSearchByTextOptions options
    ) throws GeoException {
        return Await.<GeoSearchResult, GeoException>result((onResult, onError) ->
                asyncDelegate.searchByText(query, options, onResult, onError));
    }

    /**
     * Fetches geocode of provided geo-spacial coordinates.
     *
     * @param coordinates coordinates to reverse geocode.
     * @param options options to specify search parameters.
     * @return search result object containing a list of places.
     * @throws GeoException if search indexes are not configured.
     */
    public GeoSearchResult searchByCoordinates(
            Coordinates coordinates,
            GeoSearchByCoordinatesOptions options
    ) throws GeoException {
        return Await.<GeoSearchResult, GeoException>result((onResult, onError) ->
                asyncDelegate.searchByCoordinates(coordinates, options, onResult, onError));
    }
}
