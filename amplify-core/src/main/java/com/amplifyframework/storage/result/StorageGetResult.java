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

public final class StorageGetResult implements Result {
    private final File file;
    private final String url;

    StorageGetResult(Builder builder) {
        this.file = builder.file();
        this.url = builder.url();
    }

    /**
     * Downloaded local file
     */
    public File file() {
        return file;
    }

    /**
     * Remote pre-signed URL for downloading file
     */
    public String url() {
        return url;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private File file;
        private String url;

        Builder() {
        }

        public Builder file(File file) {
            this.file = file;
            return this;
        }

        public Builder url(String url) {
            this.url = url;
            return this;
        }

        File file() {
            return file;
        }

        String url() {
            return url;
        }

        public StorageGetResult build() {
            return new StorageGetResult(this);
        }
    }
}
