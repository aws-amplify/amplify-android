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
import com.amplifyframework.auth.AuthCodeDeliveryDetails;
import com.amplifyframework.auth.AuthException;
import com.amplifyframework.auth.AuthUser;
import com.amplifyframework.auth.AuthUserAttribute;
import com.amplifyframework.auth.AuthUserAttributeKey;
import com.amplifyframework.auth.cognito.options.AWSCognitoAuthSignInOptions;
import com.amplifyframework.auth.cognito.options.AWSCognitoAuthSignUpOptions;
import com.amplifyframework.auth.result.AuthResetPasswordResult;
import com.amplifyframework.auth.result.AuthSignInResult;
import com.amplifyframework.auth.result.AuthSignUpResult;
import com.amplifyframework.auth.result.step.AuthNextSignInStep;
import com.amplifyframework.auth.result.step.AuthNextSignUpStep;
import com.amplifyframework.auth.result.step.AuthResetPasswordStep;
import com.amplifyframework.auth.result.step.AuthSignInStep;
import com.amplifyframework.auth.result.step.AuthSignUpStep;
import com.amplifyframework.testutils.sync.SynchronousAuth;

import com.amazonaws.mobile.client.AWSMobileClient;
import com.amazonaws.mobile.client.Callback;
import com.amazonaws.mobile.client.UserState;
import com.amazonaws.mobile.client.UserStateDetails;
import com.amazonaws.mobile.client.results.ForgotPasswordResult;
import com.amazonaws.mobile.client.results.ForgotPasswordState;
import com.amazonaws.mobile.client.results.SignInResult;
import com.amazonaws.mobile.client.results.SignInState;
import com.amazonaws.mobile.client.results.SignUpResult;
import com.amazonaws.mobile.client.results.Tokens;
import com.amazonaws.mobile.client.results.UserCodeDeliveryDetails;
import com.amazonaws.mobile.config.AWSConfiguration;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.util.Collections;
import java.util.Map;

import static androidx.test.core.app.ApplicationProvider.getApplicationContext;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * Test that the current implementation of Auth as a wrapper of AWSMobileClient calls the correct
 * AWSMobileClient methods with the correct parameters when the different Auth methods are called.
 */
@RunWith(RobolectricTestRunner.class)
public final class AuthComponentTest {
    private static final String USER_ID = "myId";
    private static final String DESTINATION = "e***@email.com";
    private static final String DELIVERY_MEDIUM = "sms";
    private static final String ATTRIBUTE_NAME = "email";
    private static final String USERNAME = "username";
    private static final String PASSWORD = "password123";
    private static final String NEW_PASSWORD = "newPassword123";
    private static final String ATTRIBUTE_KEY = AuthUserAttributeKey.email().getKeyString();
    private static final String ATTRIBUTE_VAL = "email@email.com";
    private static final String CONFIRMATION_CODE = "confirm";
    private static final Map<String, String> METADATA = Collections.singletonMap("aCustomKey", "aCustomVal");
    private AWSMobileClient mobileClient;
    private AuthCategory authCategory;
    private SynchronousAuth synchronousAuth;

