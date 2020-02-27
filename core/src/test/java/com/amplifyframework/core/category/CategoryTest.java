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

package com.amplifyframework.core.category;

import android.content.Context;

import com.amplifyframework.AmplifyException;
import com.amplifyframework.core.BadInitLoggingPlugin;
import com.amplifyframework.core.plugin.Plugin;
import com.amplifyframework.testutils.Await;
import com.amplifyframework.testutils.RandomString;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.skyscreamer.jsonassert.JSONAssert;

import java.util.Collections;
import java.util.Set;

import static androidx.test.core.app.ApplicationProvider.getApplicationContext;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 * Tests the plugin management and initialization facilities of the {@link Category}.
 */
@RunWith(RobolectricTestRunner.class)
public final class CategoryTest {

    /**
     * Validate the behavior of a successful category initialization.
     * @throws AmplifyException not expected; possible from addPlugin(), configure(), etc.
     */
    @Test
    public void successfulCategoryInitialization() throws AmplifyException {
        Category<Plugin<Void>> category = SimpleCategory.type(CategoryType.DATASTORE);
        category.addPlugin(SimplePlugin.type(CategoryType.DATASTORE));
        category.configure(SimpleCategoryConfiguration.type(CategoryType.DATASTORE), getApplicationContext());

        //noinspection CodeBlock2Expr Easier to read as block
        CategoryInitializationResult result = Await.result((onResult, ignored) -> {
            category.initialize(getApplicationContext(), onResult);
        });
        assertEquals(1, result.getSuccessfulPlugins().size());
        assertEquals(0, result.getFailedPlugins().size());
    }

    /**
     * Validate the behavior of a category failing to initialize.
     * @throws AmplifyException On addPlugin() of configure(), not expected
     */
    @Test
    public void failedCategoryInitialization() throws AmplifyException {
        Category<Plugin<Void>> category = SimpleCategory.type(CategoryType.LOGGING);
        category.addPlugin(BadInitLoggingPlugin.instance());
        category.configure(SimpleCategoryConfiguration.type(CategoryType.LOGGING), getApplicationContext());

        //noinspection CodeBlock2Expr Easier to read as a block
        CategoryInitializationResult categoryInitializationResult = Await.result((onResult, ignored) -> {
            category.initialize(getApplicationContext(), onResult);
        });

        assertEquals(0, categoryInitializationResult.getSuccessfulPlugins().size());
        Set<String> failedPlugins = categoryInitializationResult.getFailedPlugins();
        assertEquals(1, failedPlugins.size());
        assertEquals("BadInitLoggingPlugin", failedPlugins.iterator().next());
    }

    /**
     * When a single plugin is added, it gets configured via the call to
     * {@link Category#configure(CategoryConfiguration, Context)}, and can then be accessed
     * via the category.
     * @throws AmplifyException Possible from category APIs, not expected in this test
     * @throws JSONException Not expected; on failure to arrange test data
     */
    @Test
    public void singlePluginCanBeConfiguredAndSelected() throws AmplifyException, JSONException {
        // Create a plugin and add it to category
        String pluginKey = RandomString.string();
        CategoryType categoryType = CategoryType.DATASTORE;
        FakeCategory category = new FakeCategory(categoryType);
        FakePlugin<Object> plugin = FakePlugin.builder()
            .pluginKey(pluginKey)
            .categoryType(categoryType)
            .escapeHatch(new Object())
            .build();
        category.addPlugin(plugin);

        // Now, configure the category
        JSONObject categoryConfigurationJson = new JSONObject()
            .put("plugins", new JSONObject()
                .put(pluginKey, new JSONObject()
                    .put("pluginKey", "pluginValue")));
        FakeCategoryConfiguration config =
            FakeCategoryConfiguration.instance(categoryType, categoryConfigurationJson);
        category.configure(config, getApplicationContext());

        // Validate the category
        assertEquals(categoryType, category.getCategoryType());
        assertEquals(plugin, category.getPlugin(pluginKey));
        assertEquals(plugin, category.getSelectedPlugin());
        assertEquals(Collections.singleton(plugin), category.getPlugins());

        // Validate that plugin received config
        JSONObject expectedPluginJson =
            categoryConfigurationJson.getJSONObject("plugins").getJSONObject(pluginKey);
        JSONAssert.assertEquals(expectedPluginJson, plugin.getPluginConfiguration(), true);
    }

    /**
     * Once configured, a category cannot be re-configured.
     * @throws AmplifyException Expected; on second configuration attempt
     */
    @Test(expected = AmplifyException.class)
    public void throwsOnSecondConfigure() throws AmplifyException {
        FakeCategory category = new FakeCategory(CategoryType.API);
        FakeCategoryConfiguration config = FakeCategoryConfiguration.instance(CategoryType.API);
        try {
            category.configure(config, getApplicationContext());
        } catch (AmplifyException failureOnFirstConfiguration) {
            fail("First configuration did not succeed. Only the second is expected to fail.");
        }
        category.configure(config, getApplicationContext());
    }

    /**
     * It is an error to try and add a plugin to a category after the category
     * is already configured. Otherwise, that plugin would miss the configuration phase.
     * @throws AmplifyException Expected; should occur on attempt to add plugin
     */
    @Test(expected = AmplifyException.class)
    public void cantAddPluginAfterConfigure() throws AmplifyException {
        CategoryType categoryType = CategoryType.HUB;
        FakeCategory category = new FakeCategory(categoryType);
        FakeCategoryConfiguration config = FakeCategoryConfiguration.instance(categoryType);
        try {
            category.configure(config, getApplicationContext());
        } catch (AmplifyException failureOnConfigure) {
            fail("Configuration is meant to succeed; AmplifyException expected from addPlugin(), below.");
        }

        // This throws
        category.addPlugin(FakePlugin.builder()
            .pluginKey("Whatever")
            .escapeHatch(null)
            .categoryType(categoryType)
            .build());
    }
}
