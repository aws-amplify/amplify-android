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

package com.amplifyframework.storage.options;

import androidx.annotation.Nullable;

import com.amplifyframework.core.async.Options;
import com.amplifyframework.storage.StorageAccessLevel;

/**
 * Storage options interface requires that every
 * storage operation will inspect the provided options
 * instance for access level and target ID.
 */
abstract class StorageOptions implements Options {
    private final StorageAccessLevel accessLevel;
    private final String targetIdentityId;

    StorageOptions(StorageAccessLevel accessLevel,
                   String targetIdentityId) {
        this.accessLevel = accessLevel;
        this.targetIdentityId = targetIdentityId;
    }

    /**
     * Gets the storage access level.
     * @return Storage access level
     */
    @Nullable
    public StorageAccessLevel getAccessLevel() {
        return accessLevel;
    }

    /**
     * Gets the target identity id.
     * @return target identity id
     */
    @Nullable
    public String getTargetIdentityId() {
        return targetIdentityId;
    }
}
