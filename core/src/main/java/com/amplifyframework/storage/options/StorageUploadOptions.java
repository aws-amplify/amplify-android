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
import androidx.core.util.ObjectsCompat;

import com.amplifyframework.util.Immutable;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Options to specify attributes of put API invocation.
 */
public abstract class StorageUploadOptions extends StorageOptions {
    private final String contentType;
    private final Map<String, String> metadata;

    /**
     * Upload options for Storage.
     * @param builder A builder to pass storage options
     * @param <B>     the type of builder to chain with
     * @param <O>     the type of StorageUploadOptions to chain with
     */
    protected <B extends Builder<B, O>, O extends StorageUploadOptions>
        StorageUploadOptions(final Builder<B, O> builder) {
        super(builder.getAccessLevel(), builder.getTargetIdentityId());
        this.contentType = builder.getContentType();
        this.metadata = builder.getMetadata();
    }

    /**
     * The standard MIME type describing the format of the object to store.
     * @return Content type
     */
    @Nullable
    public String getContentType() {
        return contentType;
    }

    /**
     * Metadata for the object to store.
     * @return metadata
     */
    @NonNull
    public Map<String, String> getMetadata() {
        return Immutable.of(metadata);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        } else if (!(obj instanceof StorageUploadOptions)) {
            return false;
        } else {
            StorageUploadOptions that = (StorageUploadOptions) obj;
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
        return "StorageUploadOptions {" +
                "accessLevel=" + getAccessLevel() +
                ", targetIdentityId=" + getTargetIdentityId() +
                ", contentType=" + getContentType() +
                ", metadata=" + getMetadata() +
                '}';
    }

    /**
     * Use to configure and build immutable instances of the
     * StorageUploadFileOptions, using fluent of property configuration
     * methods.
     * @param <B> the type of builder to chain with
     * @param <O> the type of storageOptions to chain with
     */
    @SuppressWarnings({"unchecked", "WeakerAccess"})
    public abstract static class Builder<B extends Builder<B, O>, O extends StorageOptions>
            extends StorageOptions.Builder<B, O> {
        private String contentType;
        private Map<String, String> metadata;

        /**
         * Constructs a new Builder for StorageUploadOptions.
         */
        protected Builder() {
            this.metadata = new HashMap<>();
        }

        /**
         * Configures the content type for a new StorageUploadOptions instance.
         * @param contentType Content type
         * @return Current Builder instance for fluent chaining
         */
        @NonNull
        public final B contentType(@Nullable String contentType) {
            this.contentType = contentType;
            return (B) this;
        }

        /**
         * Configures metadata for new StorageUploadOptions instance.
         * @param metadata Metadata for StorageUploadOptions
         * @return Current Builder instance for fluent method chaining
         */
        @NonNull
        public final B metadata(@NonNull Map<String, String> metadata) {
            this.metadata = new HashMap<>(Objects.requireNonNull(metadata));
            return (B) this;
        }

        @Nullable
        final String getContentType() {
            return contentType;
        }

        @NonNull
        final Map<String, String> getMetadata() {
            return Immutable.of(metadata);
        }
    }
}
