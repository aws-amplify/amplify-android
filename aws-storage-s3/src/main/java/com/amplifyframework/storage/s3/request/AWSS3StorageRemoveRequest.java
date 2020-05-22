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

package com.amplifyframework.storage.s3.request;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.amplifyframework.storage.StorageAccessLevel;

/**
 * Parameters to provide to S3 that describe a request to remove a file.
 */
public final class AWSS3StorageRemoveRequest {
    private final String key;
    private final StorageAccessLevel accessLevel;
    private final String targetIdentityId;

    /**
     * Constructs a new AWSS3StorageRemoveRequest.
     * @param key key for item to download
     * @param accessLevel Storage access level
     * @param targetIdentityId If set, this should override the current user's identity ID.
     *                         If null, the operation will fetch the current identity ID.
     */
    public AWSS3StorageRemoveRequest(
            @NonNull String key,
            @NonNull StorageAccessLevel accessLevel,
            @Nullable String targetIdentityId
    ) {
        this.key = key;
        this.accessLevel = accessLevel;
        this.targetIdentityId = targetIdentityId;
    }

    /**
     * Gets the access level.
     * @return Access level
     */
    @NonNull
    public StorageAccessLevel getAccessLevel() {
        return accessLevel;
    }

    /**
     * Gets the storage key.
     * @return key
     */
    @NonNull
    public String getKey() {
        return key;
    }

    /**
     * Gets the target identity id override. If null, the operation gets the default, current user's identity ID.
     * @return target identity id override
     */
    @Nullable
    public String getTargetIdentityId() {
        return targetIdentityId;
    }
}

