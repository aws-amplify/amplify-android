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
 * Options to specify attributes of remove API invocation.
 */
public final class StorageRemoveOptions implements Options {
    private final StorageAccessLevel accessLevel;
    private final String targetIdentityId;

    StorageRemoveOptions(final Builder builder) {
        this.accessLevel = builder.getAccessLevel();
        this.targetIdentityId = builder.getTargetIdentityId();
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
     * Factory method to create a new instance of the
     * {@link StorageRemoveOptions.Builder}.  The builder can be
     * used to configure properties and then construct a new immutable
     * instance of the StorageRemoveOptions.
     * @return An instance of the {@link StorageRemoveOptions.Builder}
     */
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
    public static Builder from(StorageRemoveOptions options) {
        return builder()
                .accessLevel(options.getAccessLevel())
                .targetIdentityId(options.getTargetIdentityId());
    }

    /**
     * Constructs a default instance of the {@link StorageRemoveOptions}.
     * @return default instance of StorageRemoveOptions
     */
    public static StorageRemoveOptions defaultInstance() {
        return builder().build();
    }

    /**
     * A utility that can be used to configure and construct immutable
     * instances of the {@link StorageRemoveOptions}, by chaining
     * fluent configuration method calls.
     */
    public static final class Builder {
        private StorageAccessLevel accessLevel;
        private String targetIdentityId;

        Builder() {
        }

        /**
         * Configures the storage access level to set on new
         * StorageRemoveOptions instances.
         * @param accessLevel Storage access level for new StorageRemoveOptions instances
         * @return Current Builder instance, for fluent method chaining
         */
        public Builder accessLevel(StorageAccessLevel accessLevel) {
            this.accessLevel = accessLevel;
            return this;
        }

        /**
         * Configures the target identity ID that will be used on newly
         * built StorageRemoveOptions.
         * @param targetIdentityId Target identity ID for new StorageRemoveOptions instances
         * @return Current Builder instance, for fluent method chaining
         */
        public Builder targetIdentityId(String targetIdentityId) {
            this.targetIdentityId = targetIdentityId;
            return this;
        }

        /**
         * Constructs and returns a new immutable instance of the
         * StorageRemoveOptions, using the configurations that
         * have been provided the current instance of the Builder.
         * @return A new immutable instance of StorageRemoveOptions
         */
        public StorageRemoveOptions build() {
            return new StorageRemoveOptions(this);
        }

        StorageAccessLevel getAccessLevel() {
            return accessLevel;
        }

        String getTargetIdentityId() {
            return targetIdentityId;
        }
    }
}
