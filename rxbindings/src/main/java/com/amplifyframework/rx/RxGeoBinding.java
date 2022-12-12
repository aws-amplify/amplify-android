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
import androidx.annotation.VisibleForTesting;

import com.amplifyframework.core.Amplify;
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
import java.util.Objects;

import io.reactivex.rxjava3.core.Single;

final class RxGeoBinding implements RxGeoCategoryBehavior {
    private final GeoCategoryBehavior delegate;

    RxGeoBinding() {
        delegate = Amplify.Geo;
    }

    @VisibleForTesting
    RxGeoBinding(@NonNull GeoCategoryBehavior delegate) {
        this.delegate = Objects.requireNonNull(delegate);
    }

    @Override
    public Single<Collection<MapStyle>> getAvailableMaps() {
        return toSingle(delegate::getAvailableMaps);
    }

    @Override
    public Single<MapStyle> getDefaultMap() {
        return toSingle(delegate::getDefaultMap);
    }

    @Override
    public Single<MapStyleDescriptor> getMapStyleDescriptor() {
        return toSingle(delegate::getMapStyleDescriptor);
    }

    @Override
    public Single<MapStyleDescriptor> getMapStyleDescriptor(@NonNull GetMapStyleDescriptorOptions options) {
        return toSingle((onResult, onError) -> delegate.getMapStyleDescriptor(options, onResult, onError));
    }

    @Override
    public Single<GeoSearchResult> searchByText(@NonNull String query) {
        return toSingle((onResult, onError) -> delegate.searchByText(query, onResult, onError));
    }

    @Override
    public Single<GeoSearchResult> searchByText(@NonNull String query, @NonNull GeoSearchByTextOptions options) {
        return toSingle((onResult, onError) -> delegate.searchByText(query, options, onResult, onError));
    }

    @Override
    public Single<GeoSearchResult> searchByCoordinates(@NonNull Coordinates position) {
        return toSingle((onResult, onError) -> delegate.searchByCoordinates(position, onResult, onError));
    }

    @Override
    public Single<GeoSearchResult> searchByCoordinates(
            @NonNull Coordinates position,
            @NonNull GeoSearchByCoordinatesOptions options
    ) {
        return toSingle((onResult, onError) -> delegate.searchByCoordinates(position, options, onResult, onError));
    }

    private static <T> Single<T> toSingle(RxAdapters.VoidBehaviors.ResultEmitter<T, GeoException> behavior) {
        return RxAdapters.VoidBehaviors.toSingle(behavior);
    }
}
