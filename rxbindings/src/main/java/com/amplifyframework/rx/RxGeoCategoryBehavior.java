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

package com.amplifyframework.rx;

import androidx.annotation.NonNull;

import com.amplifyframework.geo.GeoCategoryBehavior;
import com.amplifyframework.geo.GeoException;
import com.amplifyframework.geo.models.Coordinates;
import com.amplifyframework.geo.models.MapStyle;
import com.amplifyframework.geo.models.MapStyleDescriptor;
import com.amplifyframework.geo.options.GeoSearchByCoordinatesOptions;
import com.amplifyframework.geo.options.GeoSearchByTextOptions;
import com.amplifyframework.geo.options.GetMapStyleDescriptorOptions;
import com.amplifyframework.geo.result.GeoSearchResult;

import java.util.Collection;

import io.reactivex.rxjava3.core.Single;

/**
 * An Rx-idiomatic expression of the behaviors in {@link GeoCategoryBehavior}.
 */
public interface RxGeoCategoryBehavior {
    /**
     * Gets a collection of maps and their corresponding styles.
     *
     * @return An Rx {@link Single} which emits a {@link MapStyle} on success, or a
     *         {@link GeoException} on failure
     */
    Single<Collection<MapStyle>> getAvailableMaps();

    /**
     * Gets the default map and style from available maps.
     *
     * @return An Rx {@link Single} which emits a {@link MapStyle} on success, or a
     *         {@link GeoException} on failure
     */
    Single<MapStyle> getDefaultMap();

    /**
     * Uses default options to get map style descriptor JSON.
     *
     * @return An Rx {@link Single} which emits a {@link MapStyleDescriptor} on success, or a
     *         {@link GeoException} on failure
     */
    Single<MapStyleDescriptor> getMapStyleDescriptor();

    /**
     * Uses given options to get map style descriptor JSON.
     *
     * @param options Options to specify for this operation.
     * @return An Rx {@link Single} which emits a {@link MapStyleDescriptor} on success, or a
     *         {@link GeoException} on failure
     */
    Single<MapStyleDescriptor> getMapStyleDescriptor(@NonNull GetMapStyleDescriptorOptions options);

    /**
     * Searches for locations that match text query.
     *
     * @param query Search query text.
     * @return An Rx {@link Single} which emits a {@link GeoSearchResult} on success, or a
     *         {@link GeoException} on failure
     */
    Single<GeoSearchResult> searchByText(@NonNull String query);

    /**
     * Searches for locations that match text query.
     *
     * @param query   Search query text.
     * @param options Search options to use.
     * @return An Rx {@link Single} which emits a {@link GeoSearchResult} on success, or a
     *         {@link GeoException} on failure
     */
    Single<GeoSearchResult> searchByText(
            @NonNull String query,
            @NonNull GeoSearchByTextOptions options
    );

    /**
     * Searches for location with given set of coordinates.
     *
     * @param position Coordinates to look-up.
     * @return An Rx {@link Single} which emits a {@link GeoSearchResult} on success, or a
     *         {@link GeoException} on failure
     */
    Single<GeoSearchResult> searchByCoordinates(@NonNull Coordinates position);

    /**
     * Searches for location with given set of coordinates.
     *
     * @param position Coordinates to look-up.
     * @param options  Search options to use.
     * @return An Rx {@link Single} which emits a {@link GeoSearchResult} on success, or a
     *         {@link GeoException} on failure
     */
    Single<GeoSearchResult> searchByCoordinates(
            @NonNull Coordinates position,
            @NonNull GeoSearchByCoordinatesOptions options
    );
}
