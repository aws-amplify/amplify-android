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

import com.amplifyframework.api.rest.RestOptions;
import com.amplifyframework.api.rest.RestResponse;
import com.amplifyframework.core.Amplify;

import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertTrue;

/**
 * Validates the functionality of the {@link AWSApiPlugin} for REST operations.
 *
 */
public final class RestApiInstrumentationTest {

    /**
     * Configure the Amplify framework, if that hasn't already happened in this process instance.
     */
    @BeforeClass
    public static void onceBeforeTests() {
        AmplifyTestConfigurator.configureIfNotConfigured();
    }

    /**
     * Test whether we can make api Rest call in none auth.
     */
    @Test
    public void getRequestWithNoAuth() {
        final RestOptions options = new RestOptions("nonAuthApi",
                "simplesuccess"
                );
        LatchedRestResponseListener responseListener = new LatchedRestResponseListener();
        Amplify.API.get(options, responseListener);
        RestResponse getResponse =
                responseListener.awaitTerminalEvent().assertNoError().assertResponse().getResponse();
        assertTrue(getResponse.getData() != null);
    }
}
