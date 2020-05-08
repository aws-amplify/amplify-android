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
import androidx.annotation.Nullable;

import java.util.Objects;
import java.util.UUID;

/**
 * A generic class to hold information about an inferred
 * feature and the confidence score for inference.
 * @param <T> the feature type
 */
public abstract class Feature<T> implements Comparable<Feature<T>> {
    private final String id;
    private final T value;
    private final float confidence;

    Feature(Builder<?, ? extends Feature<T>, T> builder) {
        this.id = builder.getId();
        this.value = builder.getValue();
        this.confidence = builder.getConfidence();
    }

    /**
     * Gets the type name of feature.
     * @return the feature's type alias
     */
    @NonNull
    public abstract String getTypeAlias();

    /**
     * Gets the unique ID assigned to this result.
     * @return unique ID
     */
    @NonNull
    public final String getId() {
        return id;
    }

    /**
     * Gets the detected value for this feature.
     * @return the detected result
     */
    @NonNull
    public final T getValue() {
        return value;
    }

    /**
     * Gets the confidence score for this detection result.
     * The confidence score is a percentage value between
     * 0 and 100.
     * @return the confidence score
     */
    public final float getConfidence() {
        return confidence;
    }

    /**
     * Compares an feature to another. The features are
     * sorted by their types' alphabetic order, and by
     * increasing order of their confidence score for those
     * within the same type.
     * @param other the other feature to compare to
     * @return positive if this item comes after
     */
    @Override
    public int compareTo(@Nullable Feature<T> other) {
        if (other == null) {
            return -1;
        }
        int typeDiff = this.getTypeAlias().compareToIgnoreCase(other.getTypeAlias());
        if (typeDiff != 0) {
            return typeDiff;
        }
        return (int) Math.signum(this.getConfidence() - other.getConfidence());
    }

    /**
     * Builder class to help construct an instance of {@link Feature}.
     * @param <B> Extension of this builder
     * @param <R> Extension of detected feature instance
     * @param <T> Type of result held by this feature
     */
    @SuppressWarnings("unchecked")
    abstract static class Builder<B extends Builder<B, R, T>, R extends Feature<T>, T> {
        private String id;
        private T value;
        private float confidence;

        Builder() {
            this.id = UUID.randomUUID().toString();
        }

        /**
         * Sets the ID and return this builder.
         * @param id the identifier
         * @return this builder instance
         */
        @NonNull
        @SuppressWarnings("ParameterName")
        public final B id(@NonNull String id) {
            this.id = Objects.requireNonNull(id);
            return (B) this;
        }

        /**
         * Sets the feature value and return this builder.
         * @param value the feature value
         * @return this builder instance
         */
        @NonNull
        public final B value(@NonNull T value) {
            this.value = Objects.requireNonNull(value);
            return (B) this;
        }

        /**
         * Sets the confidence score and return this builder.
         * @param confidence the confidence score
         * @return this builder instance
         */
        public final B confidence(float confidence) {
            this.confidence = confidence;
            return (B) this;
        }

        /**
         * Constructs a new instance of {@link Feature}
         * using the values assigned to this builder.
         * @return An instance of {@link Feature}
         */
        @NonNull
        public abstract R build();

        @NonNull
        final String getId() {
            return Objects.requireNonNull(id);
        }

        @NonNull
        final T getValue() {
            return Objects.requireNonNull(value);
        }

        final float getConfidence() {
            return confidence;
        }
    }
}
