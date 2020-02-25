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
import com.amplifyframework.testutils.Resources;
import com.amplifyframework.testutils.SynchronousAWSMobileClient;

import com.amazonaws.mobile.config.AWSConfiguration;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

/**
 * This is a class to help configure Amplify and prepare other test resources
 * for Storage integration tests. In order to use this package effectively,
 * please verify the following:
 *
 * 1) `res/raw/amplifyconfiguration.json` is present and one instance of
 *    "awsS3StoragePlugin" is registered with valid S3 bucket name and region.
 *
 * 2) `res/raw/awsconfiguration.json` is present with a valid Cognito Identity and
 *    User Pools.
 *
 * 3) `res/raw/credentials.json` is present with two or more user credentials from
 *    the Cognito User Pools.
 *
 * If for any reason you require the test configuration files to be named differently,
 * the constant identifier variables in this class can be adjusted accordingly.
 */
final class TestConfiguration {

    private static final String AMPLIFY_CONFIGURATION_IDENTIFIER = "amplifyconfiguration";
    private static final String AWS_CONFIGURATION_IDENTIFIER = "awsconfiguration";
    private static final String CREDENTIAL_IDENTIFIER = "credentials";

    private static TestConfiguration singleton;
    private final AWSS3StoragePlugin plugin;
    private final String bucket;
    private final Map<String, String> userCredentials;

    private TestConfiguration(Context context)
            throws AmplifyException, SynchronousAWSMobileClient.MobileClientException {
        plugin = new AWSS3StoragePlugin();
        bucket = getBucketNameFromPlugin(context, plugin);
        userCredentials = getUserCredentials(context);

        Amplify.addPlugin(plugin);
        configureAmplify(context);
        setUpCredentials(context);
    }

    /**
     * Process-wide configuration for the Storage instrumentation tests.
     * @return A TestConfiguration instance
     */
    @NonNull
    static synchronized TestConfiguration configureIfNotConfigured()
            throws AmplifyException, SynchronousAWSMobileClient.MobileClientException {
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
                // Obtain Amplify Configuration
                final int configId = Resources.getRawResourceId(context, AMPLIFY_CONFIGURATION_IDENTIFIER);
                AmplifyConfiguration configuration = AmplifyConfiguration.fromConfigFile(context, configId);

                // Configure!
                Amplify.configure(configuration, context);
            } catch (AmplifyException configurationFailure) {
                // If the configuration fails before even initialization begins, kill the Await with onError.
                onError.accept(new RuntimeException("Configuration failed.",
                        configurationFailure));
            }
        });
    }

    private static void setUpCredentials(Context context) throws SynchronousAWSMobileClient.MobileClientException {
        // Obtain AWS Configuration
        final int configId = Resources.getRawResourceId(context, AWS_CONFIGURATION_IDENTIFIER);
        AWSConfiguration configuration = new AWSConfiguration(context, configId);
        // Initialize Mobile Client!
        SynchronousAWSMobileClient.instance().initialize(configuration);
    }

    private String getBucketNameFromPlugin(Context context, AWSS3StoragePlugin plugin) {
        try {
            final int configId = Resources.getRawResourceId(context, AMPLIFY_CONFIGURATION_IDENTIFIER);
            return AmplifyConfiguration.fromConfigFile(context, configId)
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

    private Map<String, String> getUserCredentials(Context context) {
        Map<String, String> userCredentials = new HashMap<>();

        // Obtain User Pool Credentials configuration JSON
        final int configId = Resources.getRawResourceId(context, CREDENTIAL_IDENTIFIER);
        JSONObject configuration = Resources.readAsJson(context, configId);

        // Read the content for credentials
        try {
            JSONArray credentials = configuration.getJSONArray("credentials");
            for (int index = 0; index < credentials.length(); index++) {
                String username = credentials.getJSONObject(index).getString("username");
                String password = credentials.getJSONObject(index).getString("password");
                userCredentials.put(username, password);
            }
        } catch (JSONException exception) {
            throw new RuntimeException(exception);
        }
        return userCredentials;
    }

    @NonNull
    AWSS3StoragePlugin plugin() {
        return plugin;
    }

    @NonNull
    String getBucketName() {
        return bucket;
    }

    @NonNull
    Map<String, String> getUserCredentials() {
        return userCredentials;
    }
}
