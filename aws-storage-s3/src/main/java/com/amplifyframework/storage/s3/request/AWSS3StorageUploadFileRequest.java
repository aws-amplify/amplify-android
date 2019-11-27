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

import java.util.HashMap;
import java.util.Map;

/**
 * Parameters to provide to S3 that describe a request to upload a
 * file.
 */
public final class AWSS3StorageUploadFileRequest {
    private final String key;
    private final String local;
    private final StorageAccessLevel accessLevel;
    private final String targetIdentityId;
    private final String contentType;
    private final Map<String, String> metadata;


    /**
     * Constructs a new AWSS3StorageUploadFileRequest.
     * @param key key for item to upload
     * @param local Target path of file to upload
     * @param accessLevel Storage access level
     * @param targetIdentityId The user id for the user this file should be uploaded for
     *                         (to override it from assuming the currently logged in user)
     * @param contentType The standard MIME type describing the format of the object to store
     * @param metadata Metadata for the object to store
     */
    public AWSS3StorageUploadFileRequest(
            String key,
            String local,
            StorageAccessLevel accessLevel,
            String targetIdentityId,
            String contentType,
            Map<String, String> metadata
    ) {
        this.key = key;
        this.local = local;
        this.accessLevel = accessLevel;
        this.targetIdentityId = targetIdentityId;
        this.contentType = contentType;

        this.metadata = new HashMap<>();
        if (metadata != null) {
            this.metadata.putAll(metadata);
        }
    }

    /**
     * Gets the storage key.
     * @return key
     */
    public String getKey() {
        return key;
    }

    /**
     * Gets the local file path of the file to upload.
     * @return local file path
     */
    public String getLocal() {
        return local;
    }

    /**
     * Gets the access level.
     * @return Access level
     */
    public StorageAccessLevel getAccessLevel() {
        return accessLevel;
    }

    /**
     * Gets the target identity id.
     * @return target identity id
     */
    public String getTargetIdentityId() {
        return targetIdentityId;
    }

    /**
     * Gets the content type.
     * @return content type
     */
    public String getContentType() {
        return contentType;
    }

    /**
     * Gets the metadata.
     * @return metadata
     */
    public Map<String, String> getMetadata() {
        return metadata;
    }
}

