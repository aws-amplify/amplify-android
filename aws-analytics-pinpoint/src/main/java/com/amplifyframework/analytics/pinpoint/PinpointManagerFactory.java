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

import com.amplifyframework.util.UserAgent;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.mobileconnectors.pinpoint.PinpointConfiguration;
import com.amazonaws.mobileconnectors.pinpoint.PinpointManager;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.pinpoint.model.ChannelType;

/**
 * Factory class to vend out pinpoint analytics client.
 */
final class PinpointManagerFactory {
    private PinpointManagerFactory() {}

    static PinpointManager create(
            Context context,
            AWSPinpointAnalyticsPluginConfiguration pinpointAnalyticsPluginConfiguration,
            AWSCredentialsProvider credentialsProvider) {
        ClientConfiguration clientConfiguration = new ClientConfiguration();
        clientConfiguration.setUserAgent(UserAgent.string());

        // Construct configuration using information from the configure method
        PinpointConfiguration pinpointConfiguration = new PinpointConfiguration(
                context,
                pinpointAnalyticsPluginConfiguration.getAppId(),
                Regions.fromName(pinpointAnalyticsPluginConfiguration.getRegion()),
                ChannelType.GCM,
                credentialsProvider
        ).withClientConfiguration(clientConfiguration);

        return new PinpointManager(pinpointConfiguration);
    }
}
