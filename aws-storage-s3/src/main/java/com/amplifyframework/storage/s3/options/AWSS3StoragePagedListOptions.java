/*
 * Copyright 2023 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

import com.amplifyframework.storage.options.StoragePagedListOptions;

/**
 * Options to specify attributes of object listing operation from an AWS S3 bucket.
 */
public final class AWSS3StoragePagedListOptions extends StoragePagedListOptions {
    /**
     * Constant to represent returning all the keys from S3.
     * @deprecated only for internal use.
     * */
    @Deprecated
    public static final int ALL_PAGE_SIZE = -1;

    private static final int DEFAULT_PAGE_SIZE = 1000;

    private AWSS3StoragePagedListOptions(Builder builder) {
        super(builder);
    }

    /**
     * Factory method to create a new instance of the
     * {@link Builder}.  The builder can be
     * used to configure properties and then construct a new immutable
     * instance of the storage options that are specific to AWS S3.
     * @return An instance of the {@link Builder}
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Constructs a default instance of the {@link AWSS3StoragePagedListOptions}.
     * @return default instance of AWSS3StorageListOptions
     */
    public static AWSS3StoragePagedListOptions defaultInstance() {
        return builder().setPageSize(DEFAULT_PAGE_SIZE).build();
    }

    /**
     * A utility that can be used to configure and construct immutable
     * instances of the {@link AWSS3StoragePagedListOptions}, by chaining
     * fluent configuration method calls.
     */
    public static final class Builder extends StoragePagedListOptions.Builder<Builder> {

        @NonNull
        @Override
        public AWSS3StoragePagedListOptions build() {
            return new AWSS3StoragePagedListOptions(this);
        }
    }
}
