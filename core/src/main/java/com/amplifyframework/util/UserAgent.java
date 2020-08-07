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

import com.amplifyframework.AmplifyException;
import com.amplifyframework.Platform;
import com.amplifyframework.core.Amplify;
import com.amplifyframework.core.BuildConfig;
import com.amplifyframework.logging.Logger;

import java.util.Map;

/**
 * A utility to construct a User-Agent header, to be sent with all network operations.
 */
public final class UserAgent {
    private static final Logger LOG = Amplify.Logging.forNamespace("amplify:core");
    private static final int SIZE_LIMIT = 254; // VARCHAR(254)

    private static String instance = null;

    private UserAgent() {}

    /**
     * Configure User-Agent singleton with Amplify configuration instance.
     * @param platformVersions A map of additional platforms that are calling this library
     *                         and their respective versions.
     * @throws AmplifyException If called twice or user-agent exceeds size limit.
     */
    public static synchronized void configure(@NonNull Map<Platform, String> platformVersions)
            throws AmplifyException {
        // Block any sub-sequent configuration call.
        if (instance != null) {
            throw new AmplifyException(
                    "User-Agent was already configured successfully.",
                    "User-Agent is configured internally during Amplify configuration. " +
                            "This method should not be called externally."
            );
        }

        // Pre-pend the additional platforms before Android user-agent
        final StringBuilder userAgent = new StringBuilder();
        for (Map.Entry<Platform, String> platform : platformVersions.entrySet()) {
            userAgent.append(String.format("%s/%s ",
                    platform.getKey().getLibraryName(),
                    platform.getValue()));
        }
        userAgent.append(forAndroid());

        // The character limit for our User-Agent header is 254 characters.
        // HTTP does not impose a maximum, but the AWS SDKs & Tools database
        // that stores metrics records declares user-agent as a VARCHAR(254)
        if (userAgent.length() > SIZE_LIMIT) {
            throw new AmplifyException(
                    "User-Agent exceeds the size limit of VARCHAR(254).",
                    AmplifyException.REPORT_BUG_TO_AWS_SUGGESTION
            );
        }

        instance = userAgent.toString();
    }

    /**
     * Gets a String to use as the value of a User-Agent header.
     * Uses default android user-agent if {@link #configure(Map)} was not called yet.
     * @return A value for a User-Agent header.
     */
    @SuppressLint("SyntheticAccessor")
    @NonNull
    public static String string() {
        if (instance == null) {
            LOG.debug("User-Agent is not yet configured. Returning default Android user-agent.");
            return forAndroid();
        }

        return instance;
    }

    private static String forAndroid() {
        return new UserAgent.Builder()
                .libraryName(Platform.ANDROID.getLibraryName())
                .libraryVersion(BuildConfig.VERSION_NAME)
                .systemName("Android")
                .systemVersion(Build.VERSION.RELEASE)
                .deviceManufacturer(Build.MANUFACTURER)
                .deviceName(Build.MODEL)
                .userLanguage(System.getProperty("user.language"))
                .userRegion(System.getProperty("user.region"))
                .toString();
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
            return string != null ? string : "UNKNOWN";
        }
    }
}
