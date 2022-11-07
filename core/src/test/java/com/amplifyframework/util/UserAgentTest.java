/*
 * Copyright 2022 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

import android.os.Build;

import com.amplifyframework.AmplifyException;
import com.amplifyframework.core.BuildConfig;
import com.amplifyframework.testutils.random.RandomString;

import org.junit.Before;
import org.junit.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Tests that UserAgent singleton behaves as intended.
 */
public final class UserAgentTest {
    private Map<UserAgent.Platform, String> platforms;

    /**
     * Reset User-Agent to be ready for the next test-case.
     */
    @Before
    public void setUp() {
        UserAgent.reset();
        platforms = new LinkedHashMap<>();
    }

    /**
     * Tests that configuring User-Agent more than once fails.
     * @throws AmplifyException if User-Agent was configured more than once.
     */
    @Test(expected = AmplifyException.class)
    public void testDoubleConfiguration() throws AmplifyException {
        UserAgent.configure(platforms);
        UserAgent.configure(platforms);
    }

    /**
     * Tests that default Android user-agent is used if {@link UserAgent}
     * has not been configured.
     */
    @Test
    public void testWithoutConfiguration() {
        final String userAgent = UserAgent.string();
        assertTrue(userAgent.startsWith("amplify-android:" + BuildConfig.VERSION_NAME + " md/"));
    }

    /**
     * Tests that default Android user-agent is used if {@link UserAgent}
     * was configured with an empty map.
     * @throws AmplifyException if User-Agent configuration fails.
     */
    @Test
    public void testWithEmptyMap() throws AmplifyException {
        UserAgent.configure(platforms);

        final String userAgent = UserAgent.string();
        assertTrue(userAgent.startsWith("amplify-android:" + BuildConfig.VERSION_NAME + " md/"));
    }

    /**
     * Tests that Flutter platform information is prepended to the user-agent
     * if configured as such.
     * @throws AmplifyException if User-Agent configuration fails.
     */
    @Test
    public void testWithFlutter() throws AmplifyException {
        final String version = RandomString.string();
        platforms.put(UserAgent.Platform.FLUTTER, version);
        UserAgent.configure(platforms);

        final String userAgent = UserAgent.string();
        assertTrue(userAgent.startsWith("amplify-flutter:" + version));
        assertTrue(userAgent.contains("md/"
                + UserAgent.Platform.ANDROID.getLibraryName()
                + "/" + BuildConfig.VERSION_NAME)
        );
    }

    /**
     * Test that system properties for user agent which is picked by aws sdk to form
     * actual user agent are set.
     *
     * @throws AmplifyException if User-Agent configuration fails.
     */
    @Test
    public void testThatSystemPropertyIsSetForDefaultCase() throws AmplifyException {
        String frameworkMetadata = "amplify-android:" + BuildConfig.VERSION_NAME;
        String deviceManufacturer = Build.MANUFACTURER;
        String deviceName = Build.MODEL;
        String language = System.getProperty("user.language");
        String region = System.getProperty("user.region");

        if (language == null) {
            language = "UNKNOWN";
        }
        if (region == null) {
            region = "UNKNOWN";
        }

        UserAgent.configure(platforms);

        assertEquals(frameworkMetadata, System.getProperty("aws.frameworkMetadata"));
        assertEquals(deviceName, System.getProperty("aws.customMetadata." + deviceManufacturer));
        assertEquals(language + "_" + region, System.getProperty("aws.customMetadata.locale"));
    }

    /**
     * Test that system properties for user agent which is picked by aws sdk to form
     * actual user agent are set.
     * This case tests that if platform is set as Flutter, then framework metadata is set as flutter
     * and amplify android version is set in extra metadata
     *
     * @throws AmplifyException if User-Agent configuration fails.
     */
    @Test
    public void testThatSystemPropertyIsSetForFlutter() throws AmplifyException {
        final String version = RandomString.string();
        platforms.put(UserAgent.Platform.FLUTTER, version);
        String frameworkMetadata = "amplify-flutter:" + version;
        String deviceManufacturer = Build.MANUFACTURER;
        String deviceName = Build.MODEL;
        String language = System.getProperty("user.language");
        String region = System.getProperty("user.region");

        if (language == null) {
            language = "UNKNOWN";
        }
        if (region == null) {
            region = "UNKNOWN";
        }

        UserAgent.configure(platforms);

        assertEquals(frameworkMetadata, System.getProperty("aws.frameworkMetadata"));
        assertEquals(deviceName, System.getProperty("aws.customMetadata." + deviceManufacturer));
        assertEquals(language + "_" + region, System.getProperty("aws.customMetadata.locale"));
        assertEquals(BuildConfig.VERSION_NAME, System.getProperty("aws.customMetadata.amplify-android"));
    }
}
