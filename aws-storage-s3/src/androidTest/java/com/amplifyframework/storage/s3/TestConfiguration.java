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

package com.amplifyframework.storage.s3;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.test.core.app.ApplicationProvider;

import com.amplifyframework.AmplifyException;
import com.amplifyframework.core.Amplify;
import com.amplifyframework.core.AmplifyConfiguration;
import com.amplifyframework.core.InitializationStatus;
import com.amplifyframework.core.category.CategoryType;
import com.amplifyframework.hub.HubChannel;
import com.amplifyframework.testutils.Await;

import com.amazonaws.mobile.client.AWSMobileClient;
import com.amazonaws.mobile.client.Callback;
import com.amazonaws.mobile.client.UserStateDetails;
import org.json.JSONException;

final class TestConfiguration {
    private static TestConfiguration singleton;
    private final AWSS3StoragePlugin plugin;
    private final String bucket;

    private TestConfiguration(Context context) throws AmplifyException {
        plugin = new AWSS3StoragePlugin();
        bucket = getBucketNameFromPlugin(context, plugin);

        Amplify.addPlugin(plugin);
        configureAmplify(context);
        setUpCredentials(context);
    }

    /**
     * Process-wide configuration for the Storage instrumentation tests.
     * @return A TestConfiguration instance
     */
    @NonNull
    static synchronized TestConfiguration configureIfNotConfigured() throws AmplifyException {
        if (singleton == null) {
            singleton = new TestConfiguration(ApplicationProvider.getApplicationContext());
        }
        return singleton;
    }

    private static void configureAmplify(Context context) {
        // Configure Amplify, and wait for Storage to be ready.
        Await.result((onResult, onError) -> {
            // Listen for initialization success messages
            Amplify.Hub.subscribe(HubChannel.STORAGE, event -> {
                // When we get one, end the Await call by firing a result
                if (InitializationStatus.SUCCEEDED.toString().equals(event.getName())) {
                    onResult.accept(CategoryType.STORAGE);
                    // When we see a failure, end the await by firing onError
                } else if (InitializationStatus.FAILED.toString().equals(event.getName())) {
                    onError.accept(new RuntimeException(String.valueOf(event.getData())));
                }
            });
            // Now that we're listening for it ... configure Amplify and begin initialization
            try {
                Amplify.configure(context);
            } catch (AmplifyException configurationFailure) {
                // If the configuration fails before even initialization begins, kill the Await with onError.
                onError.accept(new RuntimeException("Configuration failed.",
                        configurationFailure));
            }
        });
    }

    private static void setUpCredentials(Context context) {
        Await.result((onResult, onError) ->
            AWSMobileClient.getInstance().initialize(context, new Callback<UserStateDetails>() {
                @Override
                public void onResult(UserStateDetails userStateDetails) {
                    onResult.accept(userStateDetails);
                }

                @Override
                public void onError(Exception initializationError) {
                    onError.accept(new RuntimeException("Mobile Client initialization failed.",
                            initializationError));
                }
            })
        );
    }

    private static String getBucketNameFromPlugin(Context context, AWSS3StoragePlugin plugin) {
        try {
            return AmplifyConfiguration.fromConfigFile(context)
                    .forCategoryType(plugin.getCategoryType())
                    .getPluginConfig(plugin.getPluginKey())
                    .getString("bucket");
        } catch (AmplifyException exception) {
            throw new RuntimeException("Failed to obtain bucket name from configuration.",
                    exception);
        } catch (JSONException jsonError) {
            throw new RuntimeException("Configuration is missing bucket name.", jsonError);
        }
    }

    private static int getConfigResourceId(Context context, String identifier) {
        return context.getResources().getIdentifier(identifier,
                "raw", context.getPackageName());
    }

    @NonNull
    AWSS3StoragePlugin plugin() {
        return plugin;
    }

    @NonNull
    String getBucketName() {
        return bucket;
    }
}
