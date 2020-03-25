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
 * Holds the facial feature detection results
 * for the predictions category.
 */
public final class FacialFeature extends ImageAttribute<FacialFeatureType> {
    /**
     * Attribute type for {@link FacialFeatureType}.
     */
    public static final String ATTRIBUTE_TYPE = FacialFeatureType.class.getSimpleName();

    private FacialFeature(final Builder builder) {
        super(builder);
    }

    @Override
    @NonNull
    public String getType() {
        return ATTRIBUTE_TYPE;
    }

    /**
     * Gets a builder to help easily construct a
     * facial feature detection result object.
     * @return an unassigned builder instance
     */
    @NonNull
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link FacialFeature}.
     */
    public static final class Builder extends ImageAttribute.Builder<Builder, FacialFeature, FacialFeatureType> {
        /**
         * Construct a new instance of {@link FacialFeature} from
         * the values assigned to this builder instance.
         * @return An instance of {@link FacialFeature}
         */
        @NonNull
        public FacialFeature build() {
            return new FacialFeature(this);
        }
    }
}
