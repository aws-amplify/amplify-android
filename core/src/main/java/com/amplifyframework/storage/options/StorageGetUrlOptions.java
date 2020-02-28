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

import android.annotation.SuppressLint;
import androidx.annotation.NonNull;

/**
 * Options to specify attributes of get URL API invocation.
 */
public final class StorageGetUrlOptions extends StorageOptions {
    private final int expires;

    private StorageGetUrlOptions(final Builder builder) {
        super(builder.getAccessLevel(), builder.getTargetIdentityId());
        this.expires = builder.getExpires();
    }

    /**
     * Gets the number of seconds before the URL expires.
     * @return number of seconds before the URL expires
     */
    public int getExpires() {
        return expires;
    }

    /**
     * Returns a new Builder instance that can be used to configure
     * and build a new immutable instance of StorageGetUrlOptions.
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
    public static Builder from(@NonNull StorageGetUrlOptions options) {
        return builder()
            .accessLevel(options.getAccessLevel())
            .targetIdentityId(options.getTargetIdentityId())
            .expires(options.getExpires());
    }

    /**
     * Constructs a default instance of the {@link StorageGetUrlOptions}.
     * @return default instance of StorageGetUrlOptions
     */
    @NonNull
    public static StorageGetUrlOptions defaultInstance() {
        return builder().build();
    }

    /**
     * A utility that can be used to configure and construct immutable
     * instances of the {@link StorageGetUrlOptions}, by chaining
     * fluent configuration method calls.
     */
    public static final class Builder extends StorageOptions.Builder<Builder, StorageGetUrlOptions> {
        private int expires;

        /**
         * Configures the number of seconds left until URL expires on new
         * StorageGetUrlOptions instances.
         * @param expires Amount of seconds until URL expires
         * @return Current Builder instance, for fluent method chaining
         */
        @SuppressWarnings("WeakerAccess")
        public Builder expires(int expires) {
            this.expires = expires;
            return this;
        }

        @SuppressWarnings("WeakerAccess")
        public int getExpires() {
            return expires;
        }

        /**
         * Constructs and returns a new immutable instance of the
         * StorageGetUrlOptions, using the configurations that
         * have been provided the current instance of the Builder.
         * @return A new immutable instance of StorageGetUrlOptions
         */
        @SuppressLint("SyntheticAccessor")
        @Override
        @NonNull
        public StorageGetUrlOptions build() {
            return new StorageGetUrlOptions(this);
        }
    }
}
