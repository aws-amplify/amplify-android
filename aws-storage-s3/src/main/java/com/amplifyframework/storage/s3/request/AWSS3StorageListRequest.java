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
import com.amplifyframework.storage.s3.options.AWSS3StoragePagedListOptions;

/**
 * Parameters to provide to S3 that describe a request to list files.
 */
public final class AWSS3StorageListRequest {
    private final String path;
    private final StorageAccessLevel accessLevel;
    private final String targetIdentityId;
    private final int pageSize;
    private final String nextToken;

    /**
     * Constructs a new AWSS3StorageListRequest.
     * Although this has public access, it is intended for internal use and should not be used directly by host
     * applications. The behavior of this may change without warning.
     *
     * @param path the path in S3 to list items from
     * @param accessLevel Storage access level
     * @param targetIdentityId If set, this should override the current user's identity ID.
     *                         If null, the operation will fetch the current identity ID.
     */
    @SuppressWarnings("deprecation")
    public AWSS3StorageListRequest(
            @NonNull String path,
            @NonNull StorageAccessLevel accessLevel,
            @Nullable String targetIdentityId
    ) {
        this.path = path;
        this.accessLevel = accessLevel;
        this.targetIdentityId = targetIdentityId;
        this.pageSize = AWSS3StoragePagedListOptions.ALL_PAGE_SIZE;
        this.nextToken = null;
    }

    /**
     * Constructs a new AWSS3StorageListRequest.
     * Although this has public access, it is intended for internal use and should not be used directly by host
     * applications. The behavior of this may change without warning.
     *
     * @param path the path in S3 to list items from
     * @param accessLevel Storage access level
     * @param targetIdentityId If set, this should override the current user's identity ID.
     *                         If null, the operation will fetch the current identity ID.
     * @param pageSize number of keys to be retrieved from s3
     * @param nextToken next continuation token to be passed to s3
     */
    public AWSS3StorageListRequest(
        @NonNull String path,
        @NonNull StorageAccessLevel accessLevel,
        @Nullable String targetIdentityId,
        int pageSize,
        @Nullable String nextToken
    ) {
        this.path = path;
        this.accessLevel = accessLevel;
        this.targetIdentityId = targetIdentityId;
        this.pageSize = pageSize;
        this.nextToken = nextToken;
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
     * Gets the path.
     * @return path
     */
    @NonNull
    public String getPath() {
        return path;
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
    * Get the pageSize, if -1 it returns all the items.
    * @return pageSize
    * */
    public int getPageSize() {
        return pageSize;
    }

    /**
    * Get the next continuation token.
    * @return next continuation token.
    * */
    @Nullable
    public String getNextToken() {
        return nextToken;
    }
}

