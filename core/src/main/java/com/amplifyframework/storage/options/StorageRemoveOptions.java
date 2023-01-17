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
import androidx.core.util.ObjectsCompat;

/**
 * Options to specify attributes of remove API invocation.
 */
public class StorageRemoveOptions extends StorageOptions {

    /**
     * Constructs a StorageRemoveOptions instance with the
     * attributes from builder instance.
     * @param builder the builder with configured attributes
     */
    protected StorageRemoveOptions(final Builder<?> builder) {
        super(builder.getAccessLevel(), builder.getTargetIdentityId());
    }

    /**
     * Factory method to create a new instance of the
     * {@link StorageRemoveOptions.Builder}.  The builder can be
     * used to configure properties and then construct a new immutable
     * instance of the StorageRemoveOptions.
     * @return An instance of the {@link StorageRemoveOptions.Builder}
     */
    @NonNull
    public static Builder<?> builder() {
        return new Builder<>();
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
    public static Builder<?> from(@NonNull final StorageRemoveOptions options) {
        return builder()
            .accessLevel(options.getAccessLevel())
            .targetIdentityId(options.getTargetIdentityId());
    }

    /**
     * Constructs a default instance of the {@link StorageRemoveOptions}.
     * @return default instance of StorageRemoveOptions
     */
    @NonNull
    public static StorageRemoveOptions defaultInstance() {
        return builder().build();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        } else if (!(obj instanceof StorageRemoveOptions)) {
            return false;
        } else {
            StorageRemoveOptions that = (StorageRemoveOptions) obj;
            return ObjectsCompat.equals(getAccessLevel(), that.getAccessLevel()) &&
                    ObjectsCompat.equals(getTargetIdentityId(), that.getTargetIdentityId());
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        return ObjectsCompat.hash(
                getAccessLevel(),
                getTargetIdentityId()
        );
    }

    /**
     * {@inheritDoc}
     */
    @NonNull
    @Override
    public String toString() {
        return "StorageRemoveOptions {" +
                "accessLevel=" + getAccessLevel() +
                ", targetIdentityId=" + getTargetIdentityId() +
                '}';
    }

    /**
     * A utility that can be used to configure and construct immutable
     * instances of the {@link StorageRemoveOptions}, by chaining
     * fluent configuration method calls.
     * @param <B> the type of builder to chain with
     */
    public static class Builder<B extends Builder<B>> extends StorageOptions.Builder<B, StorageRemoveOptions> {
        /**
         * Returns an instance of StorageRemoveOptions with the parameters
         * specified by this builder.
         * @return a configured instance of StorageRemoveOptions
         */
        @SuppressLint("SyntheticAccessor")
        @Override
        @NonNull
        public StorageRemoveOptions build() {
            return new StorageRemoveOptions(this);
        }
    }
}
