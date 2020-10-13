/*
 * Copyright 2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package com.amplifyframework.storage.options;

import android.annotation.SuppressLint;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.amplifyframework.util.Immutable;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Options to specify attributes of put API invocation.
 */
public class StorageUploadFileOptions extends StorageOptions {
    private final String contentType;
    private final Map<String, String> metadata;

    /**
     * Constructs a StorageUploadFileOptions instance with the
     * attributes from builder instance.
     * @param builder the builder with configured attributes
     */
    protected StorageUploadFileOptions(final Builder<?> builder) {
        super(builder.getAccessLevel(), builder.getTargetIdentityId());
        this.contentType = builder.getContentType();
        this.metadata = builder.getMetadata();
    }

    /**
     * The standard MIME type describing the format of the object to store.
     * @return Content type
     */
    @Nullable
    public String getContentType() {
        return contentType;
    }

    /**
     * Metadata for the object to store.
     * @return metadata
     */
    @NonNull
    public Map<String, String> getMetadata() {
        return Immutable.of(metadata);
    }

    /**
     * Returns a new Builder instance that can be used to configure
     * and build a new immutable instance of StorageUploadFileOptions.
     * @return a new builder instance
     */
    @SuppressLint("SyntheticAccessor")
    @NonNull
    public static Builder<?> builder() {
        return new Builder<>();
    }

    /**
     * Factory method to create builder which is configured to prepare
     * object instances with the same field values as the provided
     * options. This can be used as a starting ground to create a
     * new clone of the provided options, which shares some common
     * configuration.
     * @param options Options to populate into a new builder configuration
     * @return A Builder instance that has been configured using the
     *         values in the provided options
     */
    @NonNull
    public static Builder<?> from(@NonNull final StorageUploadFileOptions options) {
        return builder()
            .accessLevel(options.getAccessLevel())
            .targetIdentityId(options.getTargetIdentityId())
            .contentType(options.getContentType())
            .metadata(options.getMetadata());
    }

    /**
     * Creates a new default instance of the StorageUploadFileOptions.
     * @return default storage put options
     */
    @NonNull
    public static StorageUploadFileOptions defaultInstance() {
        return builder().build();
    }

    /**
     * Use to configure and build immutable instances of the
     * StorageUploadFileOptions, using fluent of property configuration
     * methods.
     * @param <B> the type of builder to chain with
     */
    @SuppressWarnings({"unchecked", "WeakerAccess"})
    public static class Builder<B extends Builder<B>> extends StorageOptions.Builder<B, StorageUploadFileOptions> {
        private String contentType;
        private Map<String, String> metadata;

        /**
         * Constructs a new Builder for StorageUploadFileOptions.
         */
        protected Builder() {
            this.metadata = new HashMap<>();
        }

        /**
         * Configures the content type for a new StorageUploadFileOptions instance.
         * @param contentType Content type
         * @return Current Builder instance for fluent chaining
         */
        @NonNull
        public final B contentType(@Nullable String contentType) {
            this.contentType = contentType;
            return (B) this;
        }

        /**
         * Configures metadata for new StorageUploadFileOptions instance.
         * @param metadata Metadata for StorageUploadFileOptions
         * @return Current Builder instance for fluent method chaining
         */
        @NonNull
        public final B metadata(@NonNull Map<String, String> metadata) {
            this.metadata = new HashMap<>(Objects.requireNonNull(metadata));
            return (B) this;
        }

        @Nullable
        final String getContentType() {
            return contentType;
        }

        @NonNull
        final Map<String, String> getMetadata() {
            return Immutable.of(metadata);
        }

        /**
         * Builds a new immutable StorageUploadFileOptions instance,
         * based on the configuration options that have been previously
         * set on this Builder instance.
         * @return A new immutable StorageUploadFileOptions instance
         */
        @SuppressLint("SyntheticAccessor")
        @Override
        @NonNull
        public StorageUploadFileOptions build() {
            return new StorageUploadFileOptions(this);
        }
    }
}
