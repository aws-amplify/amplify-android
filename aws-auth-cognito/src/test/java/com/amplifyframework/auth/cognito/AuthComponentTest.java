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
import com.amplifyframework.auth.AuthCodeDeliveryDetails;
import com.amplifyframework.auth.AuthException;
import com.amplifyframework.auth.AuthUser;
import com.amplifyframework.auth.AuthUserAttribute;
import com.amplifyframework.auth.AuthUserAttributeKey;
import com.amplifyframework.auth.cognito.options.AWSCognitoAuthSignInOptions;
import com.amplifyframework.auth.cognito.options.AWSCognitoAuthSignUpOptions;
import com.amplifyframework.auth.result.AuthSignInResult;
import com.amplifyframework.auth.result.AuthSignUpResult;
import com.amplifyframework.auth.result.step.AuthNextSignInStep;
import com.amplifyframework.auth.result.step.AuthNextSignUpStep;
import com.amplifyframework.auth.result.step.AuthResetPasswordStep;
import com.amplifyframework.auth.result.step.AuthSignInStep;
import com.amplifyframework.auth.result.step.AuthSignUpStep;

import com.amazonaws.mobile.client.AWSMobileClient;
import com.amazonaws.mobile.client.Callback;
import com.amazonaws.mobile.client.UserState;
import com.amazonaws.mobile.client.UserStateDetails;
import com.amazonaws.mobile.client.results.ForgotPasswordResult;
import com.amazonaws.mobile.client.results.ForgotPasswordState;
import com.amazonaws.mobile.client.results.SignInResult;
import com.amazonaws.mobile.client.results.SignInState;
import com.amazonaws.mobile.client.results.SignUpResult;
import com.amazonaws.mobile.client.results.UserCodeDeliveryDetails;
import com.amazonaws.mobile.config.AWSConfiguration;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.util.Map;

import edu.emory.mathcs.backport.java.util.Collections;

import static androidx.test.core.app.ApplicationProvider.getApplicationContext;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * Test that the current implementation of Auth as a wrapper of AWSMobileClient calls the correct
 * AWSMobileClient methods with the correct parameters when the different Auth methods are called.
 */
@RunWith(RobolectricTestRunner.class)
@SuppressWarnings("unchecked")
public final class AuthComponentTest {
    private AWSMobileClient mobileClient;
    private AWSCognitoAuthPlugin authPlugin;
    private final String userId = "myId";
    private final String destination = "e***@email.com";
    private final String deliveryMedium = "sms";
    private final String attributeName = "email";
    private final String username = "username";
    private final String password = "password123";
    private final String newPassword = "newPassword123";
    private final String attributeKey = AuthUserAttributeKey.email().getKeyString();
    private final String attributeVal = "email@email.com";
    private final String confirmationCode = "confirm";
    private final Map<String, String> metadata = Collections.singletonMap("aCustomKey", "aCustomVal");

    @Before
    public void setup() throws AmplifyException {
        mobileClient = mock(AWSMobileClient.class);
        authPlugin = new AWSCognitoAuthPlugin(mobileClient, userId);
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
        JSONObject testConfig = new JSONObject().put("TestKey", "TestVal");

        doAnswer(invocation -> {
            assertEquals(context, invocation.getArgument(0));
            assertEquals(testConfig.toString(), invocation.getArgument(1).toString());

            Callback<UserStateDetails> callback = invocation.getArgument(2);
            callback.onResult(userStateDetails);
            return null;
        }).when(mobileClient).initialize(any(Context.class), any(AWSConfiguration.class), any(Callback.class));

        authPlugin.configure(testConfig, context);
        verify(mobileClient, times(1))
                .initialize(any(Context.class), any(AWSConfiguration.class), any(Callback.class));
    }

