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

import android.content.Context;
import android.content.res.Resources;
import androidx.annotation.NonNull;

import com.amplifyframework.AmplifyException;
import com.amplifyframework.core.Amplify;
import com.amplifyframework.core.category.Category;
import com.amplifyframework.core.plugin.Plugin;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Scanner;

/**
 * Contains information about the plugin versions added to the application
 * and the developer's environment.
 */
public final class EnvironmentInfo {
    // Name of the file that contains the developer environment information.
    private static final String DEV_ENV_INFO_FILE_NAME = "localenvinfo";
    // A map from developer environment information items to the formatted name of that item.
    private final Map<String, String> devEnvironmentItems;

    /**
     * Constructs a new EnvironmentInfo instance.
     */
    public EnvironmentInfo() {
        devEnvironmentItems = new HashMap<>();
        devEnvironmentItems.put("nodejsVersion", "Node.js Version");
        devEnvironmentItems.put("npmVersion", "NPM Version");
        devEnvironmentItems.put("amplifyCliVersion", "Amplify CLI Version");
        devEnvironmentItems.put("androidStudioVersion", "Android Studio Version");
        devEnvironmentItems.put("osName", "OS");
        devEnvironmentItems.put("osVersion", "OS Version");
    }

    /**
     * Returns a String representation of the version of each plugin added, or
     * "No plugins added." if no plugins were added.
     * @return version information for each plugin added
     */
    public String getPluginVersions() {
        String pluginVersions = getCategoryPluginVersions(Amplify.Analytics) +
                getCategoryPluginVersions(Amplify.API) +
                getCategoryPluginVersions(Amplify.Auth) +
                getCategoryPluginVersions(Amplify.Logging) +
                getCategoryPluginVersions(Amplify.Storage) +
                getCategoryPluginVersions(Amplify.Hub) +
                getCategoryPluginVersions(Amplify.DataStore) +
                getCategoryPluginVersions(Amplify.Predictions);
        return pluginVersions.isEmpty() ? "No plugins added." : pluginVersions;
    }

    /**
     * Returns a String representation of information about the developer's environment, or
     * an empty string if the developer environment information could not be found.
     * @param context an Android Context
     * @return developer environment information
     * @throws AmplifyException if the developer environment information could not be read
     */
    public String getDeveloperEnvironmentInfo(@NonNull Context context) throws AmplifyException {
        Context appContext = Objects.requireNonNull(context).getApplicationContext();
        Resources resources = appContext.getApplicationContext().getResources();
        int resourceId = resources.getIdentifier(DEV_ENV_INFO_FILE_NAME, "raw", appContext.getPackageName());
        if (resourceId == 0) {
            throw new AmplifyException("Error reading the developer environment information.",
                    "Check that the resource " + DEV_ENV_INFO_FILE_NAME + ".json exists.");
        }
        InputStream inputStream = resources.openRawResource(resourceId);
        Scanner in = new Scanner(inputStream);
        StringBuilder stringBuilder = new StringBuilder();
        while (in.hasNextLine()) {
            stringBuilder.append(in.nextLine());
        }
        in.close();
        JSONObject envInfo;
        try {
            envInfo = new JSONObject(stringBuilder.toString());
        } catch (JSONException jsonError) {
            throw new AmplifyException("Error reading the developer environment information.", jsonError,
                    "Check that " + DEV_ENV_INFO_FILE_NAME + ".json is properly formatted");
        }
        StringBuilder formattedEnvInfo = new StringBuilder();
        for (String envItem : devEnvironmentItems.keySet()) {
            if (envInfo.has(envItem)) {
                String envValue;
                try {
                    envValue = envInfo.getString(envItem);
                } catch (JSONException jsonError) {
                    throw new AmplifyException("Error reading the developer environment information.", jsonError,
                            "Check that " + DEV_ENV_INFO_FILE_NAME + ".json is properly formatted");
                }
                String devEnvInfoItem = devEnvironmentItems.get(envItem) + ": " + envValue + "\n";
                formattedEnvInfo.append(devEnvInfoItem);
            }
        }
        return formattedEnvInfo.toString();
    }

    /**
     * Returns a String representation of the version for each added plugin
     * that belongs to the given category.
     * @param category Category to retrieve plugin versions from
     * @return the version for each added plugin in the given category
     */
    private String getCategoryPluginVersions(Category<?> category) {
        StringBuilder pluginVersions = new StringBuilder();
        for (Plugin<?> plugin : category.getPlugins()) {
            String versionInfo = plugin.getPluginKey() + ": " + plugin.getVersion() + "\n";
            pluginVersions.append(versionInfo);
        }
        return pluginVersions.toString();
    }
}
