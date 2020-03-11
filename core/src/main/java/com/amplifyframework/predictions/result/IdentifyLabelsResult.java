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
import androidx.annotation.Nullable;

import com.amplifyframework.predictions.model.Label;

import java.util.List;
import java.util.Objects;

/**
 * Class that holds the labels detection results for
 * the predictions category.
 */
public final class IdentifyLabelsResult implements IdentifyResult {
    private final List<Label> labels;
    private final Boolean unsafeContent;

    private IdentifyLabelsResult(
            @NonNull List<Label> labels,
            @Nullable Boolean unsafeContent
    ) {
        this.labels = labels;
        this.unsafeContent = unsafeContent;
    }

    /**
     * Gets the list of labels.
     * @return the labels
     */
    @NonNull
    public List<Label> getLabels() {
        return labels;
    }

    /**
     * Returns true if it is unsafe content.
     * @return true if it is unsafe content
     */
    @Nullable
    public Boolean isUnsafeContent() {
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
    public static class Builder {
        private List<Label> labels;
        private Boolean unsafeContent;

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
        public Builder unsafeContent(@Nullable Boolean unsafeContent) {
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
            return new IdentifyLabelsResult(
                    Objects.requireNonNull(labels),
                    unsafeContent
            );
        }
    }
}
