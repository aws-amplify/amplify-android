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

import android.text.TextUtils;

import com.amplifyframework.core.Amplify;
import com.amplifyframework.logging.LoggingPlugin;

import java.util.List;

/**
 * Retrieves and formats the logs to be displayed on the developer menu.
 */
public class RetrieveLogs {

    /**
     * Returns a String representation of all of the stored logs.
     * @return the stored logs as a String.
     */
    public String getLogs() {
        String persistentPluginKey = new PersistentLogStoragePlugin().getPluginKey();
        LoggingPlugin<?> persistentLogPlugin = Amplify.Logging.getPlugin(persistentPluginKey);
        List<LogEntry> logs = ((PersistentLogStoragePlugin) persistentLogPlugin).getLogs();
        if (logs.isEmpty()) {
            return "No logs to display.";
        } else {
            return TextUtils.join("", logs);
        }
    }
}
