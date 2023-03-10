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
import androidx.annotation.Nullable;

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
    private final boolean useAccelerateEndpoint;

    /**
     * Constructs a new AWSS3StorageGetUrlRequest.
     * Although this has public access, it is intended for internal use and should not be used directly by host
     * applications. The behavior of this may change without warning.
     *
     * @param key key for item to obtain URL for
     * @param accessLevel Storage access level
     * @param targetIdentityId If set, this should override the current user's identity ID.
     *                         If null, the operation will fetch the current identity ID.
     * @param expires The number of seconds before the URL expires
     * @param useAccelerateEndpoint Flag to enable acceleration mode
     */
    public AWSS3StorageGetPresignedUrlRequest(
            @NonNull String key,
            @NonNull StorageAccessLevel accessLevel,
            @Nullable String targetIdentityId,
            int expires,
            boolean useAccelerateEndpoint) {
        this.key = key;
        this.accessLevel = accessLevel;
        this.targetIdentityId = targetIdentityId;
        this.expires = expires;
        this.useAccelerateEndpoint = useAccelerateEndpoint;
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

    /**
     * Gets the number of seconds before the URL expires.
     * @return expiration seconds
     */
    public int getExpires() {
        return expires;
    }

    /**
     * Gets the flag to determine whether to use acceleration endpoint.
     *
     * @return boolean flag
     */
    public boolean useAccelerateEndpoint() {
        return useAccelerateEndpoint;
    }
}

