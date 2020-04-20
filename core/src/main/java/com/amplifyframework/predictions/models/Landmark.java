/*
 * Copyright 2020 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package com.amplifyframework.predictions.models;

import androidx.annotation.NonNull;

/**
 * Holds the image entity feature detection results
 * for the predictions category.
 */
public final class Landmark extends ImageFeature<LandmarkType> {
    /**
     * Feature type for {@link LandmarkType}.
     */
    public static final String FEATURE_TYPE = LandmarkType.class.getSimpleName();

    private Landmark(final Builder builder) {
        super(builder);
    }

    @Override
    @NonNull
    public String getTypeAlias() {
        return FEATURE_TYPE;
    }

    /**
     * Gets a builder to help easily construct a
     * entity landmark detection result object.
     * @return an unassigned builder instance
     */
    @NonNull
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link Landmark}.
     */
    public static final class Builder extends ImageFeature.Builder<Builder, Landmark, LandmarkType> {
        /**
         * Construct a new instance of {@link Landmark} from
         * the values assigned to this builder instance.
         * @return An instance of {@link Landmark}
         */
        @NonNull
        public Landmark build() {
            return new Landmark(this);
        }
    }
}