    /**
     * Tests that if AWSMobileClient returns an error callback during initialization, the Auth configure method
     * throws an AuthException.
     * @throws AuthException the AuthException expected to be thrown if initialization fails.
     */
    @Test(expected = AuthException.class)
    public void testConfigureExceptionHandling() throws AuthException {
        doAnswer(invocation -> {
            Callback<UserStateDetails> callback = invocation.getArgument(2);
            callback.onError(new Exception());
            return null;
        }).when(mobileClient).initialize(any(Context.class), any(AWSConfiguration.class), any(Callback.class));

        authPlugin.configure(new JSONObject(), getApplicationContext());
    }

    /**
     * Tests that the signUp method of the Auth wrapper of AWSMobileClient (AMC) calls AMC.signUp() with the username
     * and password it received and, if included, the userAttributes and validationData sent in the options object.
     * Also ensures that in the onResult case, the success callback receives a valid AuthSignUpResult.
     */
    @Test
    public void signUp() {
        SignUpResult amcResult = new SignUpResult(
            false,
            new UserCodeDeliveryDetails(
                    destination,
                    deliveryMedium,
                    attributeName
            ),
            null
        );

        doAnswer(invocation -> {
            assertEquals(username, invocation.getArgument(0));
            assertEquals(password, invocation.getArgument(1));
            Map<String, String> attributeMap = invocation.getArgument(2);
            assertTrue(attributeMap.containsKey(attributeKey));
            assertEquals(attributeVal, attributeMap.get(attributeKey));
            assertEquals(metadata, invocation.getArgument(3));

            Callback<SignUpResult> callback = invocation.getArgument(4);
            callback.onResult(amcResult);
            return null;
        }).when(mobileClient)
            .signUp(any(String.class), any(String.class), any(Map.class), any(Map.class), any(Callback.class));

        authPlugin.signUp(
            username,
            password,
            AWSCognitoAuthSignUpOptions.builder()
            .userAttributes(
                Collections.singletonList(new AuthUserAttribute(AuthUserAttributeKey.email(), attributeVal))
            )
            .validationData(metadata)
            .build(),
            result -> validateSignUpResult(result, AuthSignUpStep.CONFIRM_SIGN_UP_STEP),
            error -> fail());

        verify(mobileClient, times(1))
                .signUp(any(String.class), any(String.class), any(Map.class), any(Map.class), any(Callback.class));
    }

    /**
     * Tests that the confirmSignUp method of the Auth wrapper of AWSMobileClient (AMC) calls AMC.confirmSignUp with
     * the username and confirmation code it received.
     * Also ensures that in the onResult case, the success callback receives a valid AuthSignUpResult.
     */
    @Test
    public void confirmSignUp() {
        SignUpResult amcResult = new SignUpResult(
                true,
                new UserCodeDeliveryDetails(
                        destination,
                        deliveryMedium,
                        attributeName
                ),
                null
        );

        doAnswer(invocation -> {
            assertEquals(username, invocation.getArgument(0));
            assertEquals(confirmationCode, invocation.getArgument(1));

            Callback<SignUpResult> callback = invocation.getArgument(2);
            callback.onResult(amcResult);
            return null;
        }).when(mobileClient)
            .confirmSignUp(any(String.class), any(String.class), any(Callback.class));

        authPlugin.confirmSignUp(
            username,
            confirmationCode,
            result -> validateSignUpResult(result, AuthSignUpStep.DONE),
            error -> fail());

        verify(mobileClient, times(1))
                .confirmSignUp(any(String.class), any(String.class), any(Callback.class));
    }

