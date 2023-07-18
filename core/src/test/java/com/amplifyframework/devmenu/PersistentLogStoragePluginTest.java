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

package com.amplifyframework.devmenu;

import com.amplifyframework.logging.LogLevel;
import com.amplifyframework.logging.Logger;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.util.List;

import static org.junit.Assert.assertEquals;

/**
 * Tests the {@link PersistentLogStoragePlugin}.
 */
@RunWith(RobolectricTestRunner.class)
public final class PersistentLogStoragePluginTest {

    /**
     * Tests that a log emitted by a PersistentLogger is stored.
     */
    @Test
    public void logsAreStored() {
        PersistentLogStoragePlugin plugin = new PersistentLogStoragePlugin();
        Logger logger = plugin.logger("logging-test");
        String message = "Error log";
        Throwable throwable = new Throwable("error message");
        logger.error(message, throwable);
        List<LogEntry> logs = plugin.getLogs();
        assertEquals(1, logs.size());

        LogEntry expectedLog = new LogEntry(logs.get(0).getDate(), logger.getNamespace(), message,
                throwable, LogLevel.ERROR);
        assertEquals(expectedLog, logs.get(0));
    }
}
