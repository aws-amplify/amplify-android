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
import androidx.core.util.ObjectsCompat;

import java.util.Objects;

/**
 * Stores the style descriptor of a map resource.
 */
public final class MapStyleDescriptor {
    private final String json;

    /**
     * Creates a new {@link MapStyleDescriptor} object.
     *
     * @param json Map style descriptor JSON.
     */
    public MapStyleDescriptor(@NonNull String json) {
        this.json = Objects.requireNonNull(json);
    }
    /**
     * Returns the style descriptor.
     *
     * @return the style descriptor.
     */
    @NonNull
    public String getJson() {
        return json;
    }

    @Override
    public String toString() {
        return "MapStyleDescriptor{" +
                "json='" + json + '\'' +
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
        MapStyleDescriptor that = (MapStyleDescriptor) obj;
        return ObjectsCompat.equals(json, that.json);
    }

    @Override
    public int hashCode() {
        return ObjectsCompat.hash(json);
    }
}
