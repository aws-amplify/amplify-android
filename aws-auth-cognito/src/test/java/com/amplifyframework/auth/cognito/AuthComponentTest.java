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

import android.app.Activity;
import android.content.Context;

import com.amplifyframework.AmplifyException;
import com.amplifyframework.auth.AuthCategory;
import com.amplifyframework.auth.AuthCategoryConfiguration;
import com.amplifyframework.auth.AuthCodeDeliveryDetails;
import com.amplifyframework.auth.AuthException;
import com.amplifyframework.auth.AuthProvider;
import com.amplifyframework.auth.AuthUser;
import com.amplifyframework.auth.AuthUserAttributeKey;
import com.amplifyframework.auth.cognito.options.AWSCognitoAuthSignInOptions;
import com.amplifyframework.auth.cognito.options.AWSCognitoAuthSignUpOptions;
import com.amplifyframework.auth.cognito.options.AWSCognitoAuthWebUISignInOptions;
import com.amplifyframework.auth.result.AuthResetPasswordResult;
import com.amplifyframework.auth.result.AuthSessionResult;
import com.amplifyframework.auth.result.AuthSignInResult;
import com.amplifyframework.auth.result.AuthSignUpResult;
import com.amplifyframework.auth.result.step.AuthNextSignInStep;
import com.amplifyframework.auth.result.step.AuthNextSignUpStep;
import com.amplifyframework.auth.result.step.AuthResetPasswordStep;
import com.amplifyframework.auth.result.step.AuthSignInStep;
import com.amplifyframework.auth.result.step.AuthSignUpStep;
import com.amplifyframework.testutils.sync.SynchronousAuth;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.mobile.client.AWSMobileClient;
import com.amazonaws.mobile.client.Callback;
import com.amazonaws.mobile.client.HostedUIOptions;
import com.amazonaws.mobile.client.SignInUIOptions;
import com.amazonaws.mobile.client.SignOutOptions;
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
import org.mockito.ArgumentCaptor;
import org.robolectric.RobolectricTestRunner;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static androidx.test.core.app.ApplicationProvider.getApplicationContext;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * Test that the current implementation of Auth as a wrapper of AWSMobileClient calls the correct
 * AWSMobileClient methods with the correct parameters when the different Auth methods are called.
 */
@RunWith(RobolectricTestRunner.class)
public final class AuthComponentTest {
    private static final String DESTINATION = "e***@email.com";
    private static final String DELIVERY_MEDIUM = "sms";
    private static final String ATTRIBUTE_NAME = "email";
    private static final String USERNAME = "username";
    private static final String PASSWORD = "password123";
    private static final String NEW_PASSWORD = "newPassword123";
    private static final String ATTRIBUTE_KEY = AuthUserAttributeKey.email().getKeyString();
    private static final String ATTRIBUTE_VAL = "email@email.com";
    private static final String CONFIRMATION_CODE = "confirm";
    private static final String PLUGIN_KEY = "awsCognitoAuthPlugin";
    private static final String IDENTITY_ID = "identityId";
    private static final String ACCESS_KEY = "accessKey";
    private static final String SECRET_KEY = "secretKey";
    private static final String ID_TOKEN = "idToken";
    private static final String REFRESH_TOKEN = "refreshToken";
    // Actual JWT token used since access token needs to be parsable as part of user sub retrieval.
    private static final String ACCESS_TOKEN = "eyJraWQiOiJjM2VKd2oxMURcL2ozUE8zd0s2MFwvNWVvRFl2Z0lESmVpdDVaU0YzanFne" +
            "GM9IiwiYWxnIjoiUlMyNTYifQ.eyJzdWIiOiI2OWJjMjUyYi1kZDA3LTQxYzAtYjFkYi1hNDYwNjZiOGVmNTEiLCJldmVudF9pZCI6Im" +
            "U4ZWE3MDdiLTE4ODctNGQ3Zi04MDM4LTQwODdkYWE5ZjUwOSIsInRva2VuX3VzZSI6ImFjY2VzcyIsInNjb3BlIjoiYXdzLmNvZ25pdG" +
            "8uc2lnbmluLnVzZXIuYWRtaW4iLCJhdXRoX3RpbWUiOjE1ODkzOTM3NTksImlzcyI6Imh0dHBzOlwvXC9jb2duaXRvLWlkcC51cy1lYX" +
            "N0LTEuYW1hem9uYXdzLmNvbVwvdXMtZWFzdC0xX3lGT2w4ZktVVSIsImV4cCI6MTU4OTM5NzM1OSwiaWF0IjoxNTg5MzkzNzU5LCJqdG" +
            "kiOiI0OWIzOGVlYi1iYzAzLTRhOWEtOWQ2YS04YjczYjEyMWRjNmMiLCJjbGllbnRfaWQiOiIycW1yb3A5NWxjdmZqZ2Q5cGI4bGpmZ3" +
            "FyOCIsInVzZXJuYW1lIjoidXNlcm5hbWUyIn0.Y38IDR0wB1MvTxKP0K7mYeM6HLV-BQGQMhZsGJlWUbqXXZYy1R2aAs0Nz5EdBIVvN_" +
            "wIvDuWVK2M-9IS-lptJPQcjc8d5CHWPUNQzG4N7qkqDd5ATdj6Rvbpn8JiMgMJwM4bemNl4ZxlLs63iEMqzZUGq3iuwKHp8EpMAjSfwn" +
            "lNbI7OGWEfXR0FHA_pHtwu1cBHlHvf21R0saGdki2rcN_elSrizKMqESfyKLvYf-kv0N8aSMxJ0cujwevrfCe1a0WGuUmZkzbUO2AJ3o" +
            "O8KyMzoWePPXetwjBk7HB-RX9k-kltuHGrdMMEXMCHlWkSZJ7VwQksLOA2RMfQs-0i0w";
    // User sub value here should match the one encoded in the access token above
    private static final String USER_SUB = "69bc252b-dd07-41c0-b1db-a46066b8ef51";
    private static final Map<String, String> METADATA = Collections.singletonMap("aCustomKey", "aCustomVal");
    private AWSMobileClient mobileClient;
    private AuthCategory authCategory;
    private SynchronousAuth synchronousAuth;

