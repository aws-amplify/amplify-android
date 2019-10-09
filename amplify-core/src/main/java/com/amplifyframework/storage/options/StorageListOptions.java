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

/**
 * Options to specify attributes of list API invocation
 */
public final class StorageListOptions implements Options {
    private final StorageAccessLevel accessLevel;
    private final String targetIdentityId;
    private final String path;
    private final Options options;

    public StorageListOptions(final Builder builder) {
        this.accessLevel = builder.accessLevel();
        this.targetIdentityId = builder.targetIdentityId();
        this.path = builder.path();
        this.options = builder.options();
    }

    public StorageAccessLevel getAccessLevel() {
        return accessLevel;
    }

    public String getTargetIdentityId() {
        return targetIdentityId;
    }

    public String getPath() {
        return path;
    }

    public Options getOptions() {
        return options;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static StorageListOptions create() {
        return builder().build();
    }

    public static final class Builder {

        private StorageAccessLevel accessLevel;
        private String targetIdentityId;
        private String path;
        private Options options;

        Builder() {
        }

        public Builder accessLevel(StorageAccessLevel accessLevel) {
            this.accessLevel = accessLevel;
            return this;
        }

        public Builder trgetIdentityId(String targetIdentityId) {
            this.targetIdentityId = targetIdentityId;
            return this;
        }

        public Builder path(String path) {
            this.path = path;
            return this;
        }

        public Builder options(Options options) {
            this.options = options;
            return this;
        }

        StorageAccessLevel accessLevel() {
            return accessLevel;
        }

        String targetIdentityId() {
            return targetIdentityId;
        }

        String path() {
            return path;
        }

        Options options() {
            return options;
        }

        public StorageListOptions build() {
            return new StorageListOptions(this);
        }
    }
}
