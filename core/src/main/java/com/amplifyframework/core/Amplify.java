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
import com.amplifyframework.storage.StorageCategory;
import com.amplifyframework.util.Immutable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * This is the top-level entry-point to the Amplify framework. Amplify is a declarative,
 * high-level framework used to fulfill common mobile application use cases.
 *
 * Capabilities are organized into "Categories," and categories may be fulfilled by
 * using a particular plugin, that implements the category's API. AWS provides plug-ins
 * to fulfill the category APIs using AWS backend resources, but other plugins may
 * be developed, or may already be available.
 *
 * A user interacts with Amplify by adding plugins, and configuring the framework for use:
 * <pre>
 *     Amplify.addPlugin(new AWSS3StoragePlugin());
 *     Amplify.addPlugin(new AWSAPIPlugin());
 *     Amplify.configure(
 *         getApplicationContext(),
 *         () -> Log.i(TAG, "Ready to use."),
 *         failure -> Log.e(TAG, "Failed to configure Amplify framework.", failure)
 *     );
 * </pre>
 *
 * When you're done using the Amplify framework, you can release its resources, to
 * free memory:
 * <pre>
 *     Amplify.release(
 *         getApplicationContext(),
 *         () -> Log.i(TAG, "Releases resources used by Amplify."),
 *         failure -> Log.e(TAG, "Failed to release resources used by Amplify.")
 *     );
 * </pre>
 */
public final class Amplify {
    // These static references provide an entry point to the different categories.
    // For example, you can call storage operations like Amplify.Storage.list(String path).
    @SuppressWarnings("checkstyle:all") public static final AnalyticsCategory Analytics = new AnalyticsCategory();
    @SuppressWarnings("checkstyle:all") public static final ApiCategory API = new ApiCategory();
    @SuppressWarnings("checkstyle:all") public static final LoggingCategory Logging = new LoggingCategory();
    @SuppressWarnings("checkstyle:all") public static final StorageCategory Storage = new StorageCategory();
    @SuppressWarnings("checkstyle:all") public static final HubCategory Hub = new HubCategory();
    @SuppressWarnings("checkstyle:all") public static final DataStoreCategory DataStore = new DataStoreCategory();

    private static final Map<CategoryType, Category<? extends Plugin<?>>> CATEGORIES = buildCategoriesMap();

