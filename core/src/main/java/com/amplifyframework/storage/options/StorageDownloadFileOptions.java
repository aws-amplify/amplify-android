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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.amplifyframework.storage.StorageAccessLevel;

/**
 * Options to specify attributes of get API invocation.
 */
public final class StorageDownloadFileOptions extends StorageOptions {

    private StorageDownloadFileOptions(final Builder builder) {
        super(builder.getAccessLevel(), builder.getTargetIdentityId());
    }

    /**
     * Factory method to create a new instance of the
     * {@link StorageDownloadFileOptions.Builder}.  The builder can be
     * used to configure properties and then construct a new immutable
     * instance of the StorageDownloadFileOptions.
     * @return An instance of the {@link StorageDownloadFileOptions.Builder}
     */
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
    public static Builder from(@NonNull final StorageDownloadFileOptions options) {
        return builder()
            .accessLevel(options.getAccessLevel())
            .targetIdentityId(options.getTargetIdentityId());
    }

    /**
     * Constructs a default instance of the {@link StorageDownloadFileOptions}.
     * @return default instance of StorageDownloadFileOptions
     */
    @NonNull
    public static StorageDownloadFileOptions defaultInstance() {
        return builder().build();
    }

    /**
     * A utility that can be used to configure and construct immutable
     * instances of the {@link StorageDownloadFileOptions}, by chaining
     * fluent configuration method calls.
     */
    public static final class Builder {
        private StorageAccessLevel accessLevel;
        private String targetIdentityId;

        /**
         * Configures the storage access level to set on new
         * StorageDownloadFileOptions instances.
         * @param accessLevel Storage access level for new StorageDownloadFileOptions instances
         * @return Current Builder instance, for fluent method chaining
         */
        @NonNull
        public Builder accessLevel(@Nullable StorageAccessLevel accessLevel) {
            this.accessLevel = accessLevel;
            return this;
        }

        /**
         * Configures the target identity ID that will be used on newly
         * built StorageDownloadFileOptions.
         * @param targetIdentityId Target identity ID for new StorageDownloadFileOptions instances
         * @return Current Builder instance, for fluent method chaining
         */
        @NonNull
        public Builder targetIdentityId(@Nullable String targetIdentityId) {
            this.targetIdentityId = targetIdentityId;
            return this;
        }

        /**
         * Constructs and returns a new immutable instance of the
         * StorageDownloadFileOptions, using the configurations that
         * have been provided the current instance of the Builder.
         * @return A new immutable instance of StorageDownloadFileOptions
         */
        @NonNull
        public StorageDownloadFileOptions build() {
            return new StorageDownloadFileOptions(this);
        }

        @Nullable
        public StorageAccessLevel getAccessLevel() {
            return accessLevel;
        }

        @Nullable
        public String getTargetIdentityId() {
            return targetIdentityId;
        }
    }
}
