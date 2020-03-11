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

package com.amplifyframework.predictions.model;

import android.graphics.Rect;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Objects;

/**
 * Abstract class representing the text that is identified
 * from inside an image.
 */
abstract class IdentifiedText {
    private final String text;
    private final Rect boundingBox;
    private final Polygon polygon;
    private final Integer page;

    IdentifiedText(
            @NonNull String text,
            @NonNull Rect boundingBox,
            @Nullable Polygon polygon,
            @Nullable Integer page
    ) {
        this.text = Objects.requireNonNull(text);
        this.boundingBox = Objects.requireNonNull(boundingBox);
        this.polygon = polygon;
        this.page = page;
    }

    /**
     * Gets the identified text.
     * @return the identified text
     */
    @NonNull
    public String getText() {
        return text;
    }

    /**
     * Gets the rectangular boundary.
     * @return the bounding box
     */
    @NonNull
    public Rect getBoundingBox() {
        return boundingBox;
    }

    /**
     * Gets the polygonal boundary.
     * @return the polygon
     */
    @Nullable
    public Polygon getPolygon() {
        return polygon;
    }

    /**
     * Gets the page value.
     * @return the page
     */
    @Nullable
    public Integer getPage() {
        return page;
    }

    /**
     * Builder for {@link IdentifiedText}.
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    abstract static class Builder<B extends Builder, T extends IdentifiedText> {
        private String text;
        private Rect boundingBox;
        private Polygon polygon;
        private Integer page;

        /**
         * Sets the identified text and return this builder.
         * @param text the identified text
         * @return this builder instance.
         */
        @NonNull
        public final B text(@NonNull String text) {
            this.text = Objects.requireNonNull(text);
            return (B) this;
        }

        /**
         * Sets the bounding box and return this builder.
         * @param boundingBox the bounding box
         * @return this builder instance.
         */
        @NonNull
        public final B boundingBox(@NonNull Rect boundingBox) {
            this.boundingBox = Objects.requireNonNull(boundingBox);
            return (B) this;
        }

        /**
         * Sets the polygon bound and return this builder.
         * @param polygon the polygon
         * @return this builder instance.
         */
        @NonNull
        public final B polygon(@Nullable Polygon polygon) {
            this.polygon = polygon;
            return (B) this;
        }

        /**
         * Sets the page and return this builder.
         * @param page the page
         * @return this builder instance.
         */
        @NonNull
        public final B page(@Nullable Integer page) {
            this.page = page;
            return (B) this;
        }

        /**
         * Constructs an instance of extended {@link IdentifiedText}
         * using the values stored inside this builder instance.
         * @return An inherited instance of {@link IdentifiedText}
         */
        @NonNull
        public abstract T build();

        @NonNull
        String getText() {
            return text;
        }

        @NonNull
        Rect getBoundingBox() {
            return boundingBox;
        }

        @Nullable
        Polygon getPolygon() {
            return polygon;
        }

        @Nullable
        Integer getPage() {
            return page;
        }
    }
}
