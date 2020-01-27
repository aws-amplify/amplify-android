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

import com.amplifyframework.AmplifyException;
import com.amplifyframework.core.BadInitLoggingPlugin;
import com.amplifyframework.core.plugin.Plugin;
import com.amplifyframework.testutils.Await;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.util.Set;

import static androidx.test.core.app.ApplicationProvider.getApplicationContext;
import static org.junit.Assert.assertEquals;

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
        assertEquals(1, result.getSuccessPlugins().size());
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

        assertEquals(0, categoryInitializationResult.getSuccessPlugins().size());
        Set<String> failedPlugins = categoryInitializationResult.getFailedPlugins();
        assertEquals(1, failedPlugins.size());
        assertEquals("BadInitLoggingPlugin", failedPlugins.iterator().next());
    }
}
