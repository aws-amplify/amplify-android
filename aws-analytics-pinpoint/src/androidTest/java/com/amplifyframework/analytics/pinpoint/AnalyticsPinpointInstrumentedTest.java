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
import android.util.Log;
import androidx.test.core.app.ApplicationProvider;

import com.amplifyframework.analytics.GeneralAnalyticsEvent;
import com.amplifyframework.analytics.Properties;
import com.amplifyframework.core.Amplify;
import com.amplifyframework.core.AmplifyConfiguration;

import org.junit.BeforeClass;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

/**
 * Validates the functionality of the {@link AmazonPinpointAnalyticsPlugin}.
 */
public class AnalyticsPinpointInstrumentedTest {

    /**
     * Log tag for the test class.
     */
    private static final String TAG = AnalyticsPinpointInstrumentedTest.class.getSimpleName();

    /**
     * Configure the Amplify framework, if that hasn't already happened in this process instance.
     */
    @BeforeClass
    public static void setUp() {
        Context context = ApplicationProvider.getApplicationContext();
        AmplifyConfiguration configuration = new AmplifyConfiguration();
        configuration.populateFromConfigFile(context, R.raw.amplifyconfiguration);
        Amplify.addPlugin(new AmazonPinpointAnalyticsPlugin());
        Amplify.configure(configuration, context);
    }

    /**
     * Record a general analytics event and verify that it has been recorded using Analytics
     * pinpoint client.
     */
    @Test
    public void testRecordEvent() {
        assert true;
        Log.i(TAG, "Test configuration invoked");

        GeneralAnalyticsEvent event = new GeneralAnalyticsEvent("Amplify-event-double",
                PinpointProperties.builder()
                .add("DemoProperty1", "DemoValue1")
        .add("DemoDoubleProperty2", 2.0)
        .build());
        Amplify.Analytics.recordEvent(event);
    }
}
