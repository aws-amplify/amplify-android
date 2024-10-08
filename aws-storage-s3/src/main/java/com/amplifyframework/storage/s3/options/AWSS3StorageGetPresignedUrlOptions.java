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

import android.annotation.SuppressLint;
import androidx.annotation.NonNull;
import androidx.core.util.ObjectsCompat;

import com.amplifyframework.storage.options.StorageGetUrlOptions;

/**
 * Options to specify attributes of presigned URL generation from an AWS S3 bucket.
 */
public final class AWSS3StorageGetPresignedUrlOptions extends StorageGetUrlOptions {
    private final boolean useAccelerationMode;
    private final boolean validateObjectExistence;

    private AWSS3StorageGetPresignedUrlOptions(final Builder builder) {
        super(builder);
        this.useAccelerationMode = builder.useAccelerateEndpoint;
        this.validateObjectExistence = builder.validateObjectExistence;
    }

    /**
     * Returns a new Builder instance that can be used to configure
     * and build a new immutable instance of AWSS3StorageGetPresignedUrlOptions.
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
    @SuppressWarnings("deprecation")
    public static Builder from(@NonNull AWSS3StorageGetPresignedUrlOptions options) {
        return builder()
            .accessLevel(options.getAccessLevel())
            .targetIdentityId(options.getTargetIdentityId())
            .expires(options.getExpires())
            .setValidateObjectExistence(options.getValidateObjectExistence())
            .expires(options.getExpires())
            .bucket(options.getBucket());
    }

    /**
     * Constructs a default instance of the {@link StorageGetUrlOptions}.
     * @return default instance of StorageGetUrlOptions
     */
    @NonNull
    public static AWSS3StorageGetPresignedUrlOptions defaultInstance() {
        return builder().build();
    }

    /**
     * Gets the flag to determine whether to use acceleration endpoint.
     *
     * @return boolean flag
     */
    public boolean useAccelerateEndpoint() {
        return useAccelerationMode;
    }

    /**
     * Gets the flag to determine whether to validate whether an S3 object exists.
     * Note: Setting this to `true` will result in a latency cost since confirming the existence
     * of the underlying S3 object will likely require a round-trip network call.
     * @return boolean flag
     */
    public boolean getValidateObjectExistence() {
        return validateObjectExistence;
    }

    @Override
    @SuppressWarnings("deprecation")
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        } else if (!(obj instanceof AWSS3StorageGetPresignedUrlOptions)) {
            return false;
        } else {
            AWSS3StorageGetPresignedUrlOptions that = (AWSS3StorageGetPresignedUrlOptions) obj;
            return ObjectsCompat.equals(getAccessLevel(), that.getAccessLevel()) &&
                    ObjectsCompat.equals(getTargetIdentityId(), that.getTargetIdentityId()) &&
                    ObjectsCompat.equals(getExpires(), that.getExpires()) &&
                    ObjectsCompat.equals(getBucket(), that.getBucket()) &&
                    ObjectsCompat.equals(getValidateObjectExistence(), that.getValidateObjectExistence());
        }
    }

    @Override
    @SuppressWarnings("deprecation")
    public int hashCode() {
        return ObjectsCompat.hash(
                getAccessLevel(),
                getTargetIdentityId(),
                getExpires(),
                getValidateObjectExistence(),
                getBucket()
        );
    }

    @NonNull
    @Override
    @SuppressWarnings("deprecation")
    public String toString() {
        return "AWSS3StorageGetPresignedUrlOptions {" +
                "accessLevel=" + getAccessLevel() +
                ", targetIdentityId=" + getTargetIdentityId() +
                ", expires=" + getExpires() +
                ", validateObjectExistence=" + getValidateObjectExistence() +
                ", bucket=" + getBucket() +
                '}';
    }

    /**
     * A utility that can be used to configure and construct immutable
     * instances of the {@link AWSS3StorageGetPresignedUrlOptions}, by chaining
     * fluent configuration method calls.
     */
    public static final class Builder extends StorageGetUrlOptions.Builder<Builder> {
        private boolean useAccelerateEndpoint;
        private boolean validateObjectExistence;

        /**
         * Configure to use acceleration mode on new StorageGetPresignedUrlOptions instances.
         * @param useAccelerateEndpoint flag to represent acceleration mode for new GetPresignedUrlOptions instance
         * @return Current Builder instance for fluent chaining
         */
        public Builder setUseAccelerateEndpoint(boolean useAccelerateEndpoint) {
            this.useAccelerateEndpoint = useAccelerateEndpoint;
            return this;
        }

        /**
         * Configure to validate object existence flag on new StorageGetPresignedUrlOptions instances.
         * @param validateObjectExistence boolean flag to represent flag to validate object existence.
         * @return Current Builder instance for fluent chaining
         */
        public Builder setValidateObjectExistence(boolean validateObjectExistence) {
            this.validateObjectExistence = validateObjectExistence;
            return this;
        }

        @Override
        @NonNull
        public AWSS3StorageGetPresignedUrlOptions build() {
            return new AWSS3StorageGetPresignedUrlOptions(this);
        }
    }
}
