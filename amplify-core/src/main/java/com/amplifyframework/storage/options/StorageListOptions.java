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
 * Options to specify attributes of list API invocation.
 */
public final class StorageListOptions implements Options {
    private final StorageAccessLevel accessLevel;
    private final String targetIdentityId;

    StorageListOptions(final Builder builder) {
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
     * Gets the target identity id.
     * @return target identity id
     */
    public String getTargetIdentityId() {
        return targetIdentityId;
    }

    /**
     * Factory method to return an {@link StorageListOptions.Builder} instance
     * which may be used to configure and build an immutable {@link StorageListOptions} object.
     * @return Builder used to construct {@link StorageListOptions}
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Factory method to create a simple, defaulted instance of the
     * {@link StorageListOptions}.
     * @return Default storage list options instance
     */
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

        Builder() {
        }

        /**
         * Configures the storage access level.
         * @param accessLevel Storage access level
         * @return Builder instance for fluent chaining
         */
        public Builder accessLevel(StorageAccessLevel accessLevel) {
            this.accessLevel = accessLevel;
            return this;
        }

        /**
         * Configures the target identity ID.
         * @param targetIdentityId target identity ID
         * @return current Builder instance, for fluent chaining
         */
        public Builder targetIdentityId(String targetIdentityId) {
            this.targetIdentityId = targetIdentityId;
            return this;
        }

        StorageAccessLevel getAccessLevel() {
            return accessLevel;
        }

        String getTargetIdentityId() {
            return targetIdentityId;
        }

        /**
         * Constructs a new immutable instance of the {@link StorageListOptions},
         * using the values that have been configured on the current instance of this
         * {@link StorageListOptions.Builder} instance, via prior method calls.
         * @return A new immutable instance of {@link StorageListOptions}.
         */
        public StorageListOptions build() {
            return new StorageListOptions(this);
        }
    }
}
