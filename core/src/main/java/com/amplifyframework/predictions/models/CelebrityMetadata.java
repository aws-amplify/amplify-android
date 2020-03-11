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

import java.net.URL;
import java.util.List;
import java.util.Objects;

/**
 * Metadata class holding details about a celebrity.
 */
public final class CelebrityMetadata {
    private final String name;
    private final String identifier;
    private final List<URL> urls;
    private final Pose pose;

    private CelebrityMetadata(
            @NonNull String name,
            @NonNull String identifier,
            @NonNull List<URL> urls,
            @NonNull Pose pose
    ) {
        this.name = name;
        this.identifier = identifier;
        this.urls = urls;
        this.pose = pose;
    }

    /**
     * Gets the detected celebrity's name.
     * @return the name
     */
    @NonNull
    public String getName() {
        return name;
    }

    /**
     * Gets the identifier.
     * @return the identifier
     */
    @NonNull
    public String getIdentifier() {
        return identifier;
    }

    /**
     * Gets the list of URLs.
     * @return the urls
     */
    @NonNull
    public List<URL> getUrls() {
        return urls;
    }

    /**
     * Gets the detected pose.
     * @return the pose
     */
    @NonNull
    public Pose getPose() {
        return pose;
    }

    /**
     * Gets the builder to help easily construct the
     * metadata object.
     * @return an unassigned builder instance
     */
    @NonNull
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder for {@link CelebrityMetadata}.
     */
    public static class Builder {
        private String name;
        private String identifier;
        private List<URL> urls;
        private Pose pose;

        /**
         * Sets the celebrity's name and return this builder.
         * @param name the celebrity's name
         * @return this builder instance
         */
        @NonNull
        public Builder name(@NonNull String name) {
            this.name = Objects.requireNonNull(name);
            return this;
        }

        /**
         * Sets the identifier and return this builder.
         * @param identifier the identifier
         * @return this builder instance
         */
        @NonNull
        public Builder identifier(@NonNull String identifier) {
            this.identifier = Objects.requireNonNull(identifier);
            return this;
        }

        /**
         * Sets the urls and return this builder.
         * @param urls the urls
         * @return this builder instance
         */
        @NonNull
        public Builder urls(@NonNull List<URL> urls) {
            this.urls = Objects.requireNonNull(urls);
            return this;
        }

        /**
         * Sets the pose and return this builder.
         * @param pose the pose
         * @return this builder instance
         */
        @NonNull
        public Builder pose(@NonNull Pose pose) {
            this.pose = Objects.requireNonNull(pose);
            return this;
        }

        /**
         * Create a new instance of {@link CelebrityMetadata} using
         * the values assigned to this builder instance.
         * @return An instance of {@link CelebrityMetadata}
         */
        @NonNull
        public CelebrityMetadata build() {
            return new CelebrityMetadata(
                    Objects.requireNonNull(name),
                    Objects.requireNonNull(identifier),
                    Objects.requireNonNull(urls),
                    Objects.requireNonNull(pose)
            );
        }

    }
}
