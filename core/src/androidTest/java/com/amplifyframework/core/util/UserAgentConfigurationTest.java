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

package com.amplifyframework.core.util;

import android.content.Context;

import com.amplifyframework.AmplifyException;
import com.amplifyframework.core.Amplify;
import com.amplifyframework.core.AmplifyConfiguration;
import com.amplifyframework.core.BuildConfig;
import com.amplifyframework.core.test.R;
import com.amplifyframework.util.UserAgent;

import org.junit.BeforeClass;
import org.junit.Test;

import java.util.LinkedHashMap;

import static androidx.test.core.app.ApplicationProvider.getApplicationContext;
import static org.junit.Assert.assertTrue;

/**
 * Tests that configuring {@link UserAgent} via Amplify behaves as intended.
 */
public final class UserAgentConfigurationTest {
    /**
     * Since Amplify can only be configured once at the time of writing this test,
     * call {@link Amplify#configure(AmplifyConfiguration, Context)} once during
     * this test suite.
     * @throws AmplifyException if Amplify fails to configure.
     */
    @BeforeClass
    public static void setUpOnce() throws AmplifyException {
        Context context = getApplicationContext();
        AmplifyConfiguration config = AmplifyConfiguration.builder(context, R.raw.amplifyconfiguration)
                .addPlatform(UserAgent.Platform.FLUTTER, BuildConfig.VERSION_NAME)
                .build();
        Amplify.configure(config, context);
    }

    /**
     * Tests that the configured user-agent begins with expected platforms.
     */
    @Test
    public void testUserAgentIsGeneratedCorrectly() {
        String expectedUserAgentPrefix = UserAgent.Platform.FLUTTER.getLibraryName() + "/" +
                BuildConfig.VERSION_NAME + " " +
                UserAgent.Platform.ANDROID.getLibraryName() + "/" +
                BuildConfig.VERSION_NAME + " ("; // "(" asserts that system info follows this prefix
        assertTrue(UserAgent.string().startsWith(expectedUserAgentPrefix));
    }

    /**
     * Tests that attempting to configure {@link UserAgent} externally
     * (after configuring Amplify) fails.
     * @throws AmplifyException if configuration fails.
     */
    @Test(expected = AmplifyException.class)
    public void configuringUserAgentExternallyFails() throws AmplifyException {
        UserAgent.configure(new LinkedHashMap<>());
    }
}
