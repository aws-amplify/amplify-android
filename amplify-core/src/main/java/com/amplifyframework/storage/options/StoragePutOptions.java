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

import com.amplifyframework.core.async.Options;
import com.amplifyframework.storage.StorageAccessLevel;

import java.util.Map;

/**
 * Options to specify attributes of put API invocation
 */
public final class StoragePutOptions implements Options {
    private final StorageAccessLevel accessLevel;
    private final String contentType;
    private final Map<String, String> metadata;
    private final Options options;

    StoragePutOptions(Builder builder) {
        this.accessLevel = builder.accessLevel();
        this.contentType = builder.contentType();
        this.metadata = builder.metadata();
        this.options = builder.options();
    }

    public StorageAccessLevel accessLevel() {
        return accessLevel;
    }

    public String contentType() {
        return contentType;
    }

    public Map<String, String> metadata() {
        return metadata;
    }

    public Options options() {
        return options;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static StoragePutOptions create() {
        return builder().build();
    }

    public static final class Builder {
        private StorageAccessLevel accessLevel;
        private String contentType;
        private Map<String, String> metadata;
        private Options options;

        Builder() {
        }

        public Builder accessLevel(StorageAccessLevel accessLevel) {
            this.accessLevel = accessLevel;
            return this;
        }

        public Builder contentType(String contentType) {
            this.contentType = contentType;
            return this;
        }

        public Builder metadata(Map<String, String> metadata) {
            this.metadata = metadata;
            return this;
        }

        public Builder options(Options options) {
            this.options = options;
            return this;
        }

        StorageAccessLevel accessLevel() {
            return accessLevel;
        }

        String contentType() {
            return contentType;
        }

        Map<String, String> metadata() {
            return metadata;
        }

        Options options() {
            return options;
        }

        public StoragePutOptions build() {
            return new StoragePutOptions(this);
        }
    }
}
