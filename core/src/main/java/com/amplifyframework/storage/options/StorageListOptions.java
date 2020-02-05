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

import com.amplifyframework.core.async.Options;
import com.amplifyframework.storage.StorageAccessLevel;

import java.util.Objects;

/**
 * Options to specify attributes of list API invocation.
 */
public final class StorageListOptions implements Options {
    private final StorageAccessLevel accessLevel;
    private final String targetIdentityId;

    private StorageListOptions(final Builder builder) {
        this.accessLevel = builder.getAccessLevel();
        this.targetIdentityId = builder.getTargetIdentityId();
    }

    /**
     * Gets the storage access level.
     * @return Storage access level
     */
    @Nullable
    public StorageAccessLevel getAccessLevel() {
        return accessLevel;
    }

    /**
     * Gets the target identity id.
     * @return target identity id
     */
    @Nullable
    public String getTargetIdentityId() {
        return targetIdentityId;
    }

    /**
     * Factory method to return an {@link StorageListOptions.Builder} instance
     * which may be used to configure and build an immutable {@link StorageListOptions} object.
     * @return Builder used to construct {@link StorageListOptions}
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
    public static Builder from(@NonNull final StorageListOptions options) {
        return builder()
                .accessLevel(options.getAccessLevel())
                .targetIdentityId(options.getTargetIdentityId());
    }

    /**
     * Factory method to create a simple, defaulted instance of the
     * {@link StorageListOptions}.
     * @return Default storage list options instance
     */
    @NonNull
    public static StorageListOptions defaultInstance() {
        return builder().build();
    }

    /**
     * Used to construct instance of StorageListOptions via
     * fluent configuration methods.
     */
    public static final class Builder {
        private StorageAccessLevel accessLevel;
        private String targetIdentityId;

        /**
         * Configures the storage access level.
         * @param accessLevel Storage access level
         * @return Builder instance for fluent chaining
         */
        @NonNull
        public Builder accessLevel(@NonNull StorageAccessLevel accessLevel) {
            this.accessLevel = Objects.requireNonNull(accessLevel);
            return this;
        }

        /**
         * Configures the target identity ID.
         * @param targetIdentityId target identity ID
         * @return current Builder instance, for fluent chaining
         */
        @NonNull
        public Builder targetIdentityId(@NonNull String targetIdentityId) {
            this.targetIdentityId = Objects.requireNonNull(targetIdentityId);
            return this;
        }

        /**
         * Constructs a new immutable instance of the {@link StorageListOptions},
         * using the values that have been configured on the current instance of this
         * {@link StorageListOptions.Builder} instance, via prior method calls.
         * @return A new immutable instance of {@link StorageListOptions}.
         */
        @NonNull
        public StorageListOptions build() {
            return new StorageListOptions(this);
        }

        @Nullable
        StorageAccessLevel getAccessLevel() {
            return accessLevel;
        }

        @Nullable
        String getTargetIdentityId() {
            return targetIdentityId;
        }
    }
}
