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

import android.annotation.SuppressLint;
import androidx.annotation.NonNull;

/**
 * Options to specify attributes of list API invocation.
 */
public final class StorageListOptions extends StorageOptions {

    private StorageListOptions(final Builder builder) {
        super(builder.getAccessLevel(), builder.getTargetIdentityId());
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
        final StorageListOptions.Builder builder = builder();
        builder.accessLevel(options.getAccessLevel());
        builder.targetIdentityId(options.getTargetIdentityId());
        return builder;
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
    public static final class Builder extends StorageOptions.Builder {
        @SuppressLint("SyntheticAccessor")
        @Override
        @NonNull
        public StorageListOptions build() {
            return new StorageListOptions(this);
        }
    }
}
