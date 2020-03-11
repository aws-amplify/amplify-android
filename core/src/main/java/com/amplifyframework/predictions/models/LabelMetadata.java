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

import java.util.List;
import java.util.Objects;

/**
 * Metadata to store the details of a detected label.
 */
public final class LabelMetadata {
    private final Float score;
    private final List<String> parents;

    private LabelMetadata(
            @NonNull Float score,
            @Nullable List<String> parents
    ) {
        this.score = score;
        this.parents = parents;
    }

    /**
     * Gets the confidence score of this detection.
     * @return the confidence score
     */
    @NonNull
    public Float getScore() {
        return score;
    }

    /**
     * Gets the list of parents' names.
     * @return the list of parents
     */
    @Nullable
    public List<String> getParents() {
        return parents;
    }

    /**
     * Gets a builder to construct label metadata.
     * @return a new builder
     */
    @NonNull
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link LabelMetadata}.
     */
    public static class Builder {
        private Float score;
        private List<String> parents;

        /**
         * Sets the confidence score and return this builder.
         * @param score the confidence score
         * @return this builder instance
         */
        @NonNull
        public Builder score(@NonNull Float score) {
            this.score = Objects.requireNonNull(score);
            return this;
        }

        /**
         * Sets the list of parents' names and return this builder.
         * @param parents the parents
         * @return this builder instance
         */
        @NonNull
        public Builder parents(@Nullable List<String> parents) {
            this.parents = parents;
            return this;
        }

        /**
         * Construct a new instance of {@link LabelMetadata} from
         * the values assigned to this builder.
         * @return An instance of {@link LabelMetadata}
         */
        @NonNull
        public LabelMetadata build() {
            return new LabelMetadata(
                    Objects.requireNonNull(score),
                    parents
            );
        }
    }
}
