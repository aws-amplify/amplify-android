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

package com.amplifyframework.storage.s3.options;

import androidx.annotation.NonNull;
import androidx.core.util.ObjectsCompat;

import com.amplifyframework.storage.options.StorageUploadInputStreamOptions;
import com.amplifyframework.storage.s3.ServerSideEncryption;

import java.util.Objects;

/**
 * Options to specify attributes of object upload operation to an AWS S3 bucket.
 */
public final class AWSS3StorageUploadInputStreamOptions extends StorageUploadInputStreamOptions {
    private final ServerSideEncryption serverSideEncryption;

    private AWSS3StorageUploadInputStreamOptions(final Builder builder) {
        super(builder);
        this.serverSideEncryption = builder.serverSideEncryption;
    }

    /**
     * Server side encryption algorithm.
     * @return Server side encryption algorithm
     */
    @NonNull
    public ServerSideEncryption getServerSideEncryption() {
        return serverSideEncryption;
    }

    /**
     * Factory method to create a new instance of the
     * {@link Builder}.  The builder can be
     * used to configure properties and then construct a new immutable
     * instance of the storage options that are specific to AWS S3.
     * @return An instance of the {@link Builder}
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
    public static Builder from(@NonNull final AWSS3StorageUploadInputStreamOptions options) {
        return builder()
                .accessLevel(options.getAccessLevel())
                .targetIdentityId(options.getTargetIdentityId())
                .contentType(options.getContentType())
                .serverSideEncryption(options.getServerSideEncryption())
                .metadata(options.getMetadata());
    }

    /**
     * Constructs a default instance of the {@link AWSS3StorageUploadInputStreamOptions}.
     * @return default instance of AWSS3StorageUploadInputStreamOptions
     */
    @NonNull
    public static AWSS3StorageUploadInputStreamOptions defaultInstance() {
        return builder().build();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        } else if (!(obj instanceof AWSS3StorageUploadInputStreamOptions)) {
            return false;
        } else {
            AWSS3StorageUploadInputStreamOptions that = (AWSS3StorageUploadInputStreamOptions) obj;
            return ObjectsCompat.equals(getAccessLevel(), that.getAccessLevel()) &&
                    ObjectsCompat.equals(getTargetIdentityId(), that.getTargetIdentityId()) &&
                    ObjectsCompat.equals(getContentType(), that.getContentType()) &&
                    ObjectsCompat.equals(getServerSideEncryption(), that.getServerSideEncryption()) &&
                    ObjectsCompat.equals(getMetadata(), that.getMetadata());
        }
    }

    @Override
    public int hashCode() {
        return ObjectsCompat.hash(
                getAccessLevel(),
                getTargetIdentityId(),
                getContentType(),
                getServerSideEncryption(),
                getMetadata()
        );
    }

    @NonNull
    @Override
    public String toString() {
        return "AWSS3StorageUploadInputStreamOptions {" +
                "accessLevel=" + getAccessLevel() +
                ", targetIdentityId=" + getTargetIdentityId() +
                ", contentType=" + getContentType() +
                ", serverSideEncryption=" + getServerSideEncryption().getName() +
                ", metadata=" + getMetadata() +
                '}';
    }

    /**
     * A utility that can be used to configure and construct immutable
     * instances of the {@link AWSS3StorageUploadInputStreamOptions}, by chaining
     * fluent configuration method calls.
     */
    public static final class Builder extends StorageUploadInputStreamOptions.Builder<Builder> {
        private ServerSideEncryption serverSideEncryption;

        private Builder() {
            super();
            this.serverSideEncryption = ServerSideEncryption.NONE;
        }

        /**
         * Configures the server side encryption algorithm for a new AWSS3StorageUploadInputStreamOptions instance.
         * @param serverSideEncryption server side encryption algorithm
         * @return Current Builder instance for fluent chaining
         */
        @NonNull
        public Builder serverSideEncryption(@NonNull ServerSideEncryption serverSideEncryption) {
            this.serverSideEncryption = Objects.requireNonNull(serverSideEncryption);
            return this;
        }

        @Override
        @NonNull
        public AWSS3StorageUploadInputStreamOptions build() {
            return new AWSS3StorageUploadInputStreamOptions(this);
        }
    }
}
