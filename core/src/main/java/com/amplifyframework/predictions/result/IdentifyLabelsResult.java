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

package com.amplifyframework.predictions.result;

import androidx.annotation.NonNull;

import com.amplifyframework.predictions.models.Label;
import com.amplifyframework.util.Immutable;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Holds data about all of the objects and scenes that were
 * detected and labeled in an image or a video.
 */
public final class IdentifyLabelsResult implements IdentifyResult {
    private final List<Label> labels;
    private final boolean unsafeContent;

    private IdentifyLabelsResult(final Builder builder) {
        this.labels = builder.getLabels();
        this.unsafeContent = builder.getUnsafeContent();
    }

    /**
     * Gets the list of labels.
     * @return the labels
     */
    @NonNull
    public List<Label> getLabels() {
        return Immutable.of(labels);
    }

    /**
     * Returns true if it is unsafe content.
     * @return true if it is unsafe content
     */
    public boolean isUnsafeContent() {
        return unsafeContent;
    }

    /**
     * Gets a builder to help easily construct an instance
     * of label identification result.
     * @return an unassigned builder instance
     */
    @NonNull
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder to help easily construct an instance of
     * {@link IdentifyLabelsResult}.
     */
    public static final class Builder {
        private List<Label> labels;
        private boolean unsafeContent;

        private Builder() {
            this.labels = Collections.emptyList();
        }

        /**
         * Sets the labels and return this builder.
         * @param labels the list of labels
         * @return this builder instance
         */
        @NonNull
        public Builder labels(@NonNull List<Label> labels) {
            this.labels = Objects.requireNonNull(labels);
            return this;
        }

        /**
         * Sets the unsafe content flag and return this builder.
         * @param unsafeContent the unsafe content flag
         * @return this builder instance
         */
        @NonNull
        public Builder unsafeContent(boolean unsafeContent) {
            this.unsafeContent = unsafeContent;
            return this;
        }

        /**
         * Construct a new instance of {@link IdentifyLabelsResult}
         * from the values assigned to this builder instance.
         * @return An instance of {@link IdentifyLabelsResult}
         */
        @NonNull
        public IdentifyLabelsResult build() {
            return new IdentifyLabelsResult(this);
        }

        @NonNull
        List<Label> getLabels() {
            return Objects.requireNonNull(labels);
        }

        boolean getUnsafeContent() {
            return unsafeContent;
        }
    }
}
