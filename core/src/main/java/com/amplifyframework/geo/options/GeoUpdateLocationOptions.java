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

package com.amplifyframework.geo.options;

public class GeoUpdateLocationOptions {
    // Name of tracker resource. Set to default tracker if no tracker is passed in.
    String tracker;
    // Corresponds to Amazon Location Service's PositionProperties (a map that can
    // contain at most 3 key-value pairs). Default is an empty map. Amazon Location Service allows at most
    // 3 key value pairs for PositionProperties. If more than 3 properties are in
    // this map, an exception is thrown for a debug build and a warning is logged
    // for a release (production) build. The keys in the map are truncated to be
    // at most length 20 and the values are truncated to be at most length 40.
    GeoPositionProperties positionProperties;

    protected GeoUpdateLocationOptions(Builder builder) {
        this.tracker = builder.tracker;
        this.positionProperties = builder.positionProperties;
    }

    public String getTracker() {
        return tracker;
    }

    public GeoPositionProperties getPositionProperties() {
        return positionProperties;
    }

    public static final class Builder {
        // TODO: use default tracker
        String tracker = "";
        GeoPositionProperties positionProperties = new GeoPositionProperties();

        public Builder withTracker(String tracker) {
            this.tracker = tracker;
            return this;
        }

        public Builder withPositionProperties(GeoPositionProperties positionProperties) {
            this.positionProperties = positionProperties;
            return this;
        }

        public GeoUpdateLocationOptions build() {
            return new GeoUpdateLocationOptions(this);
        }
    }
}
