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
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.amazonaws.amplify.core.BuildConfig;

/**
 * A utility to construct a User-Agent header, to be sent with all network operations.
 */
public final class UserAgent {
    private static String instance = null;

    @SuppressWarnings("checkstyle:all") private UserAgent() {}

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
                .systemName(System.getProperty("os.name"))
                .systemVersion(System.getProperty("os.version"))
                .javaVmName(System.getProperty("java.vm.name"))
                .javaVmVersion(System.getProperty("java.vm.version"))
                .javaVersion(System.getProperty("java.version"))
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
        private String javaVmName;
        private String javaVmVersion;
        private String javaVersion;
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

        Builder javaVmName(String javaVmName) {
            this.javaVmName = sanitize(javaVmName);
            return this;
        }

        Builder javaVmVersion(String javaVmVersion) {
            this.javaVmVersion = sanitize(javaVmVersion);
            return this;
        }

        Builder javaVersion(String javaVersion) {
            this.javaVersion = sanitize(javaVersion);
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
                "%s/%s %s/%s %s/%s/%s %s_%s",
                libraryName, libraryVersion,
                systemName, systemVersion,
                javaVmName, javaVmVersion, javaVersion,
                userLanguage, userRegion
            );
        }

        @NonNull
        private static String sanitize(@Nullable String string) {
            if (string == null) {
                return "UNKNOWN";
            }

            return string.replace(' ', '_');
        }
    }
}
