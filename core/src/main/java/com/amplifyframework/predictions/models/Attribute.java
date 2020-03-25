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
import java.util.UUID;

/**
 * A generic class to hold information about an inferred
 * attribute and the confidence score for inference.
 * @param <T> the attribute type
 */
public abstract class Attribute<T> implements Comparable<Attribute<T>> {
    private final String id;
    private final T attribute;
    private final float confidence;

    Attribute(Builder<?, ? extends Attribute<T>, T> builder) {
        this.id = builder.getId();
        this.attribute = builder.getAttribute();
        this.confidence = builder.getConfidence();
    }

    /**
     * Gets the type name of attribute.
     * @return the attribute type
     */
    @NonNull
    public abstract String getType();

    /**
     * Gets the unique ID assigned to this result.
     * @return unique ID
     */
    @NonNull
    public final String getId() {
        return id;
    }

    /**
     * Gets the detected value for this attribute.
     * @return the detected result
     */
    @NonNull
    public final T getAttribute() {
        return attribute;
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
     * Compares an attribute to another. The attributes are
     * sorted by their types' alphabetic order, and by
     * decreasing order of their confidence score for those
     * within the same type.
     * @param other the other attribute to compare to
     * @return positive if this item comes after
     */
    @Override
    public int compareTo(Attribute<T> other) {
        if (other == null) {
            return -1;
        }
        int typeDiff = this.getType().compareToIgnoreCase(other.getType());
        if (typeDiff != 0) {
            return typeDiff;
        }
        return (int) (other.getConfidence() - this.getConfidence());
    }

    /**
     * Builder class to help construct an instance of {@link Attribute}.
     * @param <B> Extension of this builder
     * @param <R> Extension of result attribute instance
     * @param <T> Type of result
     */
    @SuppressWarnings("unchecked")
    abstract static class Builder<B extends Builder<B, R, T>, R extends Attribute<T>, T> {
        private String id;
        private T attribute;
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
         * Sets the attribute and return this builder.
         * @param attribute the attribute
         * @return this builder instance
         */
        @NonNull
        public final B attribute(@NonNull T attribute) {
            this.attribute = Objects.requireNonNull(attribute);
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
         * Constructs a new instance of {@link Attribute}
         * using the values assigned to this builder.
         * @return An instance of {@link Attribute}
         */
        @NonNull
        public abstract R build();

        @NonNull
        final String getId() {
            return Objects.requireNonNull(id);
        }

        @NonNull
        final T getAttribute() {
            return Objects.requireNonNull(attribute);
        }

        final float getConfidence() {
            return confidence;
        }
    }
}
