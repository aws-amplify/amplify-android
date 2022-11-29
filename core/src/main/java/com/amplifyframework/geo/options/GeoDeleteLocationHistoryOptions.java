/*
 *  Copyright 2022 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License").
 *  You may not use this file except in compliance with the License.
 *  A copy of the License is located at
 *
 *   http://aws.amazon.com/apache2.0
 *
 *  or in the "license" file accompanying this file. This file is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 *  express or implied. See the License for the specific language governing
 *  permissions and limitations under the License.
 */

package com.amplifyframework.geo.options;

import androidx.annotation.Nullable;

public class GeoDeleteLocationHistoryOptions {
    // Name of tracker resource. Set to default tracker if no tracker is passed in.
    @Nullable
    String tracker;

    @Nullable
    public String getTracker() {
        return tracker;
    }

    protected GeoDeleteLocationHistoryOptions(Builder builder) {
        tracker = builder.tracker;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static GeoDeleteLocationHistoryOptions defaults() {
        return builder().build();
    }

    public static final class Builder {
        @Nullable
        String tracker = null;

        public Builder tracker(String tracker) {
            this.tracker = tracker;
            return this;
        }

        public GeoDeleteLocationHistoryOptions build() {
            return new GeoDeleteLocationHistoryOptions(this);
        }
    }
}
