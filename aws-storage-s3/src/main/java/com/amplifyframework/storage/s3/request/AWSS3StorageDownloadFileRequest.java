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

import java.io.File;

/**
 * Parameters to provide to S3 that describe a request to download a
 * file.
 */
public final class AWSS3StorageDownloadFileRequest {
    private final String key;
    private final File local;
    private final StorageAccessLevel accessLevel;
    private final String targetIdentityId;
    private final boolean useAccelerateEndpoint;

    /**
     * Constructs a new AWSS3StorageDownloadFileRequest.
     * Although this has public access, it is intended for internal use and should not be used directly by host
     * applications. The behavior of this may change without warning.
     *
     * @param key key for item to download
     * @param local Target file for the downloaded file to be saved to
     * @param accessLevel Storage access level
     * @param targetIdentityId If set, this should override the current user's identity ID.
     *                         If null, the operation will fetch the current identity ID.
     * @param useAccelerateEndpoint Flag to enable acceleration mode
     */
    public AWSS3StorageDownloadFileRequest(
            @NonNull String key,
            @NonNull File local,
            @NonNull StorageAccessLevel accessLevel,
            @Nullable String targetIdentityId,
            boolean useAccelerateEndpoint
    ) {
        this.key = key;
        this.local = local;
        this.accessLevel = accessLevel;
        this.targetIdentityId = targetIdentityId;
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
     * Gets the local file where the object will be saved.
     * @return local file
     */
    @NonNull
    public File getLocal() {
        return local;
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

