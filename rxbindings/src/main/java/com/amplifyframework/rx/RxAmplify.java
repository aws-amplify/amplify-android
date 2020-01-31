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

package com.amplifyframework.rx;

import android.content.Context;
import androidx.annotation.NonNull;

import com.amplifyframework.AmplifyException;
import com.amplifyframework.core.Amplify;
import com.amplifyframework.core.AmplifyConfiguration;
import com.amplifyframework.core.plugin.Plugin;

/**
 * A collection of bindings to facilitate use of the Amplify framework with Rx-centered APIs.
 */
@SuppressWarnings({"WeakerAccess", "unused"})
public final class RxAmplify {

    @SuppressWarnings("checkstyle:all") private RxAmplify() {}

    // Breaking the rules, here. Don't look.
    @SuppressWarnings({"checkstyle:all", "unused"}) public static final RxApi API = new RxApiBinding();
    @SuppressWarnings({"checkstyle:all", "unused"}) public static final RxDataStore DataStore = new RxDataStoreBinding();
    @SuppressWarnings({"checkstyle:all", "unused"}) public static final RxHub Hub = new RxHubBinding();
    @SuppressWarnings({"checkstyle:all", "unused"}) public static final RxStorage Storage = new RxStorageBinding();
    // Logging currently has no callback/async behaviors
    // Analytics currently has no callback/async behaviors

    /**
     * Read the configuration from amplifyconfiguration.json file.
     * @param context Android context required to read the contents of file
     * @throws AmplifyException thrown when already configured or there is no plugin found for a configuration
     */
    public static void configure(@NonNull Context context) throws AmplifyException {
        Amplify.configure(context);
    }

    /**
     * Configure Amplify with AmplifyConfiguration object.
     * @param configuration AmplifyConfiguration object for configuration via code
     * @param context An Android Context
     * @throws AmplifyException thrown when already configured or there is no configuration found for a plugin
     */
    public static void configure(@NonNull final AmplifyConfiguration configuration, @NonNull Context context)
            throws AmplifyException {
        Amplify.configure(configuration, context);
    }

    /**
     * Register a plugin with Amplify.
     * @param plugin an implementation of a CATEGORY_TYPE that
     *               conforms to the {@link Plugin} interface.
     * @param <P> any plugin that conforms to the {@link Plugin} interface
     * @throws AmplifyException when a plugin cannot be registered for the category type it belongs to
     *                         or when when the plugin's category type is not supported by Amplify.
     */
    public static <P extends Plugin<?>> void addPlugin(@NonNull final P plugin) throws AmplifyException {
        Amplify.addPlugin(plugin);
    }

    /**
     * Removes a plugin form the Amplify framework.
     * @param plugin The plugin to remove from the Amplify framework
     * @param <P> The type of the plugin being removed
     * @throws AmplifyException On failure to remove a plugin
     */
    public static <P extends Plugin<?>> void removePlugin(@NonNull final P plugin) throws AmplifyException {
        Amplify.removePlugin(plugin);
    }
}
