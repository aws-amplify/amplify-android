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

package com.amplifyframework.logging;

import android.content.Context;
import android.util.Log;

import com.amplifyframework.AmplifyException;
import com.amplifyframework.core.category.CategoryConfiguration;
import com.amplifyframework.testutils.random.RandomString;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.util.List;

import static androidx.test.core.app.ApplicationProvider.getApplicationContext;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Tests the plugin management and configuration facilities of the {@link LoggingCategory}.
 */
@RunWith(RobolectricTestRunner.class)
public final class LoggingCategoryTest {
    private FakeLogger arrangedDefaultLogger;
    private LoggingCategory realLoggingCategory;

    /**
     * Sets up the object test, an {@link LoggingCategory}.
     * It uses a faked logging plugin. The fake logging plugin uses fake logger.
     * The fake logger has a convenient ability to capture and replay logs that have been
     * passed to it. This is useful for making assertions about what has been logged.
     */
    @Before
    public void setup() {
        this.arrangedDefaultLogger = FakeLogger.instance(RandomString.string(), LogLevel.VERBOSE);
        FakeLoggingPlugin<?> plugin = FakeLoggingPlugin.instance(arrangedDefaultLogger);
        this.realLoggingCategory = new LoggingCategory(plugin);
    }

    /**
     * Before {@link LoggingCategory#configure(CategoryConfiguration, Context)} is called,
     * the {@link LoggingCategoryBehavior}s should be available. They should proxy into a
     * default plugin.
     */
    @Test
    public void defaultPluginUsedBeforeInitialization() {
        // Arrange: Category is NOT configured
        // loggingCategory.configure(configuration, context); // note well: this does not occur

        // Act: log a warning, before configure() is called on the category
        String message = RandomString.string();
        Throwable issue = new RuntimeException(RandomString.string());
        realLoggingCategory.logger(RandomString.string()).warn(message, issue);

        // Assert: one log was emitted via the default logging plugin.
        List<FakeLogger.Log> capturedLogs = arrangedDefaultLogger.getLogs();
        assertEquals(1, capturedLogs.size());

        // Assert: the log line was a warning, with content matching the action of the test
        FakeLogger.Log firstLog = capturedLogs.iterator().next();
        firstLog.assertEquals(LogLevel.WARN, message, issue);
    }

    /**
     * If the category is configured, but the user did not elect any plugin,
     * then a default system logging plugin will be used.
     * @throws AmplifyException Not expected; possibly, it is thrown from the call to
     *                          {@link LoggingCategory#configure(CategoryConfiguration, Context)}.
     */
    @Test
    public void defaultPluginUsedIfNoneOtherAdded() throws AmplifyException {
        // Arrange: category is configured, but no plugins are added by the user.
        // loggingCategory.addPlugin(new SomeLoggingPlugin()); note well: This doesn't happen!
        realLoggingCategory.configure(loggingConfiguration(), getApplicationContext());

        // Act: log a warning
        String message = RandomString.string();
        Throwable issue = new RuntimeException(RandomString.string());
        realLoggingCategory.logger(RandomString.string()).warn(message, issue);

        // Assert: warning was emitted via the default logging plugin.
        List<FakeLogger.Log> capturedLogs = arrangedDefaultLogger.getLogs();
        assertEquals(1, capturedLogs.size());

        // Assert: the log line was the thing we dispatched.
        FakeLogger.Log firstLog = capturedLogs.iterator().next();
        firstLog.assertEquals(LogLevel.WARN, message, issue);
    }

    /**
     * When the user elects a plugin, that plugin is used, not the default.
     * @throws AmplifyException Not expected; possible from addPlugin() call
     */
    @Test
    public void configuredPluginUsedWhenProvided() throws AmplifyException {
        // Arrange: user adds a plugin, logging category gets configured
        FakeLogger userLogger = FakeLogger.instance(RandomString.string(), LogLevel.VERBOSE);
        FakeLoggingPlugin<Void> userPlugin = FakeLoggingPlugin.instance(userLogger);
        realLoggingCategory.addPlugin(userPlugin);
        realLoggingCategory.configure(loggingConfiguration(), getApplicationContext());
        realLoggingCategory.initialize(getApplicationContext());

        // Act: try to log something
        String message = RandomString.string();
        Throwable issue = new RuntimeException(RandomString.string());
        realLoggingCategory.logger(RandomString.string()).warn(message, issue);

        // Assert: nothing showed up via the default logging plugin
        assertTrue(arrangedDefaultLogger.getLogs().isEmpty());

        // Assert: in fact, it showed up in the newly added plugin's logger, instead.
        List<FakeLogger.Log> capturedLogs = userLogger.getLogs();
        assertEquals(1, capturedLogs.size());
        FakeLogger.Log firstLog = capturedLogs.get(0);
        firstLog.assertEquals(LogLevel.WARN, message, issue);
    }

    private static LoggingCategoryConfiguration loggingConfiguration() {
        LoggingCategoryConfiguration configuration = new LoggingCategoryConfiguration();
        try {
            configuration.populateFromJSON(new JSONObject());
        } catch (JSONException jsonException) {
            fail(Log.getStackTraceString(jsonException));
        }
        return configuration;
    }
}