    /**
     * Tests that the resendSignUpCode method of the Auth wrapper of AWSMobileClient (AMC) calls AMC.resendSignUp with
     * the username it received .
     * Also ensures that in the onResult case, the success callback receives a valid AuthSignUpResult.
     */
    @Test
    public void resendSignUpCode() {
        SignUpResult amcResult = new SignUpResult(
                false,
                new UserCodeDeliveryDetails(
                        destination,
                        deliveryMedium,
                        attributeName
                ),
                null
        );

        doAnswer(invocation -> {
            assertEquals(username, invocation.getArgument(0));

            Callback<SignUpResult> callback = invocation.getArgument(1);
            callback.onResult(amcResult);
            return null;
        }).when(mobileClient)
            .resendSignUp(any(String.class), any(Callback.class));

        authPlugin.resendSignUpCode(
            username,
            result -> validateSignUpResult(result, AuthSignUpStep.CONFIRM_SIGN_UP_STEP),
            error -> fail());

        verify(mobileClient, times(1))
                .resendSignUp(any(String.class), any(Callback.class));
    }

    /**
     * Tests that the signIn method of the Auth wrapper of AWSMobileClient (AMC) calls AMC.signIn with
     * the username, password it received, and, if included, the metadata sent in the options object.
     * Also ensures that in the onResult case, the success callback receives a valid AuthSignInResult.
     */
    @Test
    public void signIn() {
        SignInResult amcResult = new SignInResult(
            SignInState.SMS_MFA,
            new UserCodeDeliveryDetails(
                    destination,
                    deliveryMedium,
                    attributeName
            )
        );

        doAnswer(invocation -> {
            assertEquals(username, invocation.getArgument(0));
            assertEquals(password, invocation.getArgument(1));
            assertEquals(metadata, invocation.getArgument(2));

            Callback<SignInResult> callback = invocation.getArgument(3);
            callback.onResult(amcResult);
            return null;
        }).when(mobileClient)
            .signIn(any(String.class), any(String.class), any(Map.class), any(Callback.class));

        authPlugin.signIn(
            username,
            password,
            AWSCognitoAuthSignInOptions.builder().metadata(metadata).build(),
            result -> validateSignInResult(
                result,
                false,
                AuthSignInStep.CONFIRM_SIGN_IN_WITH_SMS_MFA_CODE
            ),
            error -> fail());

        verify(mobileClient, times(1))
                .signIn(any(String.class), any(String.class), any(Map.class), any(Callback.class));
    }

    /**
     * Tests that the confirmSignIn method of the Auth wrapper of AWSMobileClient (AMC) calls AMC.confirmSignIn with
     * the confirmation code it received and the success callback receives a valid AuthSignInResult.
     */
    @Test
    public void confirmSignIn() {
        SignInResult amcResult = new SignInResult(
                SignInState.DONE,
                new UserCodeDeliveryDetails(
                        destination,
                        deliveryMedium,
                        attributeName
                )
        );

        doAnswer(invocation -> {
            assertEquals(confirmationCode, invocation.getArgument(0));

            Callback<SignInResult> callback = invocation.getArgument(1);
            callback.onResult(amcResult);
            return null;
        }).when(mobileClient)
            .confirmSignIn(any(String.class), any(Callback.class));

        authPlugin.confirmSignIn(
            confirmationCode,
            result -> validateSignInResult(
                result,
                true,
                AuthSignInStep.DONE
            ),
            error -> fail()
        );

        verify(mobileClient, times(1))
                .confirmSignIn(any(String.class), any(Callback.class));
    }

    /**
     * Tests that the resetPassword method of the Auth wrapper of AWSMobileClient (AMC) calls AMC.forgotPassword with
     * the username it received.
     * Also ensures that in the onResult case, the success callback receives a valid AuthResetPasswordResult and in
     * the onError case, the error call back receives an AuthException with the root cause attached.
     */
    @Test
    public void resetPassword() {
        ForgotPasswordResult amcResult = new ForgotPasswordResult(ForgotPasswordState.CONFIRMATION_CODE);
        amcResult.setParameters(new UserCodeDeliveryDetails(
                destination,
                deliveryMedium,
                attributeName
        ));

        doAnswer(invocation -> {
            assertEquals(username, invocation.getArgument(0));

            Callback<ForgotPasswordResult> callback = invocation.getArgument(1);
            callback.onResult(amcResult);
            return null;
        }).when(mobileClient)
            .forgotPassword(any(String.class), any(Callback.class));

        authPlugin.resetPassword(
            username,
            result -> {
                assertFalse(result.isPasswordReset());
                assertEquals(
                    AuthResetPasswordStep.CONFIRM_RESET_PASSWORD_WITH_CODE,
                    result.getNextStep().getResetPasswordStep()
                );
                validateCodeDeliveryDetails(result.getNextStep().getCodeDeliveryDetails());
            },
            error -> fail()
        );

        verify(mobileClient, times(1))
                .forgotPassword(any(String.class), any(Callback.class));
    }

