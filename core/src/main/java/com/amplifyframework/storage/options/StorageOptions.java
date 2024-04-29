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

package com.amplifyframework.storage.options;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.amplifyframework.storage.StorageAccessLevel;
import com.amplifyframework.storage.StoragePath;

/**
 * Storage options interface requires that every
 * storage operation will inspect the provided options
 * instance for access level and target ID.
 */
abstract class StorageOptions {
    @SuppressWarnings("deprecation")
    private final StorageAccessLevel accessLevel;
    private final String targetIdentityId;

    @SuppressWarnings("deprecation")
    StorageOptions(StorageAccessLevel accessLevel,
                   String targetIdentityId) {
        this.accessLevel = accessLevel;
        this.targetIdentityId = targetIdentityId;
    }

    /**
     * Gets the storage access level.
     * @deprecated value will be ignored if {@link StoragePath} is used
     * @return Storage access level
     */
    @Deprecated
    @Nullable
    @SuppressWarnings("deprecation")
    public final StorageAccessLevel getAccessLevel() {
        return accessLevel;
    }

    /**
     * Gets the target identity id.
     * @deprecated value will be ignored if {@link StoragePath} is used
     * @return target identity id
     */
    @Deprecated
    @Nullable
    public final String getTargetIdentityId() {
        return targetIdentityId;
    }

    /**
     * Builds storage options.
     */
    @SuppressWarnings({"rawtypes", "unchecked"})
    abstract static class Builder<B extends Builder, O extends StorageOptions> {
        @SuppressWarnings("deprecation")
        private StorageAccessLevel accessLevel;
        private String targetIdentityId;

        /**
         * Configures the storage access level to set on new
         * StorageOptions instances.
         * @deprecated Will not be used if {@link StoragePath} is used
         * @param accessLevel Storage access level for new StorageOptions instances
         * @return Current Builder instance, for fluent method chaining
         */
        @SuppressWarnings("deprecation")
        @Deprecated
        @NonNull
        public final B accessLevel(@Nullable StorageAccessLevel accessLevel) {
            this.accessLevel = accessLevel;
            return (B) this;
        }

        /**
         * Configures the target identity ID that will be used on newly
         * built StorageOptions.
         * @deprecated Will not be used if {@link StoragePath} is used
         * @param targetIdentityId Target identity ID for new StorageOptions instances
         * @return Current Builder instance, for fluent method chaining
         */
        @Deprecated
        @NonNull
        public final B targetIdentityId(@Nullable String targetIdentityId) {
            this.targetIdentityId = targetIdentityId;
            return (B) this;
        }

        @SuppressWarnings("deprecation")
        @Deprecated
        @Nullable
        public final StorageAccessLevel getAccessLevel() {
            return accessLevel;
        }

        @Deprecated
        @Nullable
        public final String getTargetIdentityId() {
            return targetIdentityId;
        }

        /**
         * Constructs and returns a new immutable instance of the
         * StorageOptions, using the configurations that
         * have been provided the current instance of the Builder.
         * @return A new immutable instance of StorageOptions
         */
        @NonNull
        public abstract O build();
    }
}
