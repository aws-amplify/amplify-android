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
 * Stores map name and its style name.
 */
public final class MapStyle {
    private final String mapName;
    private final String style;

    /**
     * Creates a new {@link MapStyle} object.
     *
     * @param mapName Name of the map resource.
     * @param style   Name of map style being applied on the map.
     */
    public MapStyle(@NonNull String mapName, @NonNull String style) {
        this.mapName = Objects.requireNonNull(mapName);
        this.style = Objects.requireNonNull(style);
    }

    /**
     * Returns the name of map resource.
     *
     * @return the name of map resource.
     */
    @NonNull
    public String getMapName() {
        return mapName;
    }

    /**
     * Returns the style name.
     *
     * @return the style name.
     */
    @NonNull
    public String getStyle() {
        return style;
    }

    @Override
    public boolean equals(Object that) {
        if (this == that) {
            return true;
        }
        if (that == null || getClass() != that.getClass()) {
            return false;
        }
        MapStyle mapStyle = (MapStyle) that;
        return ObjectsCompat.equals(mapName, mapStyle.mapName)
                && ObjectsCompat.equals(style, mapStyle.style);
    }

    @Override
    public int hashCode() {
        return ObjectsCompat.hash(mapName,
                style);
    }

    @Override
    public String toString() {
        return "MapStyle{" +
                "mapName='" + mapName + '\'' +
                ", style='" + style + '\'' +
                '}';
    }
}