    private static final ExecutorService THREAD_POOL = Executors.newCachedThreadPool();

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
        return Immutable.of(modifiableCategories);
    }

    /**
     * Configures Amplify using the configuration found in your project's
     * `src/main/res/raw/amplifyconfiguration.json`.
     * @param context An Android Context
     * @param onConfigured Called if configuration succeeds, and Amplify is ready for use
     * @param onFailure Invoked with an {@link AmplifyException} if the framework is already
     *                  configured, or if `amplifyconfiguration.json` is absent, malformed,
     *                  or contains configuration for which no plugin has been added.
     */
    public static synchronized void configure(
            @NonNull Context context,
            @NonNull Action onConfigured,
            @NonNull Consumer<AmplifyException> onFailure) {
        try {
            Objects.requireNonNull(context);
            Objects.requireNonNull(onConfigured);
            Objects.requireNonNull(onFailure);
        } catch (NullPointerException nullArgumentException) {
            onFailure.accept(new AmplifyException(
                "A null value was provided to configure(Context, Action, Consumer).",
                nullArgumentException,
                "Ensure that all parameters are non-null."
            ));
            return;
        }

        final AmplifyConfiguration config = new AmplifyConfiguration();
        try {
            config.populateFromConfigFile(context);
        } catch (AmplifyException configurationFailure) {
            onFailure.accept(configurationFailure);
            return;
        }

        configure(config, context, onConfigured, onFailure);
    }

    /**
     * Configures Amplify from an {@link AmplifyConfiguration} object.
     * @param configuration An Amplify configuration
     * @param context An Android Context
     * @param onConfigured Called if configuration succeeds, and Amplify is ready for use
     * @param onFailure Invoked with an {@link AmplifyException} if the framework is already
     *                  configured, or if `amplifyconfiguration.json` is absent, malformed,
     *                  or contains configuration for which no plugin has been added.
     */
    public static synchronized void configure(
            @NonNull AmplifyConfiguration configuration,
            @NonNull Context context,
            @NonNull Action onConfigured,
            @NonNull Consumer<AmplifyException> onFailure) {
        try {
            Objects.requireNonNull(configuration);
            Objects.requireNonNull(context);
            Objects.requireNonNull(onConfigured);
            Objects.requireNonNull(onFailure);
        } catch (NullPointerException nullArgumentException) {
            onFailure.accept(new AmplifyException(
                "Passed a null argument to configure(AmplifyConfiguration, Context, Action, Consumer).",
                nullArgumentException,
                "Verify that all arguments are non-null."
            ));
            return;
        }

        final CountDownLatch pendingInitializations = new CountDownLatch(CATEGORIES.size());
        final List<AmplifyException> failures = new ArrayList<>();

        for (Category<? extends Plugin<?>> category : CATEGORIES.values()) {
            if (category.getPlugins().size() <= 0) {
                pendingInitializations.countDown();
                continue;
            }

            THREAD_POOL.execute(() -> {
                try {
                    CategoryConfiguration categoryConfiguration =
                        configuration.forCategoryType(category.getCategoryType());
                    category.configure(categoryConfiguration, context);
                } catch (AmplifyException configurationError) {
                    failures.add(configurationError);
                } finally {
                    pendingInitializations.countDown();
                }
            });
        }

        THREAD_POOL.execute(() -> {
            try {
                pendingInitializations.await();
            } catch (InterruptedException interruptedException) {
                failures.add(new AmplifyException(
                    "Interrupted while waiting for category configuration.",
                    interruptedException,
                    "One of your plugins may be hanging during configuration."
                ));
            }

            if (failures.isEmpty()) {
                onConfigured.call();
            } else {
                onFailure.accept(failures.get(0));
            }
        });
    }

    /**
     * Releases all resources used by the Amplify framework.
     * @param context An Android Context
     * @param onReleased Called when all resources have been resources
     * @param onFailure Called upon failure to release Amplify resources
     */
    public static synchronized void release(
            @NonNull Context context,
            @NonNull Action onReleased,
            @NonNull Consumer<AmplifyException> onFailure) {
        try {
            Objects.requireNonNull(context);
            Objects.requireNonNull(onReleased);
            Objects.requireNonNull(onFailure);
        } catch (NullPointerException nullArgumentException) {
            onFailure.accept(new AmplifyException(
                "Passed a null argument to release(Context, Action, Consumer).",
                nullArgumentException,
                "Verify that all arguments passed are non-null."
            ));
            return;
        }

        final CountDownLatch pendingReleases = new CountDownLatch(CATEGORIES.size());
        final List<AmplifyException> failures = new ArrayList<>();

        for (Category<? extends Plugin<?>> category : CATEGORIES.values()) {
            if (category.getPlugins().size() <= 0) {
                pendingReleases.countDown();
                continue;
            }

            THREAD_POOL.execute(() -> {
                try {
                    category.release(context);
                } catch (AmplifyException configurationError) {
                    failures.add(configurationError);
                } finally {
                    pendingReleases.countDown();
                }
            });
        }

        THREAD_POOL.execute(() -> {
            try {
                pendingReleases.await();
            } catch (InterruptedException interruptedException) {
                failures.add(new AmplifyException(
                    "Interrupted while waiting for category release.",
                    interruptedException,
                    "One of your plugins may be hanging during release."
                ));
            }

            if (failures.isEmpty()) {
                onReleased.call();
            } else {
                onFailure.accept(failures.get(0));
            }
        });
    }

    /**
     * Register a plugin with Amplify.
     * @param plugin an implementation of a CATEGORY_TYPE that
     *               conforms to the {@link Plugin} interface.
     * @param <P> any plugin that conforms to the {@link Plugin} interface
     * @throws AmplifyException when a plugin cannot be registered for the category type it belongs to
     *                         or when when the plugin's category type is not supported by Amplify.
     */
    public static synchronized <P extends Plugin<?>> void addPlugin(@NonNull final P plugin)
            throws AmplifyException {
        updatePluginRegistry(plugin, RegistryUpdateType.ADD);
    }

    /**
     * Removes a plugin form the Amplify framework.
     * @param plugin The plugin to remove from the Amplify framework
     * @param <P> The type of the plugin being removed
     * @throws AmplifyException On failure to remove a plugin
     */
    @SuppressWarnings("WeakerAccess")
    public static synchronized <P extends Plugin<?>> void removePlugin(@NonNull final P plugin)
            throws AmplifyException {
        updatePluginRegistry(plugin, RegistryUpdateType.REMOVE);
    }

    @SuppressWarnings("unchecked") // Wants Category<P> from CATEGORIES.get(...), but it has Category<?>
    private static <P extends Plugin<?>> void updatePluginRegistry(
            final P plugin, final RegistryUpdateType registryUpdateType) throws AmplifyException {

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

    private enum RegistryUpdateType {
        ADD,
        REMOVE
    }
}
