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
 * Options to specify attributes of remove API invocation
 */
public final class StorageRemoveOptions implements Options {
    private final StorageAccessLevel accessLevel;
    private final Options options;

    StorageRemoveOptions(Builder builder) {
        this.accessLevel = builder.accessLevel();
        this.options = builder.options();
    }

    public StorageAccessLevel accessLevel() {
        return accessLevel;
    }

    public Options options() {
        return options;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static StorageRemoveOptions create() {
        return builder().build();
    }

    public static final class Builder {
        private StorageAccessLevel accessLevel;
        private Options options;

        Builder() {
        }

        public Builder accessLevel(StorageAccessLevel accessLevel) {
            this.accessLevel = accessLevel;
            return this;
        }

        public Builder options(Options options) {
            this.options = options;
            return this;
        }

        StorageAccessLevel accessLevel() {
            return accessLevel;
        }

        Options options() {
            return options;
        }

        public StorageRemoveOptions build() {
            return new StorageRemoveOptions(this);
        }
    }
}
