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

import com.amplifyframework.core.async.Options;
import com.amplifyframework.storage.StorageAccessLevel;

/**
 * Options to specify attributes of get URL API invocation.
 */
public final class StorageGetUrlOptions implements Options {
    private final StorageAccessLevel accessLevel;
    private final String targetIdentityId;
    private final int expires;

    StorageGetUrlOptions(final Builder builder) {
        this.accessLevel = builder.getAccessLevel();
        this.targetIdentityId = builder.getTargetIdentityId();
        this.expires = builder.getExpires();
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
     * Gets the number of seconds before the URL expires.
     * @return number of seconds before the URL expires
     */
    public int getExpires() {
        return expires;
    }

    /**
     * Factory method to create a new instance of the
     * {@link StorageGetUrlOptions.Builder}.  The builder can be
     * used to configure properties and then construct a new immutable
     * instance of the StorageGetUrlOptions.
     * @return An instance of the {@link StorageGetUrlOptions.Builder}
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
    public static Builder from(StorageGetUrlOptions options) {
        return builder()
            .accessLevel(options.getAccessLevel())
            .targetIdentityId(options.getTargetIdentityId());
    }

    /**
     * Constructs a default instance of the {@link StorageGetUrlOptions}.
     * @return default instance of StorageGetUrlOptions
     */
    public static StorageGetUrlOptions defaultInstance() {
        return builder().build();
    }

    /**
     * A utility that can be used to configure and construct immutable
     * instances of the {@link StorageGetUrlOptions}, by chaining
     * fluent configuration method calls.
     */
    public static final class Builder {
        private StorageAccessLevel accessLevel;
        private String targetIdentityId;
        private int expires;

        Builder() {
        }

        /**
         * Configures the storage access level to set on new
         * StorageGetUrlOptions instances.
         * @param accessLevel Storage access level for new StorageGetUrlOptions instances
         * @return Current Builder instance, for fluent method chaining
         */
        public Builder accessLevel(StorageAccessLevel accessLevel) {
            this.accessLevel = accessLevel;
            return this;
        }

        /**
         * Configures the target identity ID that will be used on newly
         * built StorageGetUrlOptions.
         * @param targetIdentityId Target identity ID for new StorageGetUrlOptions instances
         * @return Current Builder instance, for fluent method chaining
         */
        public Builder targetIdentityId(String targetIdentityId) {
            this.targetIdentityId = targetIdentityId;
            return this;
        }

        /**
         * Configures the number of seconds left until URL expires on new
         * StorageGetUrlOptions instances.
         * StorageGetUrlOptions instances.
         * @param accessLevel Storage access level for new StorageGetUrlOptions instances
         * @return Current Builder instance, for fluent method chaining
         */
        public Builder expires(int accessLevel) {
            this.expires = expires;
            return this;
        }

        /**
         * Constructs and returns a new immutable instance of the
         * StorageGetUrlOptions, using the configurations that
         * have been provided the current instance of the Builder.
         * @return A new immutable instance of StorageGetUrlOptions
         */
        public StorageGetUrlOptions build() {
            return new StorageGetUrlOptions(this);
        }

        StorageAccessLevel getAccessLevel() {
            return accessLevel;
        }

        String getTargetIdentityId() {
            return targetIdentityId;
        }

        int getExpires() {
            return expires;
        }
    }
}
