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
 * Holds the gender detection results
 * for the predictions category.
 */
public final class Gender extends Feature<GenderBinaryType> {
    /**
     * Feature type for {@link Gender}.
     */
    public static final String FEATURE_TYPE = GenderBinaryType.class.getSimpleName();

    private Gender(final Builder builder) {
        super(builder);
    }

    @Override
    @NonNull
    public String getType() {
        return FEATURE_TYPE;
    }

    /**
     * Gets a builder to construct an gender feature.
     * @return a new builder
     */
    @NonNull
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link Gender}.
     */
    public static final class Builder extends Feature.Builder<Builder, Gender, GenderBinaryType> {
        @Override
        @NonNull
        public Gender build() {
            return new Gender(this);
        }
    }
}
