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

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Metadata to store the details of a detected label.
 */
public final class Label extends ImageAttribute<String> {
    /**
     * Attribute type for {@link Label}.
     */
    public static final String ATTRIBUTE_TYPE = Label.class.getSimpleName();

    private final List<String> parents;

    private Label(final Builder builder) {
        super(builder);
        this.parents = builder.getParents();
    }

    @Override
    @NonNull
    public String getType() {
        return ATTRIBUTE_TYPE;
    }

    /**
     * Gets the list of parents' names.
     * @return the list of parents
     */
    @NonNull
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
     * Builder for {@link Label}.
     */
    public static final class Builder extends ImageAttribute.Builder<Builder, Label, String> {
        private List<String> parents;

        private Builder() {
            this.parents = new ArrayList<>();
        }

        /**
         * Sets the list of parents' names and return this builder.
         * @param parents the parents
         * @return this builder instance
         */
        @NonNull
        public Builder parents(@NonNull List<String> parents) {
            this.parents = Objects.requireNonNull(parents);
            return this;
        }

        /**
         * Construct a new instance of {@link Label} from
         * the values assigned to this builder.
         * @return An instance of {@link Label}
         */
        @NonNull
        public Label build() {
            return new Label(this);
        }

        @NonNull
        List<String> getParents() {
            return Objects.requireNonNull(parents);
        }
    }
}
