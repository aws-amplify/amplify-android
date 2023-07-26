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

package com.amplifyframework.logging;

import androidx.annotation.NonNull;
import androidx.test.core.app.ApplicationProvider;

import com.amplifyframework.util.Empty;

import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.shadows.ShadowLog;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Tests the {@link AndroidLoggingPlugin}.
 */
@RunWith(RobolectricTestRunner.class)
public class AndroidLoggingPluginTest {
    private LogOutputStream systemLog;

    /**
     * Setup logging plugin, for test. Redirect system output a buffer,
     * against which we'll be able to make assertions.
     */
    @Before
    public void setup() {
        systemLog = new LogOutputStream();
        ShadowLog.stream = new PrintStream(systemLog);
    }

    /**
     * Cleanup the shadow after each test.
     */
    @After
    public void reset() {
        ShadowLog.stream.close();
        ShadowLog.stream = null;
    }

    /**
     * By default, error logs should be loggable.
     */
    @Test
    public void productionLogsEmittedAtDefaultThreshold() {
        AndroidLoggingPlugin plugin = new AndroidLoggingPlugin();
        plugin.configure(new JSONObject(), ApplicationProvider.getApplicationContext());
        Logger logger = plugin.logger("amplify");

        logger.error("A most serious issue, indeed.");
        logger.warn("Ahh, bummer, but alright.");
        logger.info("A component initialized.");
        logger.debug("This doesn't get logged.");
        logger.verbose("This neither.");

        assertEquals(
            Arrays.asList(
                "E/amplify: A most serious issue, indeed.",
                "W/amplify: Ahh, bummer, but alright.",
                "I/amplify: A component initialized."
            ),
            systemLog.getLines()
        );
    }

    /**
     * When the log-level is set to {@link LogLevel#VERBOSE}, all content is emitted.
     */
    @Test
    public void allContentLoggedAtThresholdVerbose() {
        AndroidLoggingPlugin plugin = new AndroidLoggingPlugin(LogLevel.VERBOSE);
        plugin.configure(new JSONObject(), ApplicationProvider.getApplicationContext());
        Logger logger = plugin.logger("kool-module");

        logger.verbose("This logs");
        logger.debug("This too");
        logger.info(null);
        logger.warn("Getting serious...");
        logger.error("It. Got. Serious.");

        assertEquals(
            Arrays.asList(
                "V/kool-module: This logs",
                "D/kool-module: This too",
                "I/kool-module: null",
                "W/kool-module: Getting serious...",
                "E/kool-module: It. Got. Serious."
            ),
            systemLog.getLines()
        );
    }

    /**
     * When the log-level is set to {@link LogLevel#NONE}, logging is disabled.
     */
    @Test
    public void noContentLoggedAtThresholdNone() {
        AndroidLoggingPlugin plugin = new AndroidLoggingPlugin(LogLevel.NONE);
        plugin.configure(new JSONObject(), ApplicationProvider.getApplicationContext());
        Logger logger = plugin.logger("logging-test");

        logger.error("An error happened!");
        logger.info(null);
        logger.warn("Uh oh, not great...");

        assertTrue(systemLog.getLines().isEmpty());
    }

    static final class LogOutputStream extends ByteArrayOutputStream {
        private LogOutputStream() {}

        @NonNull
        List<String> getLines() {
            final List<String> lines = new ArrayList<>();
            for (String line : toString().split("[\\r\\n]+")) {
                if (!Empty.check(line)) {
                    lines.add(line);
                }
            }
            return lines;
        }
    }
}
