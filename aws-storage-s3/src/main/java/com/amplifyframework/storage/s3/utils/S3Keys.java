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
 * A utility to manipulate keys used with S3.
 */
public final class S3Keys {
    private static final char BUCKET_SEPARATOR = '/';

    @SuppressWarnings("checkstyle:all") private S3Keys() {}

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
     *  - protected service key: "protected/{identityID}/{key}"
     *  - private service key:   "private/{identityID}/{key}"
     *
     * @param accessLevel Storage access level of the request
     * @param identityId Identity ID of the user
     * @param amplifyKey User-friendly key to access the item
     * @return Formatted key to be used internally by S3 plugin
     */
    @NonNull
    public static String createServiceKey(
            @NonNull StorageAccessLevel accessLevel,
            @NonNull String identityId,
            @NonNull String amplifyKey
    ) {
        return getAccessLevelPrefix(accessLevel, identityId) + BUCKET_SEPARATOR + amplifyKey;
    }

    @NonNull
    private static String getAccessLevelPrefix(
            @NonNull StorageAccessLevel accessLevel,
            @NonNull String identityId
    ) {
        if (accessLevel.equals(StorageAccessLevel.PRIVATE) || accessLevel.equals(StorageAccessLevel.PROTECTED)) {
            return accessLevel.name().toLowerCase(Locale.US) + BUCKET_SEPARATOR + identityId;
        } else {
            return accessLevel.name().toLowerCase(Locale.US);
        }
    }

    /**
     * This utility is useful for converting S3 service key back into
     * a user-friendly key for Amplify. It strips the access level
     * prefix as well as the associated identity ID.
     * @param serviceKey S3 specific key containing access level an identity ID
     * @return Amplify storage key devoid of S3 specific details
     * @throws IllegalArgumentException for wrong service key format
     */
    @NonNull
    public static String extractAmplifyKey(@NonNull String serviceKey) {
        try {
            int accessLevelIndex = serviceKey.indexOf(BUCKET_SEPARATOR);
            if (accessLevelIndex < 0) {
                throw new IllegalArgumentException("Missing access level.");
            }
            String accessLevelString = serviceKey.substring(0, accessLevelIndex).toUpperCase(Locale.US);
            StorageAccessLevel accessLevel = StorageAccessLevel.valueOf(accessLevelString);

            // public keys are formatted as "public/{key}"
            if (StorageAccessLevel.PUBLIC.equals(accessLevel)) {
                return serviceKey.substring(accessLevelIndex + 1);
            }

            // private and protected keys are formatted as "{access-level}/{identity-id}/{key}"
            int identityIdIndex = serviceKey.indexOf(BUCKET_SEPARATOR, accessLevelIndex + 1);
            if (identityIdIndex < 0) {
                throw new IllegalArgumentException("Missing identity ID.");
            }
            return serviceKey.substring(identityIdIndex + 1);
        } catch (RuntimeException exception) {
            throw new IllegalArgumentException("Service key is incorrectly formatted.", exception);
        }
    }
}
