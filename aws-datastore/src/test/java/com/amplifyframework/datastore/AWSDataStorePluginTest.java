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

package com.amplifyframework.datastore;

import android.content.Context;

import com.amplifyframework.core.model.ModelProvider;
import com.amplifyframework.testmodels.personcar.Person;
import com.amplifyframework.testutils.random.RandomString;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import static androidx.test.core.app.ApplicationProvider.getApplicationContext;

@RunWith(RobolectricTestRunner.class)
public final class AWSDataStorePluginTest {

    private AWSDataStorePlugin awsDataStorePlugin;

    @Before
    public void setup() {
        ModelProvider modelProvider = SimpleModelProvider.builder()
            .version(RandomString.string())
            .addModel(Person.class)
            .build();
        this.awsDataStorePlugin = AWSDataStorePlugin.forModels(modelProvider);
    }

    /**
     * Configuring and initializing the plugin succeeds without freezing or
     * crashing the calling thread. Basic. 🙄
     * @throws JSONException Not expected; on failure to arrange configuration object
     * @throws DataStoreException Not expected; on failure to configure of initialize plugin
     */
    @Test
    public void configureAndInitializeWithSyncMode() throws DataStoreException, JSONException {
        Context context = getApplicationContext();
        JSONObject json = new JSONObject()
            .put("awsDataStorePlugin", new JSONObject()
                .put("syncMode", "api"));
        awsDataStorePlugin.configure(json, context);
        awsDataStorePlugin.initialize(context);
    }
}
