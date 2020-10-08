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

package com.amplifyframework.auth.cognito;

import android.content.Context;

import com.amplifyframework.AmplifyException;
import com.amplifyframework.auth.AuthCategory;
import com.amplifyframework.auth.AuthCategoryConfiguration;
import com.amplifyframework.auth.AuthException;
import com.amplifyframework.util.UserAgent;

import com.amazonaws.mobile.client.AWSMobileClient;
import com.amazonaws.mobile.client.Callback;
import com.amazonaws.mobile.client.UserState;
import com.amazonaws.mobile.client.UserStateDetails;
import com.amazonaws.mobile.config.AWSConfiguration;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.robolectric.RobolectricTestRunner;

import static androidx.test.core.app.ApplicationProvider.getApplicationContext;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * Verifies that AWSCognitoAuthPlugin configure correctly configures an AWSMobileClient, or fails if the
 * AWSMobileClient fails to initialize.
 *
 * The @Before method creates an AuthCategory, but does not configure or initialize it.  That is done in the tests
 * themselves.   Conversely, the @Before method in {@link AuthComponentTest} sets up a "configured" AuthCategory
 * for testing the AuthCategoryBehavior methods.  This difference in the @Before method implementation is why configure
 * is tested in this separate test class.
 */
@RunWith(RobolectricTestRunner.class)
public class AuthComponentConfigureTest {
    private static final String PLUGIN_KEY = "awsCognitoAuthPlugin";
    // User sub value here should match the one encoded in the access token above
    private static final String USER_SUB = "69bc252b-dd07-41c0-b1db-a46066b8ef51";
    private AWSMobileClient mobileClient;
    private AuthCategory authCategory;

    /**
     * Get all setup for the future tests with mocks and a standard response for tokens.
     * @throws AmplifyException If add plugin fails
     */
    @Before
    public void setup() throws AmplifyException {
        mobileClient = mock(AWSMobileClient.class);
        authCategory = new AuthCategory();
        authCategory.addPlugin(new AWSCognitoAuthPlugin(mobileClient, USER_SUB));
    }

    /**
     * Tests that the configure method of the Auth wrapper of AWSMobileClient (AMC) calls AMC.initialize() with the
     * passed in context, a new AWSConfiguration object containing the passed in JSONObject, and a callback object
     * which causes configure to complete successfully in the onResult case.
     * @throws AmplifyException an exception wrapping the exception returned in onError of AMC.initialize()
     * @throws JSONException has to be declared as part of creating a test JSON object
     */
    @Test
    public void testConfigure() throws AmplifyException, JSONException {
        UserStateDetails userStateDetails = new UserStateDetails(UserState.SIGNED_OUT, null);
        Context context = getApplicationContext();
        JSONObject pluginConfig = new JSONObject().put("TestKey", "TestVal");
        pluginConfig.put("UserAgentOverride", UserAgent.string());
        JSONObject json = new JSONObject().put("plugins",
                new JSONObject().put(
                        PLUGIN_KEY,
                        pluginConfig
                )
        );
        AuthCategoryConfiguration authConfig = new AuthCategoryConfiguration();
        authConfig.populateFromJSON(json);

        doAnswer(invocation -> {
            Callback<UserStateDetails> callback = invocation.getArgument(2);
            callback.onResult(userStateDetails);
            return null;
        }).when(mobileClient).initialize(any(), any(), any());

        authCategory.configure(authConfig, context);

        ArgumentCaptor<AWSConfiguration> awsConfigCaptor = ArgumentCaptor.forClass(AWSConfiguration.class);
        verify(mobileClient).initialize(eq(context), awsConfigCaptor.capture(), any());
        String returnedConfig = awsConfigCaptor.getValue().toString();
        String inputConfig = pluginConfig.toString();
        // Strip the opening and closing braces from the test input and ensure that the key/value pair is included
        // in the returned aws config.
        assertTrue(returnedConfig.contains(inputConfig.substring(1, inputConfig.length() - 1)));
    }

    /**
     * If {@link AWSMobileClient} emits an error during initialization, the
     * {@link com.amplifyframework.auth.AuthPlugin#configure(JSONObject, Context)} method should wrap that exception
     * in an {@link AuthException} and throw it on its calling thread.
     * @throws AmplifyException the exception expected to be thrown when configuration fails.
     * @throws JSONException has to be declared as part of creating a test JSON object
     */
    @Test(expected = AuthException.class)
    public void testConfigureExceptionHandling() throws AmplifyException, JSONException {
        JSONObject pluginConfig = new JSONObject().put("TestKey", "TestVal");
        JSONObject json = new JSONObject().put("plugins",
                new JSONObject().put(
                        PLUGIN_KEY,
                        pluginConfig
                )
        );
        AuthCategoryConfiguration authConfig = new AuthCategoryConfiguration();
        authConfig.populateFromJSON(json);

        doAnswer(invocation -> {
            Callback<UserStateDetails> callback = invocation.getArgument(2);
            callback.onError(new Exception());
            return null;
        }).when(mobileClient).initialize(any(), any(), any());

        authCategory.configure(authConfig, getApplicationContext());
    }
}