    /**
     * Tests that the confirmResetPassword method of the Auth wrapper of AWSMobileClient (AMC) calls
     * AMC.confirmForgotPassword with the new password and confirmation code it received.
     * Also ensures that in the onResult case, the success callback is triggered and in the onError case,
     * the error call back receives an AuthException with the root cause attached.
     */
    @Test
    public void confirmResetPassword() {
        ForgotPasswordResult amcResult = new ForgotPasswordResult(ForgotPasswordState.DONE);

        doAnswer(invocation -> {
            assertEquals(newPassword, invocation.getArgument(0));
            assertEquals(confirmationCode, invocation.getArgument(1));

            Callback<ForgotPasswordResult> callback = invocation.getArgument(2);
            callback.onResult(amcResult);
            return null;
        }).when(mobileClient)
            .confirmForgotPassword(any(String.class), any(String.class), any(Callback.class));

        authPlugin.confirmResetPassword(
            newPassword,
            confirmationCode,
            () -> { },
            error -> fail()
        );

        verify(mobileClient, times(1))
                .confirmForgotPassword(any(String.class), any(String.class), any(Callback.class));
    }

    /**
     * Tests that the getCurrentUser method of the Auth wrapper of AWSMobileClient (AMC) returns a new
     * AWSCognitoAuthUser object containing the userId property in the plugin and the username from AMC.getUsername().
     */
    @Test
    public void getCurrentUser() {
        doAnswer(invocation -> username).when(mobileClient).getUsername();
        AuthUser user = authPlugin.getCurrentUser();

        assertEquals(userId, user.getUserId());
        assertEquals(username, user.getUsername());
    }

    /**
     * Tests that the getEscapeHatch method of the Auth wrapper of AWSMobileClient (AMC) returns the instance of
     * AWSMobileClient held by the plugin.
     */
    @Test
    public void getEscapeHatch() {
        AWSMobileClient client = authPlugin.getEscapeHatch();
        assertEquals(mobileClient, client);
    }

    /**
     * Validate the sign up result is what was expected.
     * @param result The received result
     * @param targetStep The correct value for the next step (the only part of the response that varies in these tests)
     */
    private void validateSignUpResult(AuthSignUpResult result, AuthSignUpStep targetStep) {
        AuthNextSignUpStep nextStep = result.getNextStep();
        validateCodeDeliveryDetails(nextStep.getCodeDeliveryDetails());
        assertTrue(result.isSignUpComplete());
        assertEquals(targetStep, nextStep.getSignUpStep());

    }

    private void validateSignInResult(AuthSignInResult result, boolean targetIsSignedIn, AuthSignInStep targetStep) {
        AuthNextSignInStep nextStep = result.getNextStep();
        validateCodeDeliveryDetails(nextStep.getCodeDeliveryDetails());
        assertEquals(targetIsSignedIn, result.isSignInComplete());
        assertEquals(targetStep, nextStep.getSignInStep());
    }

    private void validateCodeDeliveryDetails(AuthCodeDeliveryDetails codeDetails) {
        assertEquals(destination, codeDetails.getDestination());
        assertEquals(AuthCodeDeliveryDetails.DeliveryMedium.fromString(deliveryMedium),
                codeDetails.getDeliveryMedium());
        assertEquals(attributeName, codeDetails.getAttributeName());
    }
}
