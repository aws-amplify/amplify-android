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
import androidx.annotation.NonNull;
import androidx.core.util.ObjectsCompat;

import com.amplifyframework.core.plugin.Plugin;
import com.amplifyframework.core.plugin.PluginException;
import com.amplifyframework.logging.LoggingPlugin;

import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.util.UUID;

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
     */
    @Test
    public void pluginCanBeAddedAndRemoved() {
        // Arrange a plugin
        final SimpleLoggingPlugin loggingPlugin = new SimpleLoggingPlugin();

        // Add it, and assert that it was added...
        Amplify.addPlugin(loggingPlugin);
        assertEquals(1, Amplify.Logging.getPlugins().size());
        assertEquals(loggingPlugin, Amplify.Logging.getPlugin(loggingPlugin.getPluginKey()));

        // Remove it, make sure it's not there anymore.
        Amplify.removePlugin(loggingPlugin);
        assertTrue(Amplify.Logging.getPlugins().isEmpty());
    }

    /**
     * Some plugin (doesn't really matter what kind) that we can use
     * to test the functionality of {@link Amplify#addPlugin(Plugin)}
     * and {@link Amplify#removePlugin(Plugin)}.
     */
    static final class SimpleLoggingPlugin extends LoggingPlugin<Void> {
        private final String uuid;

        SimpleLoggingPlugin() {
            this.uuid = UUID.randomUUID().toString();
        }

        @Override
        public boolean equals(Object thatObject) {
            if (this == thatObject) {
                return true;
            }
            if (thatObject == null || getClass() != thatObject.getClass()) {
                return false;
            }

            SimpleLoggingPlugin that = (SimpleLoggingPlugin) thatObject;

            return ObjectsCompat.equals(uuid, that.uuid);
        }

        @Override
        public int hashCode() {
            return uuid != null ? uuid.hashCode() : 0;
        }

        @Override
        public String getPluginKey() {
            return uuid;
        }

        @Override
        public void configure(
                @NonNull final JSONObject pluginConfiguration,
                final Context context)
                throws PluginException {
            // No configuration for this one. Cool, huh?
        }

        @Override
        public Void getEscapeHatch() {
            // No escape hatch, either. Sweet.
            return null;
        }
    }
}

