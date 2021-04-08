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

import android.content.Context;
import android.os.Build;

import com.amplifyframework.AmplifyException;
import com.amplifyframework.core.plugin.Plugin;

import org.json.JSONObject;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import static androidx.test.core.app.ApplicationProvider.getApplicationContext;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;


/**
 * Tests the top-level {@link Amplify} facade.
 *
 * NOTE:
 *
 * This test uses {@link FixMethodOrder}, which is an anti-pattern.
 * Amplify is a static singleton and there is no simple way to reset its
 * state, once the class' clinit has run.
 *
 * The reconfiguration test has to run after the plugin test, otherwise the
 * plugin test will fail.
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
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
        assertEquals(loggingPlugin, Amplify.Logging.getPlugins().iterator().next());

        // Remove it, make sure it's not there anymore.
        Amplify.removePlugin(loggingPlugin);
        assertTrue(Amplify.Logging.getPlugins().isEmpty());
    }

    /**
     * It is an error to call {@link Amplify#configure(Context)} (or associated methods) more
     * than once.
     * @throws AmplifyException Not expected; this can be thrown from the first configuration
     *                          attempt according to its signature, however this behavior
     *                          would not be expected
     */
    @Test
    public void reconfigurationAttemptRaisesException() throws AmplifyException {
        AmplifyConfiguration config = AmplifyConfiguration.fromJson(new JSONObject());
        Amplify.configure(config, getApplicationContext());
        Amplify.AlreadyConfiguredException actuallyThrown =
            assertThrows(
                Amplify.AlreadyConfiguredException.class,
                () -> Amplify.configure(config, getApplicationContext())
            );

        assertEquals(
            "Amplify has already been configured.",
            actuallyThrown.getMessage()
        );
        assertEquals(
            "Remove the duplicate call to `Amplify.configure()`.",
            actuallyThrown.getRecoverySuggestion()
        );
    }

    /**
     * It is an error to call {@link Amplify#addPlugin(Plugin)}} after configuration.
     * @throws Amplify.AlreadyConfiguredException
     *  NOTE: The name of this method must result in it running last, according to {@link FixMethodOrder}
     */
    @Test
    public void secondaryAddPluginAfterConfigurationRaisesException() throws AmplifyException {
        final SimpleLoggingPlugin loggingPlugin = SimpleLoggingPlugin.instance();
        Amplify.AlreadyConfiguredException actuallyThrown =
                assertThrows(
                        Amplify.AlreadyConfiguredException.class,
                        () -> Amplify.addPlugin(loggingPlugin)

                );

        assertEquals(
                "Amplify has already been configured.",
                actuallyThrown.getMessage()
        );
        assertEquals(
                "Do not add plugins after calling `Amplify.configure()`.",
                actuallyThrown.getRecoverySuggestion()
        );
    }
}
