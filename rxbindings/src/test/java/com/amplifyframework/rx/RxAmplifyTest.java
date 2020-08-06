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

import com.amplifyframework.AmplifyException;
import com.amplifyframework.core.AmplifyConfiguration;
import com.amplifyframework.core.category.CategoryConfiguration;
import com.amplifyframework.core.category.CategoryType;
import com.amplifyframework.core.plugin.Plugin;
import com.amplifyframework.datastore.DataStoreCategoryConfiguration;
import com.amplifyframework.testutils.random.RandomString;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.robolectric.RobolectricTestRunner;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests the {@link RxAmplify} facade.
 */
@RunWith(RobolectricTestRunner.class)
public final class RxAmplifyTest {
    /**
     * Calling {@link RxAmplify#addPlugin(Plugin)} and {@link RxAmplify#configure(AmplifyConfiguration, Context)}
     * will pass config JSON down into the plugin via its {@link Plugin#configure(JSONObject, Context)}
     * method.
     * @throws AmplifyException Not exected; possible from RxAmplilfy's addPlugin(), configure().
     * @throws JSONException Not expected; on failure to arrange test JSON inputs.
     */
    @SuppressWarnings("unchecked")
    @Test
    public void canAddPluginsAndConfigure() throws AmplifyException, JSONException {
        // Setup a mock plugin, add it to Amplify.
        CategoryType categoryType = CategoryType.STORAGE;
        String pluginKey = RandomString.string();
        Plugin<Void> one = mock(Plugin.class);
        when(one.getPluginKey()).thenReturn(pluginKey);
        when(one.getCategoryType()).thenReturn(categoryType);
        RxAmplify.addPlugin(one);

        // Configure Amplify, with a config to match the plugin above.
        Map<String, CategoryConfiguration> categoryConfigs = new HashMap<>();
        String categoryName = categoryType.getConfigurationKey();
        CategoryConfiguration categoryConfig = new DataStoreCategoryConfiguration();
        JSONObject pluginJson = new JSONObject()
            .put("someKey", "someVal");
        categoryConfig.populateFromJSON(new JSONObject()
            .put("plugins", new JSONObject()
                .put(pluginKey, pluginJson)));
        categoryConfigs.put(categoryName, categoryConfig);
        AmplifyConfiguration config = AmplifyConfiguration.loadCategoryConfigs(categoryConfigs).build();
        RxAmplify.configure(config, mock(Context.class));

        // Validate that the plugin gets configured with the provided JSON
        ArgumentCaptor<JSONObject> configJsonCapture = ArgumentCaptor.forClass(JSONObject.class);
        verify(one).configure(configJsonCapture.capture(), any(Context.class));
        assertEquals(pluginJson, configJsonCapture.getValue());
    }
}
