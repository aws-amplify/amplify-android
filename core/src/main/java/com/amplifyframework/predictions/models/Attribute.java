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

import java.util.Objects;

/**
 * Class to store detection attribute.
 */
public final class Attribute {
    private final String name;
    private final Boolean value;
    private final Float score;

    private Attribute(
            @NonNull String name,
            @NonNull Boolean value,
            @NonNull Float score
    ) {
        this.name = name;
        this.value = value;
        this.score = score;
    }

    /**
     * Gets the name of attribute.
     * @return the name
     */
    @NonNull
    public String getName() {
        return name;
    }

    /**
     * Gets the value of attribute.
     * @return the value
     */
    @NonNull
    public Boolean getValue() {
        return value;
    }

    /**
     * Gets the confidence score.
     * @return the confidence score
     */
    @NonNull
    public Float getScore() {
        return score;
    }

    /**
     * Gets a builder to construct an attribute.
     * @return a new builder
     */
    @NonNull
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link Attribute}.
     */
    public static class Builder {
        private String name;
        private Boolean value;
        private Float score;

        /**
         * Sets the name and return this builder.
         * @param name the name
         * @return this builder instance
         */
        @NonNull
        public Builder name(@NonNull String name) {
            this.name = Objects.requireNonNull(name);
            return this;
        }

        /**
         * Sets the value and return this builder.
         * @param value the value
         * @return this builder instance
         */
        @NonNull
        public Builder value(@NonNull Boolean value) {
            this.value = Objects.requireNonNull(value);
            return this;
        }

        /**
         * Sets the score and return this builder.
         * @param score the score
         * @return this builder instance
         */
        @NonNull
        public Builder score(@NonNull Float score) {
            this.score = Objects.requireNonNull(score);
            return this;
        }

        /**
         * Constructs a new instance of {@link Attribute} using the
         * values assigned to this builder.
         * @return An instance of {@link Attribute}
         */
        @NonNull
        public Attribute build() {
            return new Attribute(
                    Objects.requireNonNull(name),
                    Objects.requireNonNull(value),
                    Objects.requireNonNull(score)
            );
        }
    }
}
