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

import com.amplifyframework.storage.options.StorageDownloadFileOptions;

/**
 * Options to specify attributes of object download operation from an AWS S3 bucket.
 */
public final class AWSS3StorageDownloadFileOptions extends StorageDownloadFileOptions {
    private final boolean useAccelerationMode;

    private AWSS3StorageDownloadFileOptions(final Builder builder) {
        super(builder);
        this.useAccelerationMode = builder.useAccelerateEndpoint;
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
    public static Builder from(@NonNull final AWSS3StorageDownloadFileOptions options) {
        return builder()
            .accessLevel(options.getAccessLevel())
            .targetIdentityId(options.getTargetIdentityId())
            .setUseAccelerateEndpoint(options.useAccelerateEndpoint());
    }

    /**
     * Constructs a default instance of the {@link AWSS3StorageDownloadFileOptions}.
     * @return default instance of AWSS3StorageDownloadFileOptions
     */
    @NonNull
    public static AWSS3StorageDownloadFileOptions defaultInstance() {
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

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        } else if (!(obj instanceof AWSS3StorageDownloadFileOptions)) {
            return false;
        } else {
            AWSS3StorageDownloadFileOptions that = (AWSS3StorageDownloadFileOptions) obj;
            return ObjectsCompat.equals(getAccessLevel(), that.getAccessLevel()) &&
                    ObjectsCompat.equals(getTargetIdentityId(), that.getTargetIdentityId());
        }
    }

    @Override
    public int hashCode() {
        return ObjectsCompat.hash(
                getAccessLevel(),
                getTargetIdentityId()
        );
    }

    @NonNull
    @Override
    public String toString() {
        return "AWSS3StorageDownloadFileOptions {" +
                "accessLevel=" + getAccessLevel() +
                ", targetIdentityId=" + getTargetIdentityId() +
                ", useAccelerationMode=" + useAccelerateEndpoint() +
                '}';
    }

    /**
     * A utility that can be used to configure and construct immutable
     * instances of the {@link AWSS3StorageDownloadFileOptions}, by chaining
     * fluent configuration method calls.
     */
    public static final class Builder extends StorageDownloadFileOptions.Builder<Builder> {
        private boolean useAccelerateEndpoint;

        /**
         * Configure to use acceleration mode on new StorageDownloadFileOptions instances.
         * @param useAccelerateEndpoint flag to represent acceleration mode for new DownloadFileOptions instance
         * @return Current Builder instance for fluent chaining
         */
        public Builder setUseAccelerateEndpoint(boolean useAccelerateEndpoint) {
            this.useAccelerateEndpoint = useAccelerateEndpoint;
            return this;
        }

        @Override
        @NonNull
        public AWSS3StorageDownloadFileOptions build() {
            return new AWSS3StorageDownloadFileOptions(this);
        }
    }
}
