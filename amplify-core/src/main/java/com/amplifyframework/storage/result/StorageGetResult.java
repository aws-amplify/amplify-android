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

package com.amplifyframework.storage.result;

import com.amplifyframework.core.async.Result;

import java.io.File;

/**
 * The result of a call to get an item from the Storage category.
 */
public final class StorageGetResult implements Result {
    private final File file;
    private final String url;

    StorageGetResult(Builder builder) {
        this.file = builder.getFile();
        this.url = builder.getUrl();
    }

    /**
     * Gets the file.
     * @return file
     */
    public File getFile() {
        return file;
    }

    /**
     * URL for downloading file.
     * @return URL for file download
     */
    public String getUrl() {
        return url;
    }

    /**
     * Creates a new instance of an {@link StorageGetResult.Builder}, which
     * can be used to create an immutable instance of hte {@link StorageGetResult}.
     * @return A build of storage get result.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Used to configure and build immutable instances of results to get calls
     * on the Storage category.
     */
    public static final class Builder {
        private File file;
        private String url;

        Builder() {
        }

        /**
         * Configures the file handle.
         * @param file file handle
         * @return Current Builder instance for fluent chaining
         */
        public Builder file(File file) {
            this.file = file;
            return this;
        }

        /**
         * Configures the url to the storage item.
         * @param url URL to storage item
         * @return Current Builder instance for fluent chaining
         */
        public Builder url(String url) {
            this.url = url;
            return this;
        }

        File getFile() {
            return file;
        }

        String getUrl() {
            return url;
        }

        /**
         * Builds a new immutable {@link StorageGetResult} instance, using the values
         * configured on the current {@link StorageGetResult.Builder} instance.
         * @return A new immutable StorageGetResult instance
         */
        public StorageGetResult build() {
            return new StorageGetResult(this);
        }
    }
}
