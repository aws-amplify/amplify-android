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

import com.amplifyframework.core.async.Options;
import com.amplifyframework.storage.StorageAccessLevel;

/**
 * Storage options interface requires that every
 * storage operation will inspect the provided options
 * instance for access level and target ID.
 */
abstract class StorageOptions implements Options {
    private final StorageAccessLevel accessLevel;
    private final String targetIdentityId;

    StorageOptions(StorageAccessLevel accessLevel,
                   String targetIdentityId) {
        this.accessLevel = accessLevel;
        this.targetIdentityId = targetIdentityId;
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

    abstract static class Builder {
        private StorageAccessLevel accessLevel;
        private String targetIdentityId;

        /**
         * Configures the storage access level to set on new
         * StorageOptions instances.
         * @param accessLevel Storage access level for new StorageOptions instances
         * @return Current Builder instance, for fluent method chaining
         */
        @NonNull
        public Builder accessLevel(@Nullable StorageAccessLevel accessLevel) {
            this.accessLevel = accessLevel;
            return this;
        }

        /**
         * Configures the target identity ID that will be used on newly
         * built StorageOptions.
         * @param targetIdentityId Target identity ID for new StorageOptions instances
         * @return Current Builder instance, for fluent method chaining
         */
        @NonNull
        public Builder targetIdentityId(@Nullable String targetIdentityId) {
            this.targetIdentityId = targetIdentityId;
            return this;
        }

        @Nullable
        public StorageAccessLevel getAccessLevel() {
            return accessLevel;
        }

        @Nullable
        public String getTargetIdentityId() {
            return targetIdentityId;
        }

        /**
         * Constructs and returns a new immutable instance of the
         * StorageOptions, using the configurations that
         * have been provided the current instance of the Builder.
         * @return A new immutable instance of StorageOptions
         */
        @NonNull
        abstract StorageOptions build();
    }
}
