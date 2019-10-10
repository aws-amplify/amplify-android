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

package com.amplifyframework.storage.s3;

import com.amplifyframework.storage.StorageAccessLevel;

/**
 * Configuration bundle for the S3 storage plugin.
 */
public final class AWSS3StoragePluginConfiguration {
    private final StorageAccessLevel defaultAccessLevel;
    private final String region;
    private final String bucket;
    private final boolean transferAcceleration;

    /**
     * Constructs a new AWSS3StoragePluginConfiguration.
     * @param defaultAccessLevel Access level to use if none is provided
     *                           into the storage category APIs
     * @param region Region of S3 endpoint
     * @param bucket S3 bucket to use for storage
     * @param transferAcceleration Whether or not to enable transfer acceleration
     */
    public AWSS3StoragePluginConfiguration(
            StorageAccessLevel defaultAccessLevel,
            String region,
            String bucket,
            boolean transferAcceleration) {
        this.defaultAccessLevel = defaultAccessLevel;
        this.region = region;
        this.bucket = bucket;
        this.transferAcceleration = transferAcceleration;
    }

    /**
     * Gets the default access level.
     * @return Default access level
     */
    public StorageAccessLevel getDefaultAccessLevel() {
        return defaultAccessLevel;
    }

    /**
     * Gets the service region.
     * @return service region
     */
    public String getRegion() {
        return region;
    }

    /**
     * Gets the S3 bucket.
     * @return S3 bucket
     */
    public String getBucket() {
        return bucket;
    }

    /**
     * Checks if transfer acceleration is configured.
     * @return true if transfer acceleration is requested
     */
    public boolean isTransferAcceleration() {
        return transferAcceleration;
    }
}

