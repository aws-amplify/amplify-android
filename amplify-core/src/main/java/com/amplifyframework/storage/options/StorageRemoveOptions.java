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
    private final Options options;

    StorageRemoveOptions(Builder builder) {
        this.accessLevel = builder.getAccessLevel();
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
     * Gets the storage options.
     * @return Storage options
     */
    public Options getOptions() {
        return options;
    }

    /**
     * Gets a new instance of {@link StorageRemoveOptions.Builder}, which can be used
     * to configure and construct new immutable instances of {@link StorageRemoveOptions}.
     * @return a new instance of the {@link StorageRemoveOptions.Builder}.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Creates a default instance of the {@link StorageRemoveOptions}, which has
     * no special/unique configurations.
     * @return Default instance of StorageRemoveOptions
     */
    public static StorageRemoveOptions defaultInstance() {
        return builder().build();
    }

    /**
     * Provides a mechanism to prepare and construct immutable instances
     * of {@link StorageRemoveOptions}, using fluent method configuration API
     * via chained method invocations.
     */
    public static final class Builder {
        private StorageAccessLevel accessLevel;
        private Options options;

        Builder() {
        }

        /**
         * Configures the storage access level to use on newly built StorageRemoveOptions instances.
         * @param accessLevel Storage access level
         * @return Current Builder instance, for fluent method chaining
         */
        public Builder accessLevel(StorageAccessLevel accessLevel) {
            this.accessLevel = accessLevel;
            return this;
        }

        /**
         * Configures the options to use on newly built StorageRemoveOptions instances.
         * @param options Options bundle
         * @return Current Builder instance, for fluent method chaining
         */
        public Builder options(Options options) {
            this.options = options;
            return this;
        }

        StorageAccessLevel getAccessLevel() {
            return accessLevel;
        }

        Options getOptions() {
            return options;
        }

        /**
         * Constructs and returns a new immutable {@link StorageRemoveOptions} instance, using the
         * values that had been configured on the current builder instance.
         * @return A new immutable instance of the {@link StorageRemoveOptions}.
         */
        public StorageRemoveOptions build() {
            return new StorageRemoveOptions(this);
        }
    }
}
