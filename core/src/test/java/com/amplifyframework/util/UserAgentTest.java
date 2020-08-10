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

import com.amplifyframework.AmplifyException;
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
        assertTrue(userAgent.startsWith("amplify-android/main ("));
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
        assertTrue(userAgent.startsWith("amplify-android/main ("));
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
        assertTrue(userAgent.startsWith("amplify-flutter/" + version));
    }

    /**
     * Tests that user-agent enforces size-limit at configure time.
     * This test should fail. AWS SDK stores user-agents as VARCHAR(254),
     * so it must be smaller than that.
     * @throws AmplifyException if User-Agent configuration fails.
     */
    @SuppressWarnings("MagicNumber")
    @Test(expected = AmplifyException.class)
    public void testSizeLimit() throws AmplifyException {
        final String longVersion = new String(new byte[254]);
        platforms.put(UserAgent.Platform.FLUTTER, longVersion);
        UserAgent.configure(platforms);
    }

    /**
     * Tests that the order of platforms being prepended is reserved.
     * @throws AmplifyException if User-Agent configuration fails.
     */
    @Test
    public void testThatPlatformOrderIsReserved() throws AmplifyException {
        final StringBuilder expected = new StringBuilder();
        for (UserAgent.Platform platform : UserAgent.Platform.values()) {
            String version = RandomString.string();
            platforms.put(platform, version);
            expected.append(platform.getLibraryName())
                    .append("/")
                    .append(version)
                    .append(" ");
        }
        // Append default Android user-agent
        expected.append(UserAgent.string());

        // Configure user-agent with additional platforms
        UserAgent.configure(platforms);
        final String userAgent = UserAgent.string();
        assertEquals(expected.toString(), userAgent);
    }
}
