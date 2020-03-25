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
 * Class that holds the celebrity detection results
 * for the predictions category.
 */
public final class Celebrity extends ImageAttribute<String> {
    /**
     * Attribute type for {@link Celebrity}.
     */
    public static final String ATTRIBUTE_TYPE = Celebrity.class.getSimpleName();

    private Celebrity(final Builder builder) {
        super(builder);
    }

    @Override
    @NonNull
    public String getType() {
        return ATTRIBUTE_TYPE;
    }

    /**
     * Gets the name of detected celebrity.
     * This is the same as {@link Celebrity#getAttribute()}.
     * @return the name of celebrity
     */
    @NonNull
    public String getName() {
        return getAttribute();
    }

    /**
     * Gets a builder to help easily construct
     * an instance of celebrity detection result.
     * @return an unassigned builder instance
     */
    @NonNull
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link Celebrity}.
     */
    public static final class Builder extends ImageAttribute.Builder<Builder, Celebrity, String> {
        /**
         * Sets the name of celebrity. This is the same as
         * setting the attribute value for this builder.
         * @param name the name of celebrity
         * @return this builder instance
         */
        @NonNull
        public Builder name(@NonNull String name) {
            return super.attribute(name);
        }

        /**
         * Create a new instance of {@link Celebrity} using the
         * values assigned to this builder instance.
         * @return An instance of {@link Celebrity}
         */
        @NonNull
        public Celebrity build() {
            return new Celebrity(this);
        }
    }
}
