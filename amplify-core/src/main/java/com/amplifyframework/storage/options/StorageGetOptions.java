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
 * Options to specify attributes of get API invocation
 */
public final class StorageGetOptions implements Options {
    private final StorageAccessLevel accessLevel;
    private final String targetIdentityId;
    private final StorageGetDestination storageGetDestination;
    private final Options options;

    StorageGetOptions(final Builder builder) {
        this.accessLevel = builder.accessLevel;
        this.targetIdentityId = builder.targetIdentityId;
        this.storageGetDestination = builder.storageGetDestination;
        this.options = builder.options;
    }

    public StorageAccessLevel getAccessLevel() {
        return accessLevel;
    }

    public String getTargetIdentityId() {
        return targetIdentityId;
    }

    public StorageGetDestination getStorageGetDestination() {
        return storageGetDestination;
    }

    public Options getOptions() {
        return options;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static StorageGetOptions create() {
        return builder().build();
    }

    public static final class Builder {
        private StorageAccessLevel accessLevel;
        private String targetIdentityId;
        private StorageGetDestination storageGetDestination;
        private Options options;

        Builder() {
        }

        public Builder accessLevel(StorageAccessLevel accessLevel) {
            this.accessLevel = accessLevel;
            return this;
        }

        public Builder targetIdentityId(String targetIdentityId) {
            this.targetIdentityId = targetIdentityId;
            return this;
        }

        public Builder storageGetDestination(StorageGetDestination storageGetDestination) {
            this.storageGetDestination = storageGetDestination;
            return this;
        }

        public Builder options(Options options) {
            this.options = options;
            return this;
        }

        public StorageGetOptions build() {
            return new StorageGetOptions(this);
        }
    }
}
