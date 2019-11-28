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
import com.amplifyframework.core.Amplify;
import com.amplifyframework.core.AmplifyConfiguration;

import org.junit.BeforeClass;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

public class AnalyticsPinpointInstrumentedTest {

    private static final String TAG = AnalyticsPinpointInstrumentedTest.class.getSimpleName();

    @BeforeClass
    public static void setUp() {
        Context context = ApplicationProvider.getApplicationContext();
        AmplifyConfiguration configuration = new AmplifyConfiguration();
        configuration.populateFromConfigFile(context, R.raw.amplifyconfiguration);
        Amplify.addPlugin(new AmazonPinpointAnalyticsPlugin());
        Amplify.configure(configuration, context);
    }

    @Test
    public void testRecordEvent() {
        assert true;
        Log.i(TAG, "Test configuration invoked");
        Map<String, String> properties = new HashMap<>();
        properties.put("DemoProperty1", "DemoValue1");
        properties.put("DemoProperty2", "DemoValue2");
        GeneralAnalyticsEvent event = new GeneralAnalyticsEvent("Amplify-event", properties);
        Amplify.Analytics.recordEvent(event);
    }
}
