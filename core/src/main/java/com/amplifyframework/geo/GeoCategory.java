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
import com.amplifyframework.core.category.Category;
import com.amplifyframework.core.category.CategoryType;
import com.amplifyframework.geo.models.Coordinates;
import com.amplifyframework.geo.models.MapStyle;
import com.amplifyframework.geo.models.MapStyleDescriptor;
import com.amplifyframework.geo.options.GeoSearchByCoordinatesOptions;
import com.amplifyframework.geo.options.GeoSearchByTextOptions;
import com.amplifyframework.geo.options.GetMapStyleDescriptorOptions;
import com.amplifyframework.geo.result.GeoSearchResult;

import java.util.Collection;

/**
 * Geo category provides an interface for maps and other location-aware
 * capabilities such as location search, routing and asset tracking.
 */
public final class GeoCategory
        extends Category<GeoCategoryPlugin<?>>
        implements GeoCategoryBehavior {

    /**
     * {@inheritDoc}
     */
    @NonNull
    @Override
    public CategoryType getCategoryType() {
        return CategoryType.GEO;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void getAvailableMaps(
            @NonNull Consumer<Collection<MapStyle>> onResult,
            @NonNull Consumer<GeoException> onError
    ) {
        getSelectedPlugin().getAvailableMaps(onResult, onError);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void getDefaultMap(
            @NonNull Consumer<MapStyle> onResult,
            @NonNull Consumer<GeoException> onError
    ) {
        getSelectedPlugin().getDefaultMap(onResult, onError);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void getMapStyleDescriptor(
            @NonNull Consumer<MapStyleDescriptor> onResult,
            @NonNull Consumer<GeoException> onError
    ) {
        getSelectedPlugin().getMapStyleDescriptor(onResult, onError);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void getMapStyleDescriptor(
            @NonNull GetMapStyleDescriptorOptions options,
            @NonNull Consumer<MapStyleDescriptor> onResult,
            @NonNull Consumer<GeoException> onError
    ) {
        getSelectedPlugin().getMapStyleDescriptor(options, onResult, onError);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void searchByText(
            @NonNull String query,
            @NonNull Consumer<GeoSearchResult> onResult,
            @NonNull Consumer<GeoException> onError
    ) {
        getSelectedPlugin().searchByText(query, onResult, onError);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void searchByText(
            @NonNull String query,
            @NonNull GeoSearchByTextOptions options,
            @NonNull Consumer<GeoSearchResult> onResult,
            @NonNull Consumer<GeoException> onError
    ) {
        getSelectedPlugin().searchByText(query, options, onResult, onError);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void searchByCoordinates(
            @NonNull Coordinates position,
            @NonNull Consumer<GeoSearchResult> onResult,
            @NonNull Consumer<GeoException> onError
    ) {
        getSelectedPlugin().searchByCoordinates(position, onResult, onError);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void searchByCoordinates(
            @NonNull Coordinates position,
            @NonNull GeoSearchByCoordinatesOptions options,
            @NonNull Consumer<GeoSearchResult> onResult,
            @NonNull Consumer<GeoException> onError
    ) {
        getSelectedPlugin().searchByCoordinates(position, options, onResult, onError);
    }
}
