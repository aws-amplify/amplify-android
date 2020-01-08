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

import com.amazonaws.mobile.client.AWSMobileClient;
import com.amazonaws.mobile.client.Callback;
import com.amazonaws.mobile.client.UserStateDetails;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

import static androidx.test.core.app.ApplicationProvider.getApplicationContext;

final class TestAWSMobileClient {
    @SuppressWarnings("checkstyle:all") private TestAWSMobileClient() {}

    @SuppressWarnings("UnusedReturnValue")
    static UserStateDetails initialize() {
        final CountDownLatch latch = new CountDownLatch(1);
        final AtomicReference<UserStateDetails> userStateDetailsContainer = new AtomicReference<>();
        final AtomicReference<Exception> errorContainer = new AtomicReference<>(null);

        AWSMobileClient.getInstance().initialize(getApplicationContext(), new Callback<UserStateDetails>() {
            @Override
            public void onResult(UserStateDetails userStateDetails) {
                userStateDetailsContainer.set(userStateDetails);
                latch.countDown();
            }

            @Override
            @SuppressWarnings("ParameterName")
            public void onError(Exception initializationError) {
                errorContainer.set(initializationError);
                latch.countDown();
            }
        });
        try {
            latch.await();
        } catch (InterruptedException interruptedException) {
            errorContainer.set(interruptedException);
        }

        if (null != errorContainer.get()) {
            throw new RuntimeException("Error while initializing mobile client", errorContainer.get());
        } else if (latch.getCount() != 0) {
            throw new RuntimeException("Failed to initialize mobile client.");
        }

        return userStateDetailsContainer.get();
    }
}
