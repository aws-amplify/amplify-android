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

package com.amplifyframework.predictions.aws;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.test.core.app.ApplicationProvider;

import com.amplifyframework.AmplifyException;
import com.amplifyframework.core.Amplify;
import com.amplifyframework.core.AmplifyConfiguration;
import com.amplifyframework.testutils.Resources;
import com.amplifyframework.testutils.sync.SynchronousMobileClient;

import com.amazonaws.mobile.config.AWSConfiguration;

/**
 * This is a class to help configure Amplify and prepare other test resources
 * for Predictions integration tests.
 */
final class TestConfiguration {

    private static final String AMPLIFY_CONFIGURATION_IDENTIFIER = "amplifyconfiguration";
    private static final String AWS_CONFIGURATION_IDENTIFIER = "awsconfiguration";

    private static boolean isConfigured;

    private TestConfiguration() {}

    /**
     * Process-wide configuration for the Predictions instrumentation tests.
     * @throws Exception if configuration fails
     */
    @NonNull
    static synchronized void configureIfNotConfigured() throws Exception {
        if (isConfigured) {
            return;
        }

        Context context = ApplicationProvider.getApplicationContext();
        Amplify.addPlugin(new AWSPredictionsPlugin());
        configureAmplify(context);
        setUpCredentials(context);

        isConfigured = true;
    }

    private static void configureAmplify(Context context) throws AmplifyException {
        // Obtain Amplify Configuration
        final int configId = Resources.getRawResourceId(context, AMPLIFY_CONFIGURATION_IDENTIFIER);
        AmplifyConfiguration configuration = AmplifyConfiguration.fromConfigFile(context, configId);

        // Configure!
        Amplify.configure(configuration, context);
    }

    private static void setUpCredentials(Context context) throws SynchronousMobileClient.MobileClientException {
        // Obtain AWS Configuration
        final int configId = Resources.getRawResourceId(context, AWS_CONFIGURATION_IDENTIFIER);
        AWSConfiguration configuration = new AWSConfiguration(context, configId);

        // Initialize Mobile Client!
        SynchronousMobileClient.instance().initialize(context, configuration);
    }
}
