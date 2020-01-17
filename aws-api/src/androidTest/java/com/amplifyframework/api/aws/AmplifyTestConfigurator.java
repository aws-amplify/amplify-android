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

package com.amplifyframework.api.aws;

import android.content.Context;
import androidx.test.core.app.ApplicationProvider;

import com.amplifyframework.AmplifyException;
import com.amplifyframework.api.aws.test.R;
import com.amplifyframework.core.Amplify;
import com.amplifyframework.core.AmplifyConfiguration;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * A little test utility to wrap Amplify configuration, so that we only attempt it
 * once per process. (The Amplify configure() method will throw an exception if you
 * try to re-configure it.)
 */
final class AmplifyTestConfigurator {
    private static final long SETUP_TIME_SECS = 5L;

    private static boolean alreadyConfigured = false;

    @SuppressWarnings("checkstyle:all") private AmplifyTestConfigurator() {}

    /**
     * Gets the singleton instance of Amplify, configured for the API
     * plugin tests.
     */
    @SuppressWarnings("checkstyle:WhitespaceAround") // {} in lambda
    static synchronized void configureIfNotConfigured() throws AmplifyException {
        if (alreadyConfigured) {
            return;
        }

        final CountDownLatch latch = new CountDownLatch(1);

        Context context = ApplicationProvider.getApplicationContext();
        AmplifyConfiguration configuration = new AmplifyConfiguration();
        configuration.populateFromConfigFile(context, R.raw.amplifyconfiguration);
        Amplify.addPlugin(new AWSApiPlugin());
        Amplify.configure(configuration, context, latch::countDown, failure -> {});

        try {
            latch.await(SETUP_TIME_SECS, TimeUnit.SECONDS);
        } catch (InterruptedException interruptedException) {
            throw new RuntimeException(interruptedException);
        }

        alreadyConfigured = true;
    }
}
