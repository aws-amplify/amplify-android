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

package com.amplifyframework.analytics.pinpoint;

import android.content.Context;

import com.amplifyframework.analytics.AnalyticsException;
import com.amplifyframework.core.Amplify;
import com.amplifyframework.logging.Logger;
import com.amplifyframework.util.UserAgent;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.mobile.client.AWSMobileClient;
import com.amazonaws.mobile.client.Callback;
import com.amazonaws.mobile.client.UserStateDetails;
import com.amazonaws.mobile.config.AWSConfiguration;
import com.amazonaws.mobileconnectors.pinpoint.PinpointConfiguration;
import com.amazonaws.mobileconnectors.pinpoint.PinpointManager;
import com.amazonaws.mobileconnectors.pinpoint.analytics.AnalyticsClient;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.pinpoint.model.ChannelType;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Factory class to vend out pinpoint analytics client.
 */
final class PinpointClientFactory {

    private static final Logger LOG = Amplify.Logging.forNamespace("amplify:aws-analytics");
    private static final int INITIALIZATION_TIMEOUT_MS = 5000;

    private PinpointClientFactory() {
    }

    static AnalyticsClient create(Context context,
                                  AmazonPinpointAnalyticsPluginConfiguration pinpointAnalyticsPluginConfiguration)
            throws AnalyticsException {
        final PinpointManager pinpointManager;
        final AWSConfiguration awsConfiguration = new AWSConfiguration(context);

        CountDownLatch mobileClientLatch = new CountDownLatch(1);
        // Initialize the AWSMobileClient
        AWSMobileClient.getInstance().initialize(context, awsConfiguration,
                new Callback<UserStateDetails>() {
                    @Override
                    public void onResult(UserStateDetails userStateDetails) {
                        LOG.info("Mobile client initialized");
                        mobileClientLatch.countDown();
                    }

                    @Override
                    public void onError(Exception exception) {
                        LOG.error("Error initializing AWS Mobile Client", exception);
                    }
                });

        try {
            if (!mobileClientLatch.await(INITIALIZATION_TIMEOUT_MS, TimeUnit.MILLISECONDS)) {
                throw new AnalyticsException("Failed to initialize mobile client.",
                        "Please check your awsconfiguration json.");
            }
        } catch (InterruptedException exception) {
            throw new RuntimeException("Failed to initialize mobile client: " + exception.getLocalizedMessage());
        }

        ClientConfiguration clientConfiguration = new ClientConfiguration();
        clientConfiguration.setUserAgent(UserAgent.string());

        // Construct configuration using information from the configure method
        PinpointConfiguration pinpointConfiguration = new PinpointConfiguration(
                context,
                pinpointAnalyticsPluginConfiguration.getAppId(),
                Regions.fromName(pinpointAnalyticsPluginConfiguration.getRegion()),
                ChannelType.GCM,
                AWSMobileClient.getInstance()
        ).withClientConfiguration(clientConfiguration);

        pinpointManager = new PinpointManager(pinpointConfiguration);
        return pinpointManager.getAnalyticsClient();
    }
}
