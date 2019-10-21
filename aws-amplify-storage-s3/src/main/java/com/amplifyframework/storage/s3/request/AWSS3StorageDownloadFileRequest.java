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

import com.amplifyframework.storage.StorageAccessLevel;

/**
 * Parameters to provide to S3 that describe a request to download a
 * file.
 */
public final class AWSS3StorageDownloadFileRequest {
    private final String key;
    private final String local;
    private final StorageAccessLevel accessLevel;
    private final String targetIdentityId;

    /**
     * Constructs a new AWSS3StorageDownloadFileRequest.
     * @param key key for item to download
     * @param local Target path for the downloaded file to be saved to
     * @param accessLevel Storage access level
     * @param targetIdentityId The user id for the user this file should be downloaded for
     *                         (to override it from assuming the currently logged in user)
     */
    public AWSS3StorageDownloadFileRequest(
            String key,
            String local,
            StorageAccessLevel accessLevel,
            String targetIdentityId
    ) {
        this.key = key;
        this.local = local;
        this.accessLevel = accessLevel;
        this.targetIdentityId = targetIdentityId;
    }

    /**
     * Gets the access level.
     * @return Access level
     */
    public StorageAccessLevel getAccessLevel() {
        return accessLevel;
    }

    /**
     * Gets the storage key.
     * @return key
     */
    public String getKey() {
        return key;
    }

    /**
     * Gets the target identity id.
     * @return target identity id
     */
    public String getTargetIdentityId() {
        return targetIdentityId;
    }

    /**
     * Gets the local file path where the object should be saved.
     * @return local file path
     */
    public String getLocal() {
        return local;
    }
}

