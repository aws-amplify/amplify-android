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

package com.amplifyframework.storage.s3.request;

import androidx.annotation.NonNull;

import com.amplifyframework.storage.StorageAccessLevel;

/**
 * Parameters to provide to S3 that describe a request to retrieve
 * pre-signed object URL.
 */
public final class AWSS3StorageGetPresignedUrlRequest {
    private final String key;
    private final StorageAccessLevel accessLevel;
    private final String targetIdentityId;
    private final int expires;

    /**
     * Constructs a new AWSS3StorageGetUrlRequest.
     * @param key key for item to obtain URL for
     * @param accessLevel Storage access level
     * @param targetIdentityId The user id for the user this URL should be generated for
     *                         (to override it from assuming the currently logged in user)
     * @param expires The number of seconds before the URL expires
     */
    public AWSS3StorageGetPresignedUrlRequest(
            @NonNull String key,
            @NonNull StorageAccessLevel accessLevel,
            @NonNull String targetIdentityId,
            int expires) {
        this.key = key;
        this.accessLevel = accessLevel;
        this.targetIdentityId = targetIdentityId;
        this.expires = expires;
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
     * Gets the target identity id.
     * @return target identity id
     */
    @NonNull
    public String getTargetIdentityId() {
        return targetIdentityId;
    }

    /**
     * Gets the number of seconds before the URL expires.
     * @return expiration seconds
     */
    public int getExpires() {
        return expires;
    }
}