    /**
     * Get all setup for the future tests with mocks and a standard response for tokens.
     * @throws AmplifyException If add plugin fails
     */
    @Before
    public void setup() throws AmplifyException {
        mobileClient = mock(AWSMobileClient.class);
        authCategory = new AuthCategory();
        authCategory.addPlugin(new AWSCognitoAuthPlugin(mobileClient, USER_SUB));
        synchronousAuth = SynchronousAuth.delegatingTo(authCategory);
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
        assertEquals(pluginConfig.toString(), awsConfigCaptor.getValue().toString());
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
            Callback<SignUpResult> callback = invocation.getArgument(4);
            callback.onResult(amcResult);
            return null;
        }).when(mobileClient).signUp(any(), any(), any(), any(), any());

        AWSCognitoAuthSignUpOptions options = AWSCognitoAuthSignUpOptions.builder()
                .userAttribute(AuthUserAttributeKey.email(), ATTRIBUTE_VAL)
                .validationData(METADATA)
                .build();

        AuthSignUpResult result = synchronousAuth.signUp(
                USERNAME,
                PASSWORD,
                options
        );

        validateSignUpResult(result, AuthSignUpStep.CONFIRM_SIGN_UP_STEP);
        Map<String, String> expectedAttributeMap = Collections.singletonMap(ATTRIBUTE_KEY, ATTRIBUTE_VAL);
        verify(mobileClient).signUp(eq(USERNAME), eq(PASSWORD), eq(expectedAttributeMap), eq(METADATA), any());
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
            Callback<SignUpResult> callback = invocation.getArgument(2);
            callback.onResult(amcResult);
            return null;
        }).when(mobileClient).confirmSignUp(any(), any(), any());

        AuthSignUpResult result = synchronousAuth.confirmSignUp(USERNAME, CONFIRMATION_CODE);
        validateSignUpResult(result, AuthSignUpStep.DONE);
        verify(mobileClient).confirmSignUp(eq(USERNAME), eq(CONFIRMATION_CODE), any());
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
            Callback<SignUpResult> callback = invocation.getArgument(1);
            callback.onResult(amcResult);
            return null;
        }).when(mobileClient).resendSignUp(any(), any());

        AuthSignUpResult result = synchronousAuth.resendSignUpCode(USERNAME);
        validateSignUpResult(result, AuthSignUpStep.CONFIRM_SIGN_UP_STEP);
        verify(mobileClient).resendSignUp(eq(USERNAME), any());
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

        Tokens tokensResult = new Tokens(ACCESS_TOKEN, ID_TOKEN, REFRESH_TOKEN);
        doAnswer(invocation -> {
            Callback<Tokens> callback = invocation.getArgument(0);
            callback.onResult(tokensResult);
            return null;
        }).when(mobileClient).getTokens(any());

        doAnswer(invocation -> {
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

        verify(mobileClient).signIn(eq(USERNAME), eq(PASSWORD), eq(METADATA), any());
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

        Tokens tokensResult = new Tokens(ACCESS_TOKEN, ID_TOKEN, REFRESH_TOKEN);
        doAnswer(invocation -> {
            Callback<Tokens> callback = invocation.getArgument(0);
            callback.onResult(tokensResult);
            return null;
        }).when(mobileClient).getTokens(any());

        doAnswer(invocation -> {
            Callback<SignInResult> callback = invocation.getArgument(1);
            callback.onResult(amcResult);
            return null;
        }).when(mobileClient).confirmSignIn(any(String.class), any());

        AuthSignInResult result = synchronousAuth.confirmSignIn(CONFIRMATION_CODE);
        validateSignInResult(result, true, AuthSignInStep.DONE);
        verify(mobileClient).confirmSignIn(eq(CONFIRMATION_CODE), any());
    }

    /**
     * Tests that the signInWithSocialWebUI method of the Auth wrapper of AWSMobileClient (AMC) calls AMC.showSignIn
     * with the proper parameters and converts the returned result to the proper AuthSignInResult.
     * @throws AuthException test fails if this gets thrown since method should succeed
     */
    @Test
    public void signInWithSocialWebUI() throws AuthException {
        Map<String, String> additionalInfoMap = Collections.singletonMap("testKey", "testVal");
        UserStateDetails userStateResult = new UserStateDetails(UserState.SIGNED_IN, additionalInfoMap);

        doAnswer(invocation -> {
            Callback<UserStateDetails> callback = invocation.getArgument(2);
            callback.onResult(userStateResult);
            return null;
        }).when(mobileClient).showSignIn(any(), any(), any());

        Tokens tokensResult = new Tokens(ACCESS_TOKEN, ID_TOKEN, REFRESH_TOKEN);
        doAnswer(invocation -> {
            Callback<Tokens> callback = invocation.getArgument(0);
            callback.onResult(tokensResult);
            return null;
        }).when(mobileClient).getTokens(any());

        Activity activity = new Activity();
        AuthSignInResult result = synchronousAuth.signInWithSocialWebUI(
                AuthProvider.facebook(),
                activity
        );
        assertTrue(result.isSignInComplete());
        assertEquals(AuthSignInStep.DONE, result.getNextStep().getSignInStep());
        assertEquals(additionalInfoMap, result.getNextStep().getAdditionalInfo());

        ArgumentCaptor<SignInUIOptions> optionsCaptor = ArgumentCaptor.forClass(SignInUIOptions.class);
        verify(mobileClient).showSignIn(eq(activity), optionsCaptor.capture(), any());
        HostedUIOptions hostedUIOptions = optionsCaptor.getValue().getHostedUIOptions();
        assertNotNull(hostedUIOptions);
        assertEquals("Facebook", hostedUIOptions.getIdentityProvider());
    }

    /**
     * Tests that the signInWithWebUI method of the Auth wrapper of AWSMobileClient (AMC) calls AMC.showSignIn
     * with the proper parameters and converts the returned result to the proper AuthSignInResult.
     * @throws AuthException test fails if this gets thrown since method should succeed
     */
    @Test
    public void signInWithWebUI() throws AuthException {
        Map<String, String> additionalInfoMap = Collections.singletonMap("testKey", "testVal");
        UserStateDetails userStateResult = new UserStateDetails(UserState.SIGNED_IN, additionalInfoMap);

        doAnswer(invocation -> {
            Callback<UserStateDetails> callback = invocation.getArgument(2);
            callback.onResult(userStateResult);
            return null;
        }).when(mobileClient).showSignIn(any(), any(), any());

        Tokens tokensResult = new Tokens(ACCESS_TOKEN, ID_TOKEN, REFRESH_TOKEN);
        doAnswer(invocation -> {
            Callback<Tokens> callback = invocation.getArgument(0);
            callback.onResult(tokensResult);
            return null;
        }).when(mobileClient).getTokens(any());

        Activity activity = new Activity();
        String federationProviderName = "testFedProvider";
        String idpIdentifier = "testIdpID";
        List<String> scopes = Collections.singletonList("scope");
        Map<String, String> signInMap = Collections.singletonMap("signInKey", "signInVal");
        Map<String, String> signOutMap = Collections.singletonMap("signOutKey", "signOutVal");
        Map<String, String> tokensMap = Collections.singletonMap("tokensKey", "tokensVal");

        AuthSignInResult result = synchronousAuth.signInWithWebUI(
                activity,
                AWSCognitoAuthWebUISignInOptions
                        .builder()
                        .federationProviderName(federationProviderName)
                        .idpIdentifier(idpIdentifier)
                        .scopes(scopes)
                        .signInQueryParameters(signInMap)
                        .signOutQueryParameters(signOutMap)
                        .tokenQueryParameters(tokensMap)
                        .build()
        );
        assertTrue(result.isSignInComplete());
        assertEquals(AuthSignInStep.DONE, result.getNextStep().getSignInStep());
        assertEquals(additionalInfoMap, result.getNextStep().getAdditionalInfo());

        ArgumentCaptor<SignInUIOptions> optionsCaptor = ArgumentCaptor.forClass(SignInUIOptions.class);
        verify(mobileClient).showSignIn(eq(activity), optionsCaptor.capture(), any());
        HostedUIOptions hostedUIOptions = optionsCaptor.getValue().getHostedUIOptions();
        assertNotNull(hostedUIOptions);
        assertNull(hostedUIOptions.getIdentityProvider());
        assertEquals(federationProviderName, hostedUIOptions.getFederationProviderName());
        assertEquals(idpIdentifier, hostedUIOptions.getIdpIdentifier());
        assertArrayEquals(scopes.toArray(), hostedUIOptions.getScopes());
        assertEquals(signInMap, hostedUIOptions.getSignInQueryParameters());
        assertEquals(signOutMap, hostedUIOptions.getSignOutQueryParameters());
        assertEquals(tokensMap, hostedUIOptions.getTokenQueryParameters());
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
        verify(mobileClient).forgotPassword(eq(USERNAME), any());
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
            Callback<ForgotPasswordResult> callback = invocation.getArgument(2);
            callback.onResult(amcResult);
            return null;
        }).when(mobileClient)
            .confirmForgotPassword(any(), any(), any());

        synchronousAuth.confirmResetPassword(NEW_PASSWORD, CONFIRMATION_CODE);
        verify(mobileClient).confirmForgotPassword(eq(NEW_PASSWORD), eq(CONFIRMATION_CODE), any());
    }

    /**
     * Tests that signOut calls the AWSMobileClient sign out method with the global signout option set to true.
     * @throws AuthException test fails if this gets thrown since method should succeed
     */
    @Test
    public void signOut() throws AuthException {
        doAnswer(invocation -> {
            Callback<Void> callback = invocation.getArgument(1);
            callback.onResult(null);
            return null;
        }).when(mobileClient).signOut(any(), any());

        synchronousAuth.signOut();

        ArgumentCaptor<SignOutOptions> signOutOptionsCaptor = ArgumentCaptor.forClass(SignOutOptions.class);
        verify(mobileClient).signOut(signOutOptionsCaptor.capture(), any());
        assertTrue(signOutOptionsCaptor.getValue().isSignOutGlobally());
    }

    /**
     * Tests that if sign out fails, the returned Exception gets wrapped in an AuthException.
     * @throws AuthException expected exception
     */
    @Test(expected = AuthException.class)
    public void signOutFails() throws AuthException {
        Exception exception = new Exception("Test exception");

        doAnswer(invocation -> {
            Callback<Void> callback = invocation.getArgument(1);
            callback.onError(exception);
            return null;
        }).when(mobileClient).signOut(any(), any());

        synchronousAuth.signOut();
    }

    /**
     * Test that a signed out account which supports identity pools but doesn't have guest credentials returns failed
     * results for all fields with the signed out exception.
     * @throws AuthException test fails if this gets thrown since method should succeed
     */
    @Test
    public void signedOutSessionWithIdentityPoolAndNoGuest() throws AuthException {
        doAnswer(invocation -> null).when(mobileClient).getIdentityId();

        UserStateDetails stateResult = new UserStateDetails(UserState.SIGNED_OUT, null);
        doAnswer(invocation -> {
            Callback<UserStateDetails> callback = invocation.getArgument(0);
            callback.onResult(stateResult);
            return null;
        }).when(mobileClient).currentUserState(any());

        AWSCognitoAuthSession authSession = (AWSCognitoAuthSession) synchronousAuth.fetchAuthSession();
        verify(mobileClient).currentUserState(any());
        verify(mobileClient).getIdentityId();

        AWSCognitoAuthSession expectedResult = new AWSCognitoAuthSession(
                false,
                AuthSessionResult.failure(new AuthException.SignedOutException()),
                AuthSessionResult.failure(new AuthException.SignedOutException()),
                AuthSessionResult.failure(new AuthException.SignedOutException()),
                AuthSessionResult.failure(new AuthException.SignedOutException())
        );

        assertEquals(expectedResult, authSession);
    }

    /**
     * Test that a signed out account which supports identity pools and has guest credentials returns the proper
     * success results for identity pool fields and signed out failure results for user pool fields.
     * @throws AuthException test fails if this gets thrown since method should succeed
     */
    @Test
    public void signedOutSessionWithIdentityPoolAndGuest() throws AuthException {
        doAnswer(invocation -> IDENTITY_ID).when(mobileClient).getIdentityId();

        AWSCredentials awsCredentialsResult = new BasicAWSCredentials(ACCESS_KEY, SECRET_KEY);
        doAnswer(invocation -> {
            Callback<AWSCredentials> callback = invocation.getArgument(0);
            callback.onResult(awsCredentialsResult);
            return null;
        }).when(mobileClient).getAWSCredentials(any());

        UserStateDetails stateResult = new UserStateDetails(UserState.GUEST, null);
        doAnswer(invocation -> {
            Callback<UserStateDetails> callback = invocation.getArgument(0);
            callback.onResult(stateResult);
            return null;
        }).when(mobileClient).currentUserState(any());

        AWSCognitoAuthSession authSession = (AWSCognitoAuthSession) synchronousAuth.fetchAuthSession();
        verify(mobileClient).currentUserState(any());
        verify(mobileClient).getIdentityId();
        verify(mobileClient).getAWSCredentials(any());

        AWSCognitoAuthSession expectedResult = new AWSCognitoAuthSession(
                false,
                AuthSessionResult.success(IDENTITY_ID),
                AuthSessionResult.success(awsCredentialsResult),
                AuthSessionResult.failure(new AuthException.SignedOutException()),
                AuthSessionResult.failure(new AuthException.SignedOutException())
        );

        assertEquals(expectedResult, authSession);
    }

    /**
     * Test that a signed out account which does not support identity pools returns invalid account failures for
     * identity pool fields and signed out failures for user pool fields.
     * @throws AuthException test fails if this gets thrown since method should succeed
     */
    @Test
    public void signedOutSessionWithNoIdentityPool() throws AuthException {
        doThrow(new RuntimeException()).when(mobileClient).getIdentityId();

        AWSCredentials awsCredentialsResult = new BasicAWSCredentials(ACCESS_KEY, SECRET_KEY);
        doAnswer(invocation -> {
            Callback<AWSCredentials> callback = invocation.getArgument(0);
            callback.onResult(awsCredentialsResult);
            return null;
        }).when(mobileClient).getAWSCredentials(any());

        UserStateDetails stateResult = new UserStateDetails(UserState.SIGNED_OUT, null);
        doAnswer(invocation -> {
            Callback<UserStateDetails> callback = invocation.getArgument(0);
            callback.onResult(stateResult);
            return null;
        }).when(mobileClient).currentUserState(any());

        AWSCognitoAuthSession authSession = (AWSCognitoAuthSession) synchronousAuth.fetchAuthSession();
        verify(mobileClient).currentUserState(any());
        verify(mobileClient).getIdentityId();

        AWSCognitoAuthSession expectedResult = new AWSCognitoAuthSession(
                false,
                AuthSessionResult.failure(new AuthException.InvalidAccountTypeException()),
                AuthSessionResult.failure(new AuthException.InvalidAccountTypeException()),
                AuthSessionResult.failure(new AuthException.SignedOutException()),
                AuthSessionResult.failure(new AuthException.SignedOutException())
        );

        assertEquals(expectedResult, authSession);
    }

    /**
     * Test that a signed in account with user and identity pool support returns all proper success results.
     * @throws AuthException test fails if this gets thrown since method should succeed
     */
    @Test
    public void signedInSessionWithIdentityAndUserPools() throws AuthException {
        doAnswer(invocation -> IDENTITY_ID).when(mobileClient).getIdentityId();

        UserStateDetails stateResult = new UserStateDetails(UserState.SIGNED_IN, null);
        doAnswer(invocation -> {
            Callback<UserStateDetails> callback = invocation.getArgument(0);
            callback.onResult(stateResult);
            return null;
        }).when(mobileClient).currentUserState(any());

        Tokens tokensResult = new Tokens(ACCESS_TOKEN, ID_TOKEN, REFRESH_TOKEN);
        doAnswer(invocation -> {
            Callback<Tokens> callback = invocation.getArgument(0);
            callback.onResult(tokensResult);
            return null;
        }).when(mobileClient).getTokens(any());

        AWSCredentials awsCredentialsResult = new BasicAWSCredentials(ACCESS_KEY, SECRET_KEY);
        doAnswer(invocation -> {
            Callback<AWSCredentials> callback = invocation.getArgument(0);
            callback.onResult(awsCredentialsResult);
            return null;
        }).when(mobileClient).getAWSCredentials(any());

        AWSCognitoAuthSession authSession = (AWSCognitoAuthSession) synchronousAuth.fetchAuthSession();
        verify(mobileClient).currentUserState(any());
        verify(mobileClient).getTokens(any());
        verify(mobileClient).getAWSCredentials(any());
        verify(mobileClient).getIdentityId();

        AWSCognitoAuthSession expectedResult = new AWSCognitoAuthSession(
                true,
                AuthSessionResult.success(IDENTITY_ID),
                AuthSessionResult.success(awsCredentialsResult),
                AuthSessionResult.success(USER_SUB),
                AuthSessionResult.success(new AWSCognitoUserPoolTokens(ACCESS_TOKEN, ID_TOKEN, REFRESH_TOKEN))
        );

        assertEquals(expectedResult, authSession);
    }

    /**
     * Test that a signed in account with only identity pool support returns proper success results for identity fields
     * and invalid account failures for user pool results.
     * @throws AuthException test fails if this gets thrown since method should succeed
     */
    @Test
    public void signedInSessionWithIdentityPoolOnly() throws AuthException {
        doAnswer(invocation -> IDENTITY_ID).when(mobileClient).getIdentityId();

        UserStateDetails stateResult = new UserStateDetails(UserState.SIGNED_IN, null);
        doAnswer(invocation -> {
            Callback<UserStateDetails> callback = invocation.getArgument(0);
            callback.onResult(stateResult);
            return null;
        }).when(mobileClient).currentUserState(any());

        doAnswer(invocation -> {
            Callback<Tokens> callback = invocation.getArgument(0);
            callback.onError(new Exception("You must be signed-in with Cognito Userpools to be able to use getTokens"));
            return null;
        }).when(mobileClient).getTokens(any());

        AWSCredentials awsCredentialsResult = new BasicAWSCredentials(ACCESS_KEY, SECRET_KEY);
        doAnswer(invocation -> {
            Callback<AWSCredentials> callback = invocation.getArgument(0);
            callback.onResult(awsCredentialsResult);
            return null;
        }).when(mobileClient).getAWSCredentials(any());

        AWSCognitoAuthSession authSession = (AWSCognitoAuthSession) synchronousAuth.fetchAuthSession();
        verify(mobileClient).currentUserState(any());
        verify(mobileClient).getTokens(any());
        verify(mobileClient).getAWSCredentials(any());
        verify(mobileClient).getIdentityId();

        AWSCognitoAuthSession expectedResult = new AWSCognitoAuthSession(
                true,
                AuthSessionResult.success(IDENTITY_ID),
                AuthSessionResult.success(awsCredentialsResult),
                AuthSessionResult.failure(new AuthException.InvalidAccountTypeException()),
                AuthSessionResult.failure(new AuthException.InvalidAccountTypeException())
        );

        assertEquals(expectedResult, authSession);
    }

    /**
     * Test that a signed in account with only user pool support returns proper success results for user pool fields
     * and invalid account failures for identity results.
     * @throws AuthException test fails if this gets thrown since method should succeed
     */
    @Test
    public void signedInSessionWithUserPoolOnly() throws AuthException {
        doAnswer(invocation -> IDENTITY_ID).when(mobileClient).getIdentityId();

        UserStateDetails stateResult = new UserStateDetails(UserState.SIGNED_IN, null);
        doAnswer(invocation -> {
            Callback<UserStateDetails> callback = invocation.getArgument(0);
            callback.onResult(stateResult);
            return null;
        }).when(mobileClient).currentUserState(any());

        Tokens tokensResult = new Tokens(ACCESS_TOKEN, ID_TOKEN, REFRESH_TOKEN);
        doAnswer(invocation -> {
            Callback<Tokens> callback = invocation.getArgument(0);
            callback.onResult(tokensResult);
            return null;
        }).when(mobileClient).getTokens(any());

        Exception credentialsException = new Exception("Cognito Identity not configured");
        doAnswer(invocation -> {
            Callback<AWSCredentials> callback = invocation.getArgument(0);
            callback.onError(credentialsException);
            return null;
        }).when(mobileClient).getAWSCredentials(any());

        AWSCognitoAuthSession authSession = (AWSCognitoAuthSession) synchronousAuth.fetchAuthSession();
        verify(mobileClient).currentUserState(any());
        verify(mobileClient).getTokens(any());
        verify(mobileClient).getAWSCredentials(any());

        AWSCognitoAuthSession expectedResult = new AWSCognitoAuthSession(
                true,
                AuthSessionResult.failure(new AuthException.InvalidAccountTypeException(credentialsException)),
                AuthSessionResult.failure(new AuthException.InvalidAccountTypeException(credentialsException)),
                AuthSessionResult.success(USER_SUB),
                AuthSessionResult.success(new AWSCognitoUserPoolTokens(ACCESS_TOKEN, ID_TOKEN, REFRESH_TOKEN))
        );

        assertEquals(expectedResult, authSession);
    }

    /**
     * Test that a signed in account with expired tokens gets back expired session exceptions for all fields.
     * @throws AuthException test fails if this gets thrown since method should succeed
     */
    @Test
    public void signedInSessionWithExpiredTokens() throws AuthException {
        UserStateDetails stateResult = new UserStateDetails(UserState.SIGNED_OUT_USER_POOLS_TOKENS_INVALID, null);
        doAnswer(invocation -> {
            Callback<UserStateDetails> callback = invocation.getArgument(0);
            callback.onResult(stateResult);
            return null;
        }).when(mobileClient).currentUserState(any());

        AWSCognitoAuthSession authSession = (AWSCognitoAuthSession) synchronousAuth.fetchAuthSession();
        verify(mobileClient).currentUserState(any());

        AWSCognitoAuthSession expectedResult = new AWSCognitoAuthSession(
                true,
                AuthSessionResult.failure(new AuthException.SessionExpiredException()),
                AuthSessionResult.failure(new AuthException.SessionExpiredException()),
                AuthSessionResult.failure(new AuthException.SessionExpiredException()),
                AuthSessionResult.failure(new AuthException.SessionExpiredException())
        );

        assertEquals(expectedResult, authSession);
    }

    /**
     * Tests that the getCurrentUser method of the Auth wrapper of AWSMobileClient (AMC) returns a new
     * AWSCognitoAuthUser object containing the userId property in the plugin and the username from AMC.getUsername().
     */
    @Test
    public void getCurrentUser() {
        doAnswer(invocation -> USERNAME).when(mobileClient).getUsername();
        AuthUser user = authCategory.getCurrentUser();

        assertEquals(USER_SUB, user.getUserId());
        assertEquals(USERNAME, user.getUsername());
    }

    /**
     * Tests that the getEscapeHatch method of the Auth wrapper of AWSMobileClient (AMC) returns the instance of
     * AWSMobileClient held by the plugin.
     */
    @Test
    public void getEscapeHatch() {
        AWSCognitoAuthPlugin plugin =
                (AWSCognitoAuthPlugin) authCategory.getPlugin(PLUGIN_KEY);
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
