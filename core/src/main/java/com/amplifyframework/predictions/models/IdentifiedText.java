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
 * Represents the graphical text that is identified
 * from inside an image.
 */
public final class IdentifiedText extends ImageFeature<String> {

    private final int page;

    private IdentifiedText(Builder builder) {
        super(builder);
        this.page = builder.getPage();
    }

    @Override
    @NonNull
    public String getTypeAlias() {
        return FeatureType.IDENTIFIED_TEXT.getAlias();
    }

    /**
     * Gets the identified text. This is the same
     * text as the associated feature.
     * @return the identified text
     */
    @NonNull
    public String getText() {
        return getValue();
    }

    /**
     * In the case that multiple pages of documents are
     * detected from a given image, this returns the page
     * value in which this text was identified.
     * Defaults to 0.
     * @return the page of the document
     */
    public int getPage() {
        return page;
    }

    /**
     * Gets a builder to construct identified text feature.
     * @return new builder instance
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link IdentifiedText}.
     */
    public static final class Builder extends ImageFeature.Builder<Builder, IdentifiedText, String> {
        private int page;

        /**
         * Sets the identified text and return this builder.
         * @param text the identified text
         * @return this builder instance.
         */
        @NonNull
        public Builder text(@NonNull String text) {
            return super.value(Objects.requireNonNull(text));
        }

        /**
         * Sets the page and return this builder.
         * @param page the page
         * @return this builder instance.
         */
        @NonNull
        public Builder page(int page) {
            this.page = page;
            return this;
        }

        /**
         * Constructs an instance of {@link IdentifiedText}
         * using the values assigned to this builder.
         * @return an instance of {@link IdentifiedText}
         */
        @Override
        @NonNull
        public IdentifiedText build() {
            return new IdentifiedText(this);
        }

        int getPage() {
            return page;
        }
    }
}
