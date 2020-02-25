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
import com.amplifyframework.core.InitializationStatus;
import com.amplifyframework.core.category.CategoryType;
import com.amplifyframework.datastore.test.R;
import com.amplifyframework.hub.HubChannel;
import com.amplifyframework.testmodels.commentsblog.AmplifyModelProvider;
import com.amplifyframework.testutils.Await;

final class TestConfiguration {
    private static TestConfiguration singleton;
    private final AWSDataStorePlugin plugin;

    private TestConfiguration(Context context) throws AmplifyException {
        plugin = AWSDataStorePlugin.forModels(AmplifyModelProvider.getInstance());

        // We need to use an API plugin, so that we can validate remote sync.
        Amplify.addPlugin(new AWSApiPlugin());
        Amplify.addPlugin(plugin);

        AmplifyConfiguration amplifyConfiguration =
            AmplifyConfiguration.fromConfigFile(context, R.raw.amplifyconfiguration);

        /*
         * Currently, initialization of categories is async, and there is no synchronization.
         * So, in order for our tests to work correctly, we have to wait for DataStore initialization
         * before calling any of the DataStore category behaviors.
         * See a discussion in https://github.com/aws-amplify/amplify-android/issues/215.
         */

        // Configure Amplify, and wait for DataStore to be ready.
        Await.result((onResult, onError) -> {
            // Listen for initialization success messages
            Amplify.Hub.subscribe(HubChannel.DATASTORE, event -> {
                // When we get one, end the Await call by firing a result
                if (InitializationStatus.SUCCEEDED.toString().equals(event.getName())) {
                    onResult.accept(CategoryType.DATASTORE);
                // When we see a failure, end the await by firing onError
                } else if (InitializationStatus.FAILED.toString().equals(event.getName())) {
                    onError.accept(new RuntimeException(String.valueOf(event.getData())));
                }
            });
            // Now that we're listening for it ... configure Amplify and begin initialization
            try {
                Amplify.configure(amplifyConfiguration, context);
            } catch (AmplifyException configurationFailure) {
                // If the configuration fails before even initialization begins, kill the Await with onError.
                onError.accept(new RuntimeException("Configuration failed.", configurationFailure));
            }
        });
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
}
