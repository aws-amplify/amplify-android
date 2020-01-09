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

import com.amplifyframework.testutils.Await;

import com.amazonaws.mobile.client.AWSMobileClient;
import com.amazonaws.mobile.client.Callback;
import com.amazonaws.mobile.client.UserStateDetails;

import static androidx.test.core.app.ApplicationProvider.getApplicationContext;

final class TestAWSMobileClient {
    @SuppressWarnings("checkstyle:all") private TestAWSMobileClient() {}

    @SuppressWarnings("UnusedReturnValue")
    static UserStateDetails initialize() throws InitializationError {
        try {
            return Await.<UserStateDetails, Exception>result((onResult, onError) ->
                AWSMobileClient.getInstance().initialize(getApplicationContext(), new Callback<UserStateDetails>() {
                    @Override
                    public void onResult(UserStateDetails userStateDetails) {
                        onResult.accept(userStateDetails);
                    }

                    @Override
                    public void onError(Exception initializationError) {
                        onError.accept(initializationError);
                    }
                })
            );
        } catch (Exception initializationError) {
            throw new InitializationError(initializationError);
        }
    }

    static final class InitializationError extends Exception {
        private static final long serialVersionUID = 1L;

        InitializationError(Throwable cause) {
            super("Failed to initialize TestAWSMobileClient.", cause);
        }
    }
}
