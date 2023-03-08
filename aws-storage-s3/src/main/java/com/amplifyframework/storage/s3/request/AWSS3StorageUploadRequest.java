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
import com.amplifyframework.storage.s3.ServerSideEncryption;

import java.util.HashMap;
import java.util.Map;

/**
 * Parameters to provide to S3 that describe a request to upload.
 * @param <L> object to upload (e.g. File or InputStream)
 */
public final class AWSS3StorageUploadRequest<L> {
    private final String key;
    private final L local;
    private final StorageAccessLevel accessLevel;
    private final String targetIdentityId;
    private final String contentType;
    private final ServerSideEncryption serverSideEncryption;
    private final Map<String, String> metadata;
    private final boolean useAccelerateEndpoint;

    /**
     * Constructs a new AWSS3StorageUploadRequest.
     * Although this has public access, it is intended for internal use and should not be used directly by host
     * applications. The behavior of this may change without warning.
     *
     * @param key key for item to upload
     * @param local object to upload (e.g. File or InputStream)
     * @param accessLevel Storage access level
     * @param targetIdentityId If set, this should override the current user's identity ID.
     *                         If null, the operation will fetch the current identity ID.
     * @param contentType The standard MIME type describing the format of the object to store
     * @param serverSideEncryption server side encryption type for the current storage bucket
     * @param metadata Metadata for the object to store
     * @param useAccelerateEndpoint flag to use acceleration endpoint.
     */
    public AWSS3StorageUploadRequest(
            @NonNull String key,
            @NonNull L local,
            @NonNull StorageAccessLevel accessLevel,
            @Nullable String targetIdentityId,
            @Nullable String contentType,
            @NonNull ServerSideEncryption serverSideEncryption,
            @Nullable Map<String, String> metadata,
            boolean useAccelerateEndpoint
    ) {
        this.key = key;
        this.local = local;
        this.accessLevel = accessLevel;
        this.targetIdentityId = targetIdentityId;
        this.contentType = contentType;
        this.serverSideEncryption = serverSideEncryption;
        this.metadata = new HashMap<>();
        if (metadata != null) {
            this.metadata.putAll(metadata);
        }
        this.useAccelerateEndpoint = useAccelerateEndpoint;
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
     * Returns the local object to be uploaded (e.g. File or InputStream).
     * @return the local object to be uploaded (e.g. File or InputStream).
     */
    @NonNull
    public L getLocal() {
        return local;
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
     * Gets the target identity id override. If null, the operation gets the default, current user's identity ID.
     * @return target identity id override
     */
    @Nullable
    public String getTargetIdentityId() {
        return targetIdentityId;
    }

    /**
     * Gets the content type.
     * @return content type
     */
    @Nullable
    public String getContentType() {
        return contentType;
    }

    /**
     * Gets the server side encryption algorithm.
     * @return server side encryption algorithm
     */
    @NonNull
    public ServerSideEncryption getServerSideEncryption() {
        return serverSideEncryption;
    }

    /**
     * Gets the metadata.
     * @return metadata
     */
    @NonNull
    public Map<String, String> getMetadata() {
        return metadata;
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

