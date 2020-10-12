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

import com.amplifyframework.storage.StorageServerSideEncryption;
import com.amplifyframework.util.Immutable;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Options to specify attributes of put API invocation.
 */
public final class StorageUploadFileOptions extends StorageOptions {
    private final String contentType;
    private final StorageServerSideEncryption serverSideEncryption;
    private final Map<String, String> metadata;

    private StorageUploadFileOptions(final Builder builder) {
        super(builder.getAccessLevel(), builder.getTargetIdentityId());
        this.contentType = builder.getContentType();
        this.serverSideEncryption = builder.getServerSideEncryption();
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
     * Server side encryption algorithm.
     * @return Server side encryption algorithm
     */
    @NonNull
    public StorageServerSideEncryption getServerSideEncryption() {
        return serverSideEncryption;
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
    public static Builder builder() {
        return new Builder();
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
    public static Builder from(@NonNull final StorageUploadFileOptions options) {
        return builder().accessLevel(options.getAccessLevel())
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
     */
    public static final class Builder extends StorageOptions.Builder<Builder, StorageUploadFileOptions> {
        private String contentType;
        private StorageServerSideEncryption serverSideEncryption;
        private Map<String, String> metadata;

        private Builder() {
            this.serverSideEncryption = StorageServerSideEncryption.NONE;
            this.metadata = new HashMap<>();
        }

        /**
         * Configures the content type for a new StorageUploadFileOptions instance.
         * @param contentType Content type
         * @return Current Builder instance for fluent chaining
         */
        @SuppressWarnings("WeakerAccess")
        @NonNull
        public Builder contentType(@Nullable String contentType) {
            this.contentType = contentType;
            return this;
        }

        /**
         * Configures the server side encryption algorithm for a new StorageUploadFileOptions instance.
         * @param serverSideEncryption server side encryption algorithm
         * @return Current Builder instance for fluent chaining
         */
        @SuppressWarnings("WeakerAccess")
        @NonNull
        public Builder serverSideEncryption(@NonNull StorageServerSideEncryption serverSideEncryption) {
            this.serverSideEncryption = Objects.requireNonNull(serverSideEncryption);
            return this;
        }

        /**
         * Configures metadata for new StorageUploadFileOptions instance.
         * @param metadata Metadata for StorageUploadFileOptions
         * @return Current Builder instance for fluent method chaining
         */
        @NonNull
        public Builder metadata(@NonNull Map<String, String> metadata) {
            this.metadata = new HashMap<>(Objects.requireNonNull(metadata));
            return this;
        }

        @Nullable
        String getContentType() {
            return contentType;
        }

        @NonNull
        StorageServerSideEncryption getServerSideEncryption() {
            return serverSideEncryption;
        }

        @NonNull
        Map<String, String> getMetadata() {
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
