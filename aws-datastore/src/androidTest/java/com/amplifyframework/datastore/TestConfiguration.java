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

package com.amplifyframework.datastore;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.test.core.app.ApplicationProvider;

import com.amplifyframework.AmplifyException;
import com.amplifyframework.api.aws.AWSApiPlugin;
import com.amplifyframework.core.Amplify;
import com.amplifyframework.core.AmplifyConfiguration;
import com.amplifyframework.core.category.CategoryType;
import com.amplifyframework.datastore.test.R.raw;
import com.amplifyframework.testmodels.commentsblog.AmplifyModelProvider;

final class TestConfiguration {
    private static TestConfiguration singleton;
    private final AWSDataStorePlugin plugin;
    private final String apiName;

    private TestConfiguration(Context context) throws AmplifyException {
        plugin = AWSDataStorePlugin.singleton(AmplifyModelProvider.getInstance());

        // We need to use an API plugin, so that we can validate remote sync.
        Amplify.addPlugin(new AWSApiPlugin());
        Amplify.addPlugin(plugin);

        AmplifyConfiguration amplifyConfiguration = new AmplifyConfiguration();
        amplifyConfiguration.populateFromConfigFile(context, raw.amplifyconfiguration);
        Amplify.configure(amplifyConfiguration, context);

        // Get the first configured API.
        apiName = amplifyConfiguration.forCategoryType(CategoryType.API)
            .getPluginConfig("awsAPIPlugin")
            .keys()
            .next();
    }

    /**
     * Process-wide configuration for the DataStore instrumentation tests.
     * @return A TestConfiguration instance
     */
    @NonNull
    static synchronized TestConfiguration configureIfNotConfigured() throws AmplifyException {
        if (singleton == null) {
            singleton = new TestConfiguration(ApplicationProvider.getApplicationContext());
        }
        return singleton;
    }

    AWSDataStorePlugin plugin() {
        return plugin;
    }

    String apiName() {
        return apiName;
    }
}
