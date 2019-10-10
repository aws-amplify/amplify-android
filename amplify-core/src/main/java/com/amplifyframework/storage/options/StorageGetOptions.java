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

import com.amplifyframework.core.async.Options;
import com.amplifyframework.storage.StorageAccessLevel;

/**
 * Options to specify attributes of get API invocation.
 */
public final class StorageGetOptions implements Options {
    private final StorageAccessLevel accessLevel;
    private final String targetIdentityId;
    private final StorageGetDestination storageGetDestination;
    private final Options options;

    StorageGetOptions(final Builder builder) {
        this.accessLevel = builder.getAccessLevel();
        this.targetIdentityId = builder.getTargetIdentityId();
        this.storageGetDestination = builder.getStorageGetDestination();
        this.options = builder.getOptions();
    }

    /**
     * Gets the storage access level.
     * @return Storage access level
     */
    public StorageAccessLevel getAccessLevel() {
        return accessLevel;
    }

    /**
     * Gets the target identity ID.
     * @return target identity ID
     */
    public String getTargetIdentityId() {
        return targetIdentityId;
    }

    /**
     * Gets the storage get destination.
     * @return Storage get destination
     */
    public StorageGetDestination getStorageGetDestination() {
        return storageGetDestination;
    }

    /**
     * Gets the options.
     * @return Options
     */
    public Options getOptions() {
        return options;
    }

    /**
     * Factory method to create a new instance of the {@link StorageGetOptions.Builder}.
     * The builder can be used to configure properties and then construct a new
     * immutable instance of the StorageGetOptions.
     * @return An instance of the {@link StorageGetOptions.Builder}
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Constructs a default instance of the {@link StorageGetOptions}.
     * @return default instance of StorageGetOptions
     */
    public static StorageGetOptions defaultInstance() {
        return builder().build();
    }

    /**
     * A utility that can be used to configure and construct immutable instances
     * of the {@link StorageGetOptions}, by chaining fluent configuration method calls.
     */
    public static final class Builder {
        private StorageAccessLevel accessLevel;
        private String targetIdentityId;
        private StorageGetDestination storageGetDestination;
        private Options options;

        Builder() {
        }

        /**
         * Configures the storage access level to set on new StorageGetOptions instances.
         * @param accessLevel Storage access level for new StorageGetOptions instances
         * @return Current Builder instance, for fluent method chaining
         */
        public Builder accessLevel(StorageAccessLevel accessLevel) {
            this.accessLevel = accessLevel;
            return this;
        }

        /**
         * Configures the target identity ID that will be used on newly built StorageGetOptions.
         * @param targetIdentityId Target identity ID for new StorageGetOptions instances
         * @return Current Builder instance, for fluent method chaining
         */
        public Builder targetIdentityId(String targetIdentityId) {
            this.targetIdentityId = targetIdentityId;
            return this;
        }

        /**
         * Configures the StorageGetDestination that will be used for newly built StorageGetOptions.
         * @param storageGetDestination Storage get destination for newly built StorageGetOptions
         * @return Current Builder instance, for fluent method chaining
         */
        public Builder storageGetDestination(StorageGetDestination storageGetDestination) {
            this.storageGetDestination = storageGetDestination;
            return this;
        }

        /**
         * Configures the options bundle that will be used for newly built StorageGetOptions.
         * @param options Options to set on newly built StorageGetOptions
         * @return Current Builder instance, for fluent method chaining
         */
        public Builder options(Options options) {
            this.options = options;
            return this;
        }

        /**
         * Constructs and returns a new immutable instance of the StorageGetOptions, using the
         * configurations that have been provided the current instance of the Builder.
         * @return A new immutable instance of StorageGetOptions
         */
        public StorageGetOptions build() {
            return new StorageGetOptions(this);
        }

        StorageAccessLevel getAccessLevel() {
            return accessLevel;
        }

        String getTargetIdentityId() {
            return targetIdentityId;
        }

        StorageGetDestination getStorageGetDestination() {
            return storageGetDestination;
        }

        Options getOptions() {
            return options;
        }
    }
}
