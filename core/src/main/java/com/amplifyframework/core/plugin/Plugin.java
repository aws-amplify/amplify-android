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

package com.amplifyframework.core.plugin;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;

import com.amplifyframework.AmplifyException;
import com.amplifyframework.core.Amplify;
import com.amplifyframework.core.category.CategoryTypeable;

import org.json.JSONObject;

/**
 * A plugin is an implementation of a category's behavior. You can implement
 * custom behavior for an Amplify category by writing a plugin. Plugins are
 * added into the Amplify system before configuration, via calls to
 * {@link Amplify#addPlugin(Plugin)}. You can remove a plugin from the system
 * with {@link Amplify#removePlugin(Plugin)}.
 *
 * The lifecycle of a plugin is:
 * 1. {@link #configure(JSONObject, Context)} is called, loading configuration data
 *    in a synchronous way. Do not do "heavy lifting" here, or you may cause ANRs.
 * 2. {@link #initialize(Context)} is called, providing an opportunity to initialize
 *    your plugin using the configuration that was loaded in the previous step.
 *    While this method is called synchronously from the Plugin's standpoint, it is
 *    executed async, in the background, by the Amplify framework.
 *
 * @param <E> The type of escape hatch provided by this plugin
 */
@SuppressWarnings("unused") // This is a public API.
public interface Plugin<E> extends CategoryTypeable {

    /**
     * Gets a key which uniquely identifies the plugin instance.
     * @return the identifier that identifies the plugin implementation
     */
    @NonNull
    String getPluginKey();

    /**
     * Configure the plugin with customized configuration object. A
     * plugin may or may not require plugin configuration, so see
     * the documentation for details.
     *
     * This hook provides a good opportunity to instantiate resources.
     * Any long-lived initialization should take place in {@link #initialize(Context)}, instead.
     * @param pluginConfiguration plugin-specific configuration data
     * @param context An Android Context
     * @throws AmplifyException an error is encountered during configuration.
     */
    void configure(JSONObject pluginConfiguration, @NonNull Context context) throws AmplifyException;

    /**
     * Initializes the plugin.
     * Perform any "heavy lifting" here.
     * @param context An Android Context
     * @throws AmplifyException On initialization failure
     */
    @WorkerThread
    void initialize(@NonNull Context context) throws AmplifyException;

    /**
     * Returns escape hatch for plugin to enable lower-level client use-cases.
     * @return the client used by category plugin; null, if there is no escape hatch
     */
    @Nullable
    E getEscapeHatch();

    /**
     * Returns the plugin version.
     * @return the plugin version
     */
    @NonNull
    String getVersion();

}
