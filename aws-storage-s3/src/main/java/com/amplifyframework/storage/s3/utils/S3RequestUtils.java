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

package com.amplifyframework.storage.s3.utils;

import androidx.annotation.NonNull;

import com.amplifyframework.storage.StorageAccessLevel;

import java.util.Locale;

/**
 * A utility to help form requests to S3.
 */
public final class S3RequestUtils {

    @SuppressWarnings("WhitespaceAround") // Looks better this way
    private S3RequestUtils() {}

    /**
     * Amplify Storage implementation with S3 integrates access level
     * and user information into the key.
     *
     * This method helps construct a correctly formatted key to give
     * user the correct level of access into the bucket.
     *
     * PUBLIC access level follows standard S3 key format whereas
     * PRIVATE/PROTECTED access level integrates user information
     * into the key prefix. For example:
     *
     *  - public service key:    "public/{key}"
     *  - protected service key: "protected/{userID}/{key}"
     *  - private service key:   "private/{userID}/{key}"
     *
     * @param accessLevel Storage access level of the request
     * @param identityId Identity ID of the user
     * @param key User-friendly key to access the item
     * @return Formatted key to be used internally by S3 plugin
     */
    @NonNull
    public static String getServiceKey(
            @NonNull StorageAccessLevel accessLevel,
            @NonNull String identityId,
            @NonNull String key
    ) {
        return getAccessLevelPrefix(accessLevel, identityId) + key;
    }

    @NonNull
    private static String getAccessLevelPrefix(
            @NonNull StorageAccessLevel accessLevel,
            @NonNull String identityId
    ) {
        if (accessLevel.equals(StorageAccessLevel.PRIVATE) || accessLevel.equals(StorageAccessLevel.PROTECTED)) {
            return accessLevel.name().toLowerCase(Locale.US) + "/" + identityId + "/";
        } else {
            return accessLevel.name().toLowerCase(Locale.US) + "/";
        }
    }
}
