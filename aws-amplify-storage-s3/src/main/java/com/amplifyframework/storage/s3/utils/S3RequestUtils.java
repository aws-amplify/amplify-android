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

import android.annotation.SuppressLint;

import com.amplifyframework.storage.StorageAccessLevel;

/**
 * A utility to help form requests to S3.
 */
public final class S3RequestUtils {
    private S3RequestUtils() { }

    @SuppressWarnings("JavadocMethod") // Not sure what all of these fields are for
    public static String getServiceKey(
            StorageAccessLevel accessLevel,
            String identityId,
            String key,
            String targetIdentityId) {
        return getAccessLevelPrefix(accessLevel, identityId, targetIdentityId) + key;
    }

    @SuppressLint({"DefaultLocale"})
    private static String getAccessLevelPrefix(
            StorageAccessLevel accessLevel,
            String identityId,
            String targetIdentityId) {
        String userId = identityId;

        if (targetIdentityId != null && !targetIdentityId.isEmpty()) {
            userId = targetIdentityId;
        }

        if (accessLevel.equals(StorageAccessLevel.PRIVATE) || accessLevel.equals(StorageAccessLevel.PROTECTED)) {
            return accessLevel.name().toLowerCase() + "/" + userId + "/";
        } else {
            return accessLevel.name().toLowerCase() + "/";
        }
    }
}

