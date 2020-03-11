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
import android.text.TextUtils;
import androidx.annotation.NonNull;

import com.amplifyframework.AmplifyException;
import com.amplifyframework.analytics.AnalyticsCategory;
import com.amplifyframework.api.ApiCategory;
import com.amplifyframework.core.category.Category;
import com.amplifyframework.core.category.CategoryConfiguration;
import com.amplifyframework.core.category.CategoryType;
import com.amplifyframework.core.plugin.Plugin;
import com.amplifyframework.datastore.DataStoreCategory;
import com.amplifyframework.hub.HubCategory;
import com.amplifyframework.logging.LoggingCategory;
import com.amplifyframework.predictions.PredictionsCategory;
import com.amplifyframework.storage.StorageCategory;
import com.amplifyframework.util.Immutable;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * This is the top-level customer-facing interface to the Amplify
 * framework.
 *
 * The Amplify System has the following responsibilities:
 *
 * 1) Add, Get and Remove Category plugins with the Amplify System
 * 2) Configure and reset the Amplify System with the information
 * from the amplifyconfiguration.json.
 *
 * Configure using amplifyconfiguration.json
 * <pre>
 *     {@code
 *      Amplify.configure(getApplicationContext());
 *     }
 * </pre>
 */
public final class Amplify {
    // These static references provide an entry point to the different categories.
    // For example, you can call storage operations through Amplify.Storage.list(String path).
    @SuppressWarnings("checkstyle:all") public static final AnalyticsCategory Analytics = new AnalyticsCategory();
    @SuppressWarnings("checkstyle:all") public static final ApiCategory API = new ApiCategory();
    @SuppressWarnings("checkstyle:all") public static final LoggingCategory Logging = new LoggingCategory();
    @SuppressWarnings("checkstyle:all") public static final StorageCategory Storage = new StorageCategory();
    @SuppressWarnings("checkstyle:all") public static final HubCategory Hub = new HubCategory();
    @SuppressWarnings("checkstyle:all") public static final DataStoreCategory DataStore = new DataStoreCategory();
    @SuppressWarnings("checkstyle:all") public static final PredictionsCategory Predictions = new PredictionsCategory();

    private static final Map<CategoryType, Category<? extends Plugin<?>>> CATEGORIES = buildCategoriesMap();

    // Used as a synchronization locking object. Set to true once configure() is complete.
    private static final AtomicBoolean CONFIGURATION_LOCK = new AtomicBoolean(false);

    // An executor on which categories may be initialized.
    private static final ExecutorService INITIALIZATION_POOL = Executors.newFixedThreadPool(CATEGORIES.size());

    /**
     * Dis-allows instantiation of this utility class.
     */
    private Amplify() {
        throw new UnsupportedOperationException("No instances allowed.");
    }

    private static Map<CategoryType, Category<? extends Plugin<?>>> buildCategoriesMap() {
        final Map<CategoryType, Category<? extends Plugin<?>>> modifiableCategories = new LinkedHashMap<>();
        modifiableCategories.put(CategoryType.ANALYTICS, Analytics);
        modifiableCategories.put(CategoryType.API, API);
        modifiableCategories.put(CategoryType.LOGGING, Logging);
        modifiableCategories.put(CategoryType.STORAGE, Storage);
        modifiableCategories.put(CategoryType.HUB, Hub);
        modifiableCategories.put(CategoryType.DATASTORE, DataStore);
        modifiableCategories.put(CategoryType.PREDICTIONS, Predictions);
        return Immutable.of(modifiableCategories);
    }

    /**
     * Read the configuration from amplifyconfiguration.json file.
     * @param context Android context required to read the contents of file
     * @throws AmplifyException thrown when already configured or there is no plugin found for a configuration
     */
    public static void configure(@NonNull Context context) throws AmplifyException {
        configure(AmplifyConfiguration.fromConfigFile(context), context);
    }

    /**
     * Configure Amplify with AmplifyConfiguration object.
     * @param configuration AmplifyConfiguration object for configuration via code
     * @param context An Android Context
     * @throws AmplifyException thrown when already configured or there is no configuration found for a plugin
     */
    public static void configure(@NonNull final AmplifyConfiguration configuration, @NonNull Context context)
            throws AmplifyException {
        Objects.requireNonNull(configuration);
        Objects.requireNonNull(context);

        synchronized (CONFIGURATION_LOCK) {
            if (CONFIGURATION_LOCK.get()) {
                throw new AmplifyException(
                    "The client issued a subsequent call to `Amplify.configure` after the first had already succeeded.",
                        "Be sure to only call Amplify.configure once"
                );
            }

            for (Category<? extends Plugin<?>> category : CATEGORIES.values()) {
                if (category.getPlugins().size() > 0) {
                    CategoryConfiguration categoryConfiguration =
                        configuration.forCategoryType(category.getCategoryType());
                    category.configure(categoryConfiguration, context);
                    beginInitialization(category, context);
                }
            }

            CONFIGURATION_LOCK.set(true);
        }
    }

    private static void beginInitialization(@NonNull Category<? extends Plugin<?>> category, @NonNull Context context) {
        INITIALIZATION_POOL.execute(() -> category.initialize(context));
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
        updatePluginRegistry(plugin, RegistryUpdateType.ADD);
    }

    /**
     * Removes a plugin form the Amplify framework.
     * @param plugin The plugin to remove from the Amplify framework
     * @param <P> The type of the plugin being removed
     * @throws AmplifyException On failure to remove a plugin
     */
    public static <P extends Plugin<?>> void removePlugin(@NonNull final P plugin) throws AmplifyException {
        updatePluginRegistry(plugin, RegistryUpdateType.REMOVE);
    }

    @SuppressWarnings("unchecked") // Wants Category<P> from CATEGORIES.get(...), but it has Category<?>
    private static <P extends Plugin<?>> void updatePluginRegistry(
            final P plugin, final RegistryUpdateType registryUpdateType) throws AmplifyException {

        synchronized (CONFIGURATION_LOCK) {
            if (TextUtils.isEmpty(plugin.getPluginKey())) {
                throw new AmplifyException(
                        "Plugin key was missing for + " + plugin.getClass().getSimpleName(),
                        "This should never happen - contact the plugin developers to find out why this is."
                );
            } else if (!CATEGORIES.containsKey(plugin.getCategoryType())) {
                throw new AmplifyException("Plugin category does not exist. ",
                    "Verify that the library version is correct and supports the plugin's category.");
            }

            Category<P> category;
            try {
                category = (Category<P>) CATEGORIES.get(plugin.getCategoryType());
            } catch (ClassCastException classCastException) {
                // will throw in a moment...
                category = null;
            }
            if (category == null) {
                throw new AmplifyException("A plugin is being added to the wrong category",
                        AmplifyException.TODO_RECOVERY_SUGGESTION);
            }

            if (RegistryUpdateType.REMOVE.equals(registryUpdateType)) {
                category.removePlugin(plugin);
            } else {
                category.addPlugin(plugin);
            }
        }
    }

    private enum RegistryUpdateType {
        ADD,
        REMOVE
    }

}
