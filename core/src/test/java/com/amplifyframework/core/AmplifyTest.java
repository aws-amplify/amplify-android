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

package com.amplifyframework.core;

import android.os.Build;

import com.amplifyframework.AmplifyException;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Tests the top-level {@link Amplify} facade.
 */
@Config(sdk = Build.VERSION_CODES.P, manifest = Config.NONE)
@RunWith(RobolectricTestRunner.class)
public class AmplifyTest {
    /**
     * Tests that a plugin can be added and removed via the Amplify facade.
     * @throws AmplifyException from Amplify configuration
     */
    @Test
    public void pluginCanBeAddedAndRemoved() throws AmplifyException {
        // Arrange a plugin
        final SimpleLoggingPlugin loggingPlugin = SimpleLoggingPlugin.instance();

        // Add it, and assert that it was added...
        Amplify.addPlugin(loggingPlugin);
        assertEquals(1, Amplify.Logging.getPlugins().size());
        assertEquals(loggingPlugin, Amplify.Logging.getPlugin(loggingPlugin.getPluginKey()));

        // Remove it, make sure it's not there anymore.
        Amplify.removePlugin(loggingPlugin);
        assertTrue(Amplify.Logging.getPlugins().isEmpty());
    }
}
