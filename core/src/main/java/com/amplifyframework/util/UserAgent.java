/*
 * Copyright 2020 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package com.amplifyframework.util;

import android.annotation.SuppressLint;
import android.os.Build;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.amplifyframework.core.BuildConfig;

/**
 * A utility to construct a User-Agent header, to be sent with all network operations.
 */
public final class UserAgent {
    private static String instance = null;

    private UserAgent() {}

    /**
     * Gets a String to use as the value of a User-Agent header.
     * @return A value for a User-Agent header.
     */
    @SuppressLint("SyntheticAccessor")
    @NonNull
    public static String string() {
        if (instance == null) {
            instance = new UserAgent.Builder()
                .libraryName("amplify-android")
                .libraryVersion(BuildConfig.VERSION_NAME)
                .systemName("Android")
                .systemVersion(Build.VERSION.RELEASE)
                .deviceManufacturer(Build.MANUFACTURER)
                .deviceName(Build.MODEL)
                .userLanguage(System.getProperty("user.language"))
                .userRegion(System.getProperty("user.region"))
                .toString();
        }

        return instance;
    }

    @SuppressWarnings("SameParameterValue")
    private static final class Builder {
        private String libraryName;
        private String libraryVersion;
        private String systemName;
        private String systemVersion;
        private String deviceManufacturer;
        private String deviceName;
        private String userLanguage;
        private String userRegion;

        Builder libraryName(String libraryName) {
            this.libraryName = sanitize(libraryName);
            return this;
        }

        Builder libraryVersion(String libraryVersion) {
            this.libraryVersion = sanitize(libraryVersion);
            return this;
        }

        Builder systemName(String systemName) {
            this.systemName = sanitize(systemName);
            return this;
        }

        Builder systemVersion(String systemVersion) {
            this.systemVersion = sanitize(systemVersion);
            return this;
        }

        Builder deviceManufacturer(String deviceManufacturer) {
            this.deviceManufacturer = sanitize(deviceManufacturer);
            return this;
        }

        Builder deviceName(String deviceName) {
            this.deviceName = sanitize(deviceName);
            return this;
        }

        Builder userLanguage(String userLanguage) {
            this.userLanguage = sanitize(userLanguage);
            return this;
        }

        Builder userRegion(String userRegion) {
            this.userRegion = sanitize(userRegion);
            return this;
        }

        @NonNull
        @Override
        public String toString() {
            return String.format(
                "%s/%s (%s %s; %s %s; %s_%s)",
                libraryName, libraryVersion,
                systemName, systemVersion,
                deviceManufacturer, deviceName,
                userLanguage, userRegion
            );
        }

        @NonNull
        private static String sanitize(@Nullable String string) {
            if (string == null) {
                return "UNKNOWN";
            }

            return string;
        }
    }
}