    @Before
    public void setup() throws AmplifyException {
        mobileClient = mock(AWSMobileClient.class);
        authCategory = new AuthCategory();
        authCategory.addPlugin(new AWSCognitoAuthPlugin(mobileClient, USER_ID));
        synchronousAuth = SynchronousAuth.delegatingTo(authCategory);

        doAnswer(invocation -> {
            Callback<Tokens> callback = invocation.getArgument(0);
            callback.onError(new Exception());
            return null;
        }).when(mobileClient).getTokens(any());
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
        JSONObject json = new JSONObject().put("plugins",
                new JSONObject().put(
                    new AWSCognitoAuthPlugin().getPluginKey(),
                    pluginConfig
                )
        );
        AuthCategoryConfiguration authConfig = new AuthCategoryConfiguration();
        authConfig.populateFromJSON(json);

        doAnswer(invocation -> {
            assertEquals(context, invocation.getArgument(0));
            assertEquals(pluginConfig.toString(), invocation.getArgument(1).toString());
            assertTrue(invocation.getArgument(1) instanceof AWSConfiguration);

            Callback<UserStateDetails> callback = invocation.getArgument(2);
            callback.onResult(userStateDetails);
            return null;
        }).when(mobileClient).initialize(any(), any(), any());

        authCategory.configure(authConfig, context);
        verify(mobileClient).initialize(any(), any(), any());
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
                        new AWSCognitoAuthPlugin().getPluginKey(),
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

    /**
     * Tests that the signUp method of the Auth wrapper of AWSMobileClient (AMC) calls AMC.signUp() with the username
     * and password it received and, if included, the userAttributes and validationData sent in the options object.
     * Also ensures that in the onResult case, the success callback receives a valid AuthSignUpResult.
     * @throws AuthException test fails if this gets thrown since method should succeed
     */
    @Test
    public void signUp() throws AuthException {
        SignUpResult amcResult = new SignUpResult(
            false,
            new UserCodeDeliveryDetails(
                    DESTINATION,
                    DELIVERY_MEDIUM,
                    ATTRIBUTE_NAME
            ),
            null
        );

        doAnswer(invocation -> {
            assertEquals(USERNAME, invocation.getArgument(0));
            assertEquals(PASSWORD, invocation.getArgument(1));
            Map<String, String> attributeMap = invocation.getArgument(2);
            assertEquals(ATTRIBUTE_VAL, attributeMap.get(ATTRIBUTE_KEY));
            assertEquals(METADATA, invocation.getArgument(3));

            Callback<SignUpResult> callback = invocation.getArgument(4);
            callback.onResult(amcResult);
            return null;
        }).when(mobileClient).signUp(any(), any(), any(), any(), any());

        AuthSignUpResult result = synchronousAuth.signUp(
                USERNAME,
                PASSWORD,
                AWSCognitoAuthSignUpOptions.builder()
                    .userAttributes(
                        Collections.singletonList(new AuthUserAttribute(AuthUserAttributeKey.email(), ATTRIBUTE_VAL))
                    )
                    .validationData(METADATA)
                    .build()
        );

        validateSignUpResult(result, AuthSignUpStep.CONFIRM_SIGN_UP_STEP);
        verify(mobileClient).signUp(any(), any(), any(), any(), any());
    }

    /**
     * Tests that the confirmSignUp method of the Auth wrapper of AWSMobileClient (AMC) calls AMC.confirmSignUp with
     * the username and confirmation code it received.
     * Also ensures that in the onResult case, the success callback receives a valid AuthSignUpResult.
     * @throws AuthException test fails if this gets thrown since method should succeed
     */
    @Test
    public void confirmSignUp() throws AuthException {
        SignUpResult amcResult = new SignUpResult(
                true,
                new UserCodeDeliveryDetails(
                        DESTINATION,
                        DELIVERY_MEDIUM,
                        ATTRIBUTE_NAME
                ),
                null
        );

        doAnswer(invocation -> {
            assertEquals(USERNAME, invocation.getArgument(0));
            assertEquals(CONFIRMATION_CODE, invocation.getArgument(1));

            Callback<SignUpResult> callback = invocation.getArgument(2);
            callback.onResult(amcResult);
            return null;
        }).when(mobileClient).confirmSignUp(any(), any(), any());

        AuthSignUpResult result = synchronousAuth.confirmSignUp(USERNAME, CONFIRMATION_CODE);
        validateSignUpResult(result, AuthSignUpStep.DONE);
        verify(mobileClient).confirmSignUp(any(), any(), any());
    }

    /**
     * Tests that the resendSignUpCode method of the Auth wrapper of AWSMobileClient (AMC) calls AMC.resendSignUp with
     * the username it received .
     * Also ensures that in the onResult case, the success callback receives a valid AuthSignUpResult.
     * @throws AuthException test fails if this gets thrown since method should succeed
     */
    @Test
    public void resendSignUpCode() throws AuthException {
        SignUpResult amcResult = new SignUpResult(
                false,
                new UserCodeDeliveryDetails(
                        DESTINATION,
                        DELIVERY_MEDIUM,
                        ATTRIBUTE_NAME
                ),
                null
        );

        doAnswer(invocation -> {
            assertEquals(USERNAME, invocation.getArgument(0));

            Callback<SignUpResult> callback = invocation.getArgument(1);
            callback.onResult(amcResult);
            return null;
        }).when(mobileClient).resendSignUp(any(), any());

        AuthSignUpResult result = synchronousAuth.resendSignUpCode(USERNAME);
        validateSignUpResult(result, AuthSignUpStep.CONFIRM_SIGN_UP_STEP);
        verify(mobileClient).resendSignUp(any(), any());
    }

    /**
     * Tests that the signIn method of the Auth wrapper of AWSMobileClient (AMC) calls AMC.signIn with
     * the username, password it received, and, if included, the metadata sent in the options object.
     * Also ensures that in the onResult case, the success callback receives a valid AuthSignInResult.
     * @throws AuthException test fails if this gets thrown since method should succeed
     */
    @Test
    public void signIn() throws AuthException {
        SignInResult amcResult = new SignInResult(
            SignInState.SMS_MFA,
            new UserCodeDeliveryDetails(
                    DESTINATION,
                    DELIVERY_MEDIUM,
                    ATTRIBUTE_NAME
            )
        );

        doAnswer(invocation -> {
            assertEquals(USERNAME, invocation.getArgument(0));
            assertEquals(PASSWORD, invocation.getArgument(1));
            assertEquals(METADATA, invocation.getArgument(2));

            Callback<SignInResult> callback = invocation.getArgument(3);
            callback.onResult(amcResult);
            return null;
        }).when(mobileClient).signIn(any(), any(), any(), any());

        AuthSignInResult result = synchronousAuth.signIn(
                USERNAME,
                PASSWORD,
                AWSCognitoAuthSignInOptions.builder().metadata(METADATA).build()
        );

        validateSignInResult(
                result,
                false,
                AuthSignInStep.CONFIRM_SIGN_IN_WITH_SMS_MFA_CODE
        );

        verify(mobileClient).signIn(any(), any(), any(), any());
    }

    /**
     * Tests that the confirmSignIn method of the Auth wrapper of AWSMobileClient (AMC) calls AMC.confirmSignIn with
     * the confirmation code it received and the success callback receives a valid AuthSignInResult.
     * @throws AuthException test fails if this gets thrown since method should succeed
     */
    @Test
    public void confirmSignIn() throws AuthException {
        SignInResult amcResult = new SignInResult(
                SignInState.DONE,
                new UserCodeDeliveryDetails(
                        DESTINATION,
                        DELIVERY_MEDIUM,
                        ATTRIBUTE_NAME
                )
        );

        doAnswer(invocation -> {
            assertEquals(CONFIRMATION_CODE, invocation.getArgument(0));

            Callback<SignInResult> callback = invocation.getArgument(1);
            callback.onResult(amcResult);
            return null;
        }).when(mobileClient).confirmSignIn(any(String.class), any());

        AuthSignInResult result = synchronousAuth.confirmSignIn(CONFIRMATION_CODE);
        validateSignInResult(result, true, AuthSignInStep.DONE);
        verify(mobileClient).confirmSignIn(any(String.class), any());
    }

    /**
     * Tests that the resetPassword method of the Auth wrapper of AWSMobileClient (AMC) calls AMC.forgotPassword with
     * the username it received.
     * Also ensures that in the onResult case, the success callback receives a valid AuthResetPasswordResult and in
     * the onError case, the error call back receives an AuthException with the root cause attached.
     * @throws AuthException test fails if this gets thrown since method should succeed
     */
    @Test
    public void resetPassword() throws AuthException {
        ForgotPasswordResult amcResult = new ForgotPasswordResult(ForgotPasswordState.CONFIRMATION_CODE);
        amcResult.setParameters(new UserCodeDeliveryDetails(
                DESTINATION,
                DELIVERY_MEDIUM,
                ATTRIBUTE_NAME
        ));

        doAnswer(invocation -> {
            assertEquals(USERNAME, invocation.getArgument(0));

            Callback<ForgotPasswordResult> callback = invocation.getArgument(1);
            callback.onResult(amcResult);
            return null;
        }).when(mobileClient)
            .forgotPassword(any(), any());

        AuthResetPasswordResult result = synchronousAuth.resetPassword(USERNAME);
        assertFalse(result.isPasswordReset());
        assertEquals(
                AuthResetPasswordStep.CONFIRM_RESET_PASSWORD_WITH_CODE,
                result.getNextStep().getResetPasswordStep()
        );
        validateCodeDeliveryDetails(result.getNextStep().getCodeDeliveryDetails());
        verify(mobileClient).forgotPassword(any(), any());
    }

    /**
     * Tests that the confirmResetPassword method of the Auth wrapper of AWSMobileClient (AMC) calls
     * AMC.confirmForgotPassword with the new password and confirmation code it received.
     * Also ensures that in the onResult case, the success callback is triggered and in the onError case,
     * the error call back receives an AuthException with the root cause attached.
     * @throws AuthException test fails if this gets thrown since method should succeed
     */
    @Test
    public void confirmResetPassword() throws AuthException {
        ForgotPasswordResult amcResult = new ForgotPasswordResult(ForgotPasswordState.DONE);

        doAnswer(invocation -> {
            assertEquals(NEW_PASSWORD, invocation.getArgument(0));
            assertEquals(CONFIRMATION_CODE, invocation.getArgument(1));

            Callback<ForgotPasswordResult> callback = invocation.getArgument(2);
            callback.onResult(amcResult);
            return null;
        }).when(mobileClient)
            .confirmForgotPassword(any(), any(), any());

        synchronousAuth.confirmResetPassword(NEW_PASSWORD, CONFIRMATION_CODE);
        verify(mobileClient).confirmForgotPassword(any(), any(), any());
    }

    /**
     * Tests that the getCurrentUser method of the Auth wrapper of AWSMobileClient (AMC) returns a new
     * AWSCognitoAuthUser object containing the userId property in the plugin and the username from AMC.getUsername().
     */
    @Test
    public void getCurrentUser() {
        doAnswer(invocation -> USERNAME).when(mobileClient).getUsername();
        AuthUser user = authCategory.getCurrentUser();

        assertEquals(USER_ID, user.getUserId());
        assertEquals(USERNAME, user.getUsername());
    }

    /**
     * Tests that the getEscapeHatch method of the Auth wrapper of AWSMobileClient (AMC) returns the instance of
     * AWSMobileClient held by the plugin.
     */
    @Test
    public void getEscapeHatch() {
        AWSCognitoAuthPlugin plugin =
                (AWSCognitoAuthPlugin) authCategory.getPlugin(new AWSCognitoAuthPlugin().getPluginKey());
        AWSMobileClient client = plugin.getEscapeHatch();
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
        assertEquals(DESTINATION, codeDetails.getDestination());
        assertEquals(AuthCodeDeliveryDetails.DeliveryMedium.fromString(DELIVERY_MEDIUM),
                codeDetails.getDeliveryMedium());
        assertEquals(ATTRIBUTE_NAME, codeDetails.getAttributeName());
    }
}
