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

package com.amplifyframework.storage.s3.Request;

import com.amplifyframework.storage.StorageAccessLevel;

/**
 * Parameters to provide to S3 that describe a request to download a
 * file.
 */
public final class AWSS3StorageDownloadFileRequest {
    private final StorageAccessLevel accessLevel;
    private final String key;
    private final String targetIdentityId;
    private final String local;

    /**
     * Constructs a new AWSS3StorageDownloadFileRequest.
     * @param accessLevel Storage access level
     * @param key key for item to download
     * @param targetIdentityId TODO: needs documentation
     * @param local Target path for save operation locally
     */
    public AWSS3StorageDownloadFileRequest(
            StorageAccessLevel accessLevel,
            String key,
            String targetIdentityId,
            String local) {
        this.accessLevel = accessLevel;
        this.key = key;
        this.targetIdentityId = targetIdentityId;
        this.local = local;
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
     * Gets the local file path where the object is saved.
     * @return local file path
     */
    public String getLocal() {
        return local;
    }
}

