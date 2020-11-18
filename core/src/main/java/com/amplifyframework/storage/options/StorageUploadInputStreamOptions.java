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
 * Options to specify attributes of put StorageUploadInputStream API invocation.
 */
public class StorageUploadInputStreamOptions extends StorageUploadOptions {

    /**
     * Constructs a StorageUploadInputStreamOptions instance with the
     * attributes from builder instance.
     * @param builder the builder with configured attributes
     */
    protected StorageUploadInputStreamOptions(final Builder<?> builder) {
        super(builder);
    }

    /**
     * Returns a new Builder instance that can be used to configure
     * and build a new immutable instance of StorageUploadInputStreamOptions.
     * @return a new builder instance
     */
    @SuppressLint("SyntheticAccessor")
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
    public static Builder<?> from(@NonNull final StorageUploadInputStreamOptions options) {
        return builder()
            .accessLevel(options.getAccessLevel())
            .targetIdentityId(options.getTargetIdentityId())
            .contentType(options.getContentType())
            .metadata(options.getMetadata());
    }

    /**
     * Creates a new default instance of the StorageUploadInputStreamOptions.
     * @return default storage put options
     */
    @NonNull
    public static StorageUploadInputStreamOptions defaultInstance() {
        return builder().build();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        } else if (!(obj instanceof StorageUploadInputStreamOptions)) {
            return false;
        } else {
            StorageUploadInputStreamOptions that = (StorageUploadInputStreamOptions) obj;
            return ObjectsCompat.equals(getAccessLevel(), that.getAccessLevel()) &&
                    ObjectsCompat.equals(getTargetIdentityId(), that.getTargetIdentityId()) &&
                    ObjectsCompat.equals(getContentType(), that.getContentType()) &&
                    ObjectsCompat.equals(getMetadata(), that.getMetadata());
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        return ObjectsCompat.hash(
                getAccessLevel(),
                getTargetIdentityId(),
                getContentType(),
                getMetadata()
        );
    }

    /**
     * {@inheritDoc}
     */
    @NonNull
    @Override
    public String toString() {
        return "StorageUploadInputStreamOptions {" +
                "accessLevel=" + getAccessLevel() +
                ", targetIdentityId=" + getTargetIdentityId() +
                ", contentType=" + getContentType() +
                ", metadata=" + getMetadata() +
                '}';
    }

    /**
     * Use to configure and build immutable instances of the
     * StorageUploadInputStreamOptions, using fluent of property configuration
     * methods.
     * @param <B> the type of builder to chain with
     */
    @SuppressWarnings({"unchecked", "WeakerAccess"})
    public static class Builder<B extends StorageUploadInputStreamOptions.Builder<B>>
            extends StorageUploadOptions.Builder<B, StorageUploadInputStreamOptions> {

        /**
         * Builds a new immutable StorageUploadInputStreamOptions instance,
         * based on the configuration options that have been previously
         * set on this Builder instance.
         * @return A new immutable StorageUploadInputStreamOptions instance
         */
        @SuppressLint("SyntheticAccessor")
        @Override
        @NonNull
        public StorageUploadInputStreamOptions build() {
            return new StorageUploadInputStreamOptions(this);
        }
    }
}
