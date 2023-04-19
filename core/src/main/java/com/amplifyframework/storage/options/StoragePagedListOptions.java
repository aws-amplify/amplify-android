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

package com.amplifyframework.storage.options;

import android.annotation.SuppressLint;
import androidx.annotation.NonNull;

/**
 * Options to specify attributes of list API invocation.
 */
public class StoragePagedListOptions extends StorageOptions {
    private int pageSize;
    private String nextToken;

    /**
     * Constructs a StoragePagedListOptions instance with the
     * attributes from builder instance.
     * @param builder the builder with configured attributes
     */
    protected StoragePagedListOptions(Builder<?> builder) {
        super(builder.getAccessLevel(), builder.getTargetIdentityId());
        pageSize = builder.pageSize;
        nextToken = builder.nextToken;
    }

    /**
     * Factory method to return an {@link Builder} instance
     * which may be used to configure and build an immutable {@link StoragePagedListOptions} object.
     * @return Builder used to construct {@link StoragePagedListOptions}
     */
    public static Builder<?> builder() {
        return new Builder<>();
    }

    /**
     * Get page size.
     * @return page size to be retrieved from S3.
     */
    public int getPageSize() {
        return pageSize;
    }

    /**
     * Get next contiuatoin token.
     * @return next continuation token to be passed to S3.
     */
    public String getNextToken() {
        return nextToken;
    }

    /**
     * Used to construct instance of StorageListOptions via
     * fluent configuration methods.
     * @param <B> the type of builder to chain with
     */
    @SuppressWarnings({"unchecked", "WeakerAccess"})
    public static class Builder<B extends Builder<B>>
        extends StorageOptions.Builder<B, StoragePagedListOptions> {

        private int pageSize;
        private String nextToken;

        /**
         * Set page size for the request.
         * @param pageSize size of the page to be retrieved from S3.
         * @return Current Builder instance for fluent chaining
         */
        public B setPageSize(int pageSize) {
            this.pageSize = pageSize;
            return (B) this;
        }

        /**
         * Set next continuation token.
         * @param nextToken next contiuation token to be passed to S3.
         * @return Current Builder instance for fluent chaining
         */
        public B setNextToken(String nextToken) {
            this.nextToken = nextToken;
            return (B) this;
        }


        /**
         * Returns an instance of StoragePagedListOptions with the parameters
         * specified by this builder.
         *
         * @return a configured instance of StoragePagedListOptions
         */
        @SuppressLint("SyntheticAccessor")
        @Override
        @NonNull
        public StoragePagedListOptions build() {
            return new StoragePagedListOptions(this);
        }
    }
}
