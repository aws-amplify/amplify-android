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

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.text.TextUtils;
import androidx.annotation.Nullable;

import com.amplifyframework.AmplifyException;
import com.amplifyframework.core.Amplify;
import com.amplifyframework.logging.LogLevel;
import com.amplifyframework.logging.Logger;
import com.amplifyframework.util.Empty;

import java.util.List;
import java.util.Locale;

/**
 * Manage the display and shake detection behavior for the
 * developer menu.
 */
public final class DeveloperMenu implements ShakeDetector.Listener {
    private static final Logger LOG = Amplify.Logging.forNamespace("amplify:devmenu");
    // An instance of DeveloperMenu.
    private static DeveloperMenu sInstance;
    // Indicates whether the developer menu is visible.
    private boolean visible;
    // Action to take when the developer menu should be hidden.
    private HideAction hideAction;
    // Android Context associated with the application.
    private Context context;
    // Used to store the logs to be displayed on the developer menu.
    private PersistentLogStoragePlugin loggingPlugin;

    /**
     * Constructs a new DeveloperMenu.
     * @param context Android Context
     */
    private DeveloperMenu(Context context) {
        this.context = context.getApplicationContext();
    }

    /**
     * Returns an instance of DeveloperMenu.
     * @param context Android Context
     * @return a DeveloperMenu
     */
    public static DeveloperMenu singletonInstance(Context context) {
        if (sInstance == null) {
            sInstance = new DeveloperMenu(context);
        }
        return sInstance;
    }

    /**
     * Allows the developer menu to be activated if the app is in a debuggable build.
     * @throws AmplifyException if attempting to enable the developer menu fails
     */
    public void enableDeveloperMenu() throws AmplifyException {
        if ((context.getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE) != 0) {
            loggingPlugin = new PersistentLogStoragePlugin();
            Amplify.addPlugin(loggingPlugin);
            startListening();
        }
    }

    /**
     * Start listening for a shake event.
     */
    public void startListening() {
        ShakeDetector shakeDetector = new ShakeDetector(context, this);
        shakeDetector.startDetecting();
    }

    @Override
    public void onShakeDetected() {
        if (visible) {
            if (hideAction != null) {
                hideAction.hideDeveloperMenu();
            }
            visible = false;
        } else {
            Intent mainIntent = new Intent(context, DeveloperMenuActivity.class);
            mainIntent.setAction(Intent.ACTION_MAIN);
            mainIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(mainIntent);
            visible = true;
        }
    }

    /**
     * Set the visibility of the developer menu.
     * @param visible boolean indicating whether the developer menu is visible.
     */
    public void setVisible(boolean visible) {
        this.visible = visible;
    }

    /**
     * Set the action to take when the developer menu should be hidden.
     * @param action HideAction
     */
    public void setOnHideAction(HideAction action) {
        hideAction = action;
    }

    /**
     * Returns a String representation of all of the stored logs.
     * @return the stored logs as a String.
     */
    public String getLogs() {
        if (loggingPlugin == null) {
            return "No logs to display.";
        }
        List<LogEntry> logs = loggingPlugin.getLogs();
        if (logs.isEmpty()) {
            return "No logs to display.";
        } else {
            return TextUtils.join("", logs);
        }
    }

    /**
     * Returns a String representation of the stored logs that contain the given String and
     * are at the given log level, or all logs if the given String is null or the empty string
     * and the given log level is null.
     * @param searchText the text to search for in the logs
     * @param logLevel the log level of the logs to return
     * @return the stored logs as a String.
     */
    public String getFilteredLogs(@Nullable String searchText, @Nullable LogLevel logLevel) {
        if (loggingPlugin == null) {
            return "No logs to display.";
        }
        if (Empty.check(searchText) && logLevel == null) {
            return getLogs();
        }
        List<LogEntry> logs = loggingPlugin.getLogs();
        if (logs.isEmpty()) {
            return "No logs to display.";
        }
        StringBuilder logsStringBuilder = new StringBuilder();
        String lowercaseSearchText = searchText == null ? "" : searchText.toLowerCase(Locale.US);
        for (LogEntry log : logs) {
            String formattedLog = log.toString();
            if (formattedLog.toLowerCase(Locale.US).contains(lowercaseSearchText)
                    && (logLevel == null || log.getLogLevel() == logLevel)) {
                logsStringBuilder.append(formattedLog);
            }
        }
        String filteredLogs = logsStringBuilder.toString();
        return filteredLogs.isEmpty() ? "No logs match the search criteria." : filteredLogs;
    }

    /**
     * Creates and returns the issue body containing the issue description,
     * environment information, device information, and (optionally) logs.
     * @param description the description of the issue
     * @param includeLogs whether to include logs in the issue body
     * @return the issue body
     */
    public String createIssueBody(String description, boolean includeLogs) {
        EnvironmentInfo environmentInfo = new EnvironmentInfo();
        String envInfo = "*Amplify Plugins Information:*\n" + environmentInfo.getPluginVersions();
        String devEnvInfo = "";
        try {
            devEnvInfo = environmentInfo.getDeveloperEnvironmentInfo(context);
        } catch (AmplifyException error) {
            LOG.warn("Error reading developer environment information.");
        }
        if (!devEnvInfo.isEmpty()) {
            envInfo += "\n\n*Developer Environment Information:*\n" + devEnvInfo;
        }
        String deviceInfo = new DeviceInfo().toString();
        String logsText = "";
        if (includeLogs && loggingPlugin != null && !loggingPlugin.getLogs().isEmpty()) {
            logsText = "**Logs**\n```\n" + getLogs() + "```";
        }
        return String.format(Locale.US, "**Issue Description**\n%s\n\n**Environment Information**\n%s\n\n" +
                "**Device Information**\n%s\n\n%s", description, envInfo, deviceInfo, logsText);
    }

    /**
     * Copies the given String to the clipboard.
     * @param textToCopy the text to copy to the clipboard
     */
    public void copyToClipboard(String textToCopy) {
        ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clipData = ClipData.newPlainText("Copied Text", textToCopy);
        clipboard.setPrimaryClip(clipData);
    }

    /**
     * Interface to handle the action to take when the developer menu
     * should be hidden.
     */
    public interface HideAction {
        /**
         * Called when the developer menu should be hidden.
         */
        void hideDeveloperMenu();
    }
}
