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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.util.ObjectsCompat;

import java.util.Objects;

/**
 * Search parameter to allow to restrict search results by
 * a bias position or bounding window.
 */
public final class SearchArea {
    private final BoundingBox boundingBox;
    private final Coordinates biasPosition;

    private SearchArea(BoundingBox boundingBox, Coordinates biasPosition) {
        this.boundingBox = boundingBox;
        this.biasPosition = biasPosition;
    }

    /**
     * Constructs a new search parameter with a bias position.
     *
     * @param biasPosition the bias position to search near.
     * @return the search area configured with bias position.
     */
    @NonNull
    public static SearchArea near(@NonNull Coordinates biasPosition) {
        return new SearchArea(null, Objects.requireNonNull(biasPosition));
    }

    /**
     * Constructs a new search parameter with a bounding box.
     *
     * @param boundingBox the bounding box to restrict search.
     * @return the search area configured with a bounding box.
     */
    @NonNull
    public static SearchArea within(@NonNull BoundingBox boundingBox) {
        return new SearchArea(Objects.requireNonNull(boundingBox), null);
    }

    /**
     * Returns the bounding box of this search parameter.
     * Null if this parameter uses bias position instead.
     *
     * @return the bounding box.
     */
    @Nullable
    public BoundingBox getBoundingBox() {
        return boundingBox;
    }

    /**
     * Returns the bias position of this search parameter.
     * Null if this parameter uses bounding box instead.
     *
     * @return the bias position.
     */
    @Nullable
    public Coordinates getBiasPosition() {
        return biasPosition;
    }

    @Override
    public String toString() {
        return "SearchArea{" +
                "boundingBox=" + boundingBox +
                ", biasPosition=" + biasPosition +
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
        SearchArea that = (SearchArea) obj;
        return ObjectsCompat.equals(boundingBox, that.boundingBox)
                && ObjectsCompat.equals(biasPosition, that.biasPosition);
    }

    @Override
    public int hashCode() {
        return ObjectsCompat.hash(boundingBox,
                biasPosition);
    }
}
