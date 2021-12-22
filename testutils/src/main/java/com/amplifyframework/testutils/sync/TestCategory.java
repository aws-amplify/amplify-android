/*
 * Copyright 2021 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package com.amplifyframework.testutils.sync;

import android.content.Context;
import androidx.test.core.app.ApplicationProvider;

import com.amplifyframework.AmplifyException;
import com.amplifyframework.analytics.AnalyticsCategory;
import com.amplifyframework.api.ApiCategory;
import com.amplifyframework.auth.AuthCategory;
import com.amplifyframework.core.AmplifyConfiguration;
import com.amplifyframework.core.category.Category;
import com.amplifyframework.core.category.CategoryConfiguration;
import com.amplifyframework.core.category.CategoryType;
import com.amplifyframework.core.plugin.Plugin;
import com.amplifyframework.datastore.DataStoreCategory;
import com.amplifyframework.geo.GeoCategory;
import com.amplifyframework.hub.HubCategory;
import com.amplifyframework.logging.LoggingCategory;
import com.amplifyframework.predictions.PredictionsCategory;
import com.amplifyframework.storage.StorageCategory;

/**
 * Test utility to generate a fully configured category from a given plugin.
 * Use this to generate categories to delegate Amplify operations to.
 */
@SuppressWarnings("unchecked")
public final class TestCategory {
    private TestCategory() {}

    /**
     * Creates a test category to be used for testing. A category containing given
     * plugin will be used to register and configure the plugin. The category
     * instance will have the same Amplify category-type as registered plugin,
     * and it should be cast appropriately before usage.
     *
     * <pre>
     *     AuthCategory auth = (AuthCategory) TestCategory.forPlugin(plugin);
     * </pre>
     *
     * Resulting category object should not be used in production.
     *
     * @param plugin Plugin to register in the category. The resulting category
     *               will share the category type with this plugin.
     * @param <P> Type of plugin being registered.
     * @return An Amplify category object with the given plugin registered and
     *         configured. The category will be of same type as registered plugin
     *         and it should be cast to correct type before usage.
     * @throws AmplifyException if category fails to configure given plugin.
     */
    public static <P extends Plugin<?>> Category<?> forPlugin(P plugin) throws AmplifyException {
        CategoryType categoryType = plugin.getCategoryType();
        Category<Plugin<?>> category = (Category<Plugin<?>>) fromCategoryType(categoryType);
        category.addPlugin(plugin);
        CategoryConfiguration config = AmplifyConfiguration.fromConfigFile(context())
                .forCategoryType(categoryType);
        category.configure(config, context());
        category.initialize(context());
        return category;
    }

    private static Category<? extends Plugin<?>> fromCategoryType(CategoryType type) {
        switch (type) {
            case ANALYTICS: return new AnalyticsCategory();
            case API: return new ApiCategory();
            case AUTH: return new AuthCategory();
            case DATASTORE: return new DataStoreCategory();
            case GEO: return new GeoCategory();
            case HUB: return new HubCategory();
            case LOGGING: return new LoggingCategory();
            case PREDICTIONS: return new PredictionsCategory();
            case STORAGE: return new StorageCategory();
            default: throw new RuntimeException("No such category type.");
        }
    }

    private static Context context() {
        return ApplicationProvider.getApplicationContext();
    }
}
