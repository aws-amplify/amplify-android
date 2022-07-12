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

package com.amplifyframework.rx;

import android.app.Activity;
import android.content.Intent;

import com.amplifyframework.auth.AuthCategoryBehavior;
import com.amplifyframework.auth.AuthCodeDeliveryDetails;
import com.amplifyframework.auth.AuthCodeDeliveryDetails.DeliveryMedium;
import com.amplifyframework.auth.AuthDevice;
import com.amplifyframework.auth.AuthException;
import com.amplifyframework.auth.AuthProvider;
import com.amplifyframework.auth.AuthSession;
import com.amplifyframework.auth.AuthUser;
import com.amplifyframework.auth.AuthUserAttribute;
import com.amplifyframework.auth.AuthUserAttributeKey;
import com.amplifyframework.auth.options.AuthSignUpOptions;
import com.amplifyframework.auth.result.AuthResetPasswordResult;
import com.amplifyframework.auth.result.AuthSignInResult;
import com.amplifyframework.auth.result.AuthSignUpResult;
import com.amplifyframework.auth.result.AuthUpdateAttributeResult;
import com.amplifyframework.auth.result.step.AuthNextResetPasswordStep;
import com.amplifyframework.auth.result.step.AuthNextSignInStep;
import com.amplifyframework.auth.result.step.AuthNextSignUpStep;
import com.amplifyframework.auth.result.step.AuthNextUpdateAttributeStep;
import com.amplifyframework.auth.result.step.AuthResetPasswordStep;
import com.amplifyframework.auth.result.step.AuthSignInStep;
import com.amplifyframework.auth.result.step.AuthSignUpStep;
import com.amplifyframework.auth.result.step.AuthUpdateAttributeStep;
import com.amplifyframework.core.Action;
import com.amplifyframework.core.Consumer;
import com.amplifyframework.testutils.random.RandomString;

import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import io.reactivex.rxjava3.observers.TestObserver;

import static com.amplifyframework.rx.Matchers.anyAction;
import static com.amplifyframework.rx.Matchers.anyConsumer;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * Tests the {@link RxAuthBinding}.
 */
public final class RxAuthBindingTest {
    private static final long TIMEOUT_SECONDS = 2;
    private static final String ATTRIBUTE_KEY = AuthUserAttributeKey.email().getKeyString();
    private static final String ATTRIBUTE_VAL = "email@email.com";
    private static final String ATTRIBUTE_KEY_WITHOUT_CODE_DELIVERY = AuthUserAttributeKey.name().getKeyString();
    private static final String ATTRIBUTE_VAL_WITHOUT_CODE_DELIVERY = "name";
    private static final String CONFIRMATION_CODE = "confirm";
    private static final String DESTINATION = "e***@email.com";
    private static final String ATTRIBUTE_NAME = "email";

    private AuthCategoryBehavior delegate;
    private RxAuthBinding auth;

    /**
     * Creates an {@link RxAuthBinding} instance to test.
     * It is tested by arranging behaviors on its {@link AuthCategoryBehavior} delegate.
     */
    @Before
    public void setup() {
        this.delegate = mock(AuthCategoryBehavior.class);
        this.auth = new RxAuthBinding(delegate);
    }

    /**
     * Validates that a sign-up result are passed through the binding.
     * @throws InterruptedException If test observer is interrupted while awaiting terminal event
     */
    @Test
    public void testSignUpSucceeds() throws InterruptedException {
        // Arrange a response from delegate
        String username = RandomString.string();
        String password = RandomString.string();
        AuthSignUpOptions options = AuthSignUpOptions.builder().build();

        // Arrange a result on the result consumer
        AuthCodeDeliveryDetails details = new AuthCodeDeliveryDetails(RandomString.string(), DeliveryMedium.SMS);
        AuthSignUpStep step = AuthSignUpStep.CONFIRM_SIGN_UP_STEP;
        AuthNextSignUpStep nextStep = new AuthNextSignUpStep(step, Collections.emptyMap(), details);
        AuthSignUpResult result = new AuthSignUpResult(false, nextStep, null);
        doAnswer(invocation -> {
            // 0 = username, 1 = pass, 2 = options, 3 = onSuccess, 4 = onFailure
            int positionOfSuccessConsumer = 3;
            Consumer<AuthSignUpResult> onResult = invocation.getArgument(positionOfSuccessConsumer);
            onResult.accept(result);
            return null;
        }).when(delegate).signUp(eq(username), eq(password), eq(options), anyConsumer(), anyConsumer());

        // Act: call the binding
        TestObserver<AuthSignUpResult> observer = auth.signUp(username, password, options).test();

        // Assert: the result was furnished to the Rx Single
        observer.await(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        observer
            .assertNoErrors()
            .assertValue(result);
    }

    /**
     * Validates that a sign-up failure are passed through the binding.
     * @throws InterruptedException If test observer is interrupted while awaiting terminal event
     */
    @Test
    public void testSignUpFails() throws InterruptedException {
        String username = RandomString.string();
        String password = RandomString.string();
        AuthSignUpOptions options = AuthSignUpOptions.builder().build();

        // Arrange a callback on the failure consumer
        AuthException failure = new AuthException("Sign up", "has failed");
        doAnswer(invocation -> {
            // 0 = username, 1 = pass, 2 = options, 3 = onSuccess, 4 = onFailure
            int positionOfFailureConsumer = 4;
            Consumer<AuthException> onFailure = invocation.getArgument(positionOfFailureConsumer);
            onFailure.accept(failure);
            return null;
        }).when(delegate).signUp(eq(username), eq(password), eq(options), anyConsumer(), anyConsumer());

        // Act: call the binding
        TestObserver<AuthSignUpResult> observer = auth.signUp(username, password, options).test();

        // Assert: error is furnished via Rx single.
        observer.await(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        observer
            .assertNoValues()
            .assertError(failure);
    }

    /**
     * Validates that a successful call to resend the sign-up code will propagate the result
     * back through the binding.
     * @throws InterruptedException If test observer is interrupted while awaiting terminal event
     */
    @Test
    public void testResendSignUpCodeSucceeds() throws InterruptedException {
        String username = RandomString.string();

        // Arrange a result on the result consumer
        AuthCodeDeliveryDetails details = new AuthCodeDeliveryDetails(RandomString.string(), DeliveryMedium.EMAIL);
        AuthSignUpStep step = AuthSignUpStep.CONFIRM_SIGN_UP_STEP;
        AuthNextSignUpStep nextStep = new AuthNextSignUpStep(step, Collections.emptyMap(), details);
        AuthSignUpResult result = new AuthSignUpResult(false, nextStep, null);
        doAnswer(invocation -> {
            // 0 = username, 1 = onResult, 2 = onFailure
            int positionOfResultConsumer = 1;
            Consumer<AuthSignUpResult> onResult = invocation.getArgument(positionOfResultConsumer);
            onResult.accept(result);
            return null;
        }).when(delegate).resendSignUpCode(eq(username), anyConsumer(), anyConsumer());

        // Act: call the binding
        TestObserver<AuthSignUpResult> observer = auth.resendSignUpCode(username).test();

        // Assert: the result was furnished to the Rx Single
        observer.await(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        observer
            .assertNoErrors()
            .assertValue(result);
    }

    /**
     * Validates that a failed call to resend the sign-up code will propagate the failure
     * back through the binding.
     * @throws InterruptedException If test observer is interrupted while awaiting terminal event
     */
    @Test
    public void testResendSignUpCodeFails() throws InterruptedException {
        String username = RandomString.string();

        // Arrange a failure on the failure consumer
        AuthException failure = new AuthException("Reset sign up", " has failed.");
        doAnswer(invocation -> {
            // 0 = username, 1 = onResult, 2 = onFailure
            int positionOfFailureConsumer = 2;
            Consumer<AuthException> onFailure = invocation.getArgument(positionOfFailureConsumer);
            onFailure.accept(failure);
            return null;
        }).when(delegate).resendSignUpCode(eq(username), anyConsumer(), anyConsumer());

        // Act: call the binding
        TestObserver<AuthSignUpResult> observer = auth.resendSignUpCode(username).test();

        // Assert: the result was furnished to the Rx Single
        observer.await(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        observer
            .assertNoValues()
            .assertError(failure);
    }

    /**
     * Validates that a successful call to sign-in will propagate the result
     * back through the binding.
     * @throws InterruptedException If test observer is interrupted while awaiting terminal event
     */
    @Test
    public void testSignInSucceeds() throws InterruptedException {
        String username = RandomString.string();
        String password = RandomString.string();

        // Arrange a result on the result consumer
        AuthCodeDeliveryDetails details = new AuthCodeDeliveryDetails(RandomString.string(), DeliveryMedium.EMAIL);
        AuthSignInStep step = AuthSignInStep.CONFIRM_SIGN_IN_WITH_CUSTOM_CHALLENGE;
        AuthNextSignInStep nextStep = new AuthNextSignInStep(step, Collections.emptyMap(), details);
        AuthSignInResult result = new AuthSignInResult(false, nextStep);
        doAnswer(invocation -> {
            // 0 = username, 1 = password, 2 = onResult, 3 = onFailure
            int positionOfResultConsumer = 2;
            Consumer<AuthSignInResult> onResult = invocation.getArgument(positionOfResultConsumer);
            onResult.accept(result);
            return null;
        }).when(delegate).signIn(eq(username), eq(password), anyConsumer(), anyConsumer());

        // Act: call the binding
        TestObserver<AuthSignInResult> observer = auth.signIn(username, password).test();

        // Assert: the result was furnished to the Rx Single
        observer.await(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        observer
            .assertNoErrors()
            .assertValue(result);
    }

    /**
     * Validates that a failed call to sign-in will propagate the result
     * back through the binding.
     * @throws InterruptedException If test observer is interrupted while awaiting terminal event
     */
    @Test
    public void testSignInFails() throws InterruptedException {
        String username = RandomString.string();
        String password = RandomString.string();

        // Arrange a failure on the failure consumer
        AuthException failure = new AuthException("Sign in", " has failed.");
        doAnswer(invocation -> {
            // 0 = username, 1 = password, 2 = onResult, 3 = onFailure
            int positionOfFailureConsumer = 3;
            Consumer<AuthException> onFailure = invocation.getArgument(positionOfFailureConsumer);
            onFailure.accept(failure);
            return null;
        }).when(delegate).signIn(eq(username), eq(password), anyConsumer(), anyConsumer());

        // Act: call the binding
        TestObserver<AuthSignInResult> observer = auth.signIn(username, password).test();

        // Assert: the failure was furnished to the Rx Single
        observer.await(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        observer
            .assertNoValues()
            .assertError(failure);
    }

    /**
     * Validates that a successful call to confirm sign-in will propagate the result
     * back through the binding.
     * @throws InterruptedException If test observer is interrupted while awaiting terminal event
     */
    @Test
    public void testConfirmSignInSucceeds() throws InterruptedException {
        String confirmationCode = RandomString.string();

        // Arrange a successful result.
        AuthSignInStep step = AuthSignInStep.CONFIRM_SIGN_IN_WITH_SMS_MFA_CODE;
        AuthCodeDeliveryDetails details = new AuthCodeDeliveryDetails(RandomString.string(), DeliveryMedium.UNKNOWN);
        AuthNextSignInStep nextStep = new AuthNextSignInStep(step, Collections.emptyMap(), details);
        AuthSignInResult expected = new AuthSignInResult(true, nextStep);
        doAnswer(invocation -> {
            // 0 = confirm code, 1 = onResult, 2 = onFailure
            int positionOfResultConsumer = 1;
            Consumer<AuthSignInResult> onResult = invocation.getArgument(positionOfResultConsumer);
            onResult.accept(expected);
            return null;
        }).when(delegate).confirmSignIn(eq(confirmationCode), anyConsumer(), anyConsumer());

        // Act: call the binding
        TestObserver<AuthSignInResult> observer = auth.confirmSignIn(confirmationCode).test();

        // Assert: result is furnished
        observer.await(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        observer
            .assertNoErrors()
            .assertValue(expected);
    }

    /**
     * Validates that a failed call to confirm sign-in will propagate the failure
     * back through the binding.
     * @throws InterruptedException If test observer is interrupted while awaiting terminal event
     */
    @Test
    public void testConfirmSignInFails() throws InterruptedException {
        String confirmationCode = RandomString.string();

        // Arrange a failure.
        AuthException failure = new AuthException("Confirmation of sign in", " has failed.");
        doAnswer(invocation -> {
            // 0 = confirm code, 1 = onResult, 2 = onFailure
            int positionOfFailureConsumer = 2;
            Consumer<AuthException> onResult = invocation.getArgument(positionOfFailureConsumer);
            onResult.accept(failure);
            return null;
        }).when(delegate).confirmSignIn(eq(confirmationCode), anyConsumer(), anyConsumer());

        // Act: call the binding
        TestObserver<AuthSignInResult> observer = auth.confirmSignIn(confirmationCode).test();

        // Assert: failure is furnished
        observer.await(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        observer
            .assertNoValues()
            .assertError(failure);
    }

    /**
     * Validates that a successful call to sign-in with social web UI will propagate the result
     * back through the binding.
     * @throws InterruptedException If test observer is interrupted while awaiting terminal event
     */
    @Test
    public void testSignInWithSocialWebUISucceeds() throws InterruptedException {
        AuthProvider provider = AuthProvider.amazon();
        Activity activity = new Activity();

        // Arrange a successful result
        AuthSignInStep step = AuthSignInStep.CONFIRM_SIGN_IN_WITH_SMS_MFA_CODE;
        AuthCodeDeliveryDetails details = new AuthCodeDeliveryDetails(RandomString.string(), DeliveryMedium.PHONE);
        AuthNextSignInStep nextStep = new AuthNextSignInStep(step, Collections.emptyMap(), details);
        AuthSignInResult result = new AuthSignInResult(false, nextStep);
        doAnswer(invocation -> {
            // 0 = provider, 1 = activity, 2 = result consumer, 3 = failure consumer
            int positionOfResultConsumer = 2;
            Consumer<AuthSignInResult> onResult = invocation.getArgument(positionOfResultConsumer);
            onResult.accept(result);
            return null;
        }).when(delegate).signInWithSocialWebUI(eq(provider), eq(activity), anyConsumer(), anyConsumer());

        // Act: call the binding
        TestObserver<AuthSignInResult> observer = auth.signInWithSocialWebUI(provider, activity).test();

        // Assert: result is furnished the via the Rx Single
        observer.await(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        observer
            .assertNoErrors()
            .assertValue(result);
    }

    /**
     * Validates that a failed call to sign-in with social web UI will propagate the failure
     * back through the binding.
     * @throws InterruptedException If test observer is interrupted while awaiting terminal event
     */
    @Test
    public void testSignInWithSocialWebUIFails() throws InterruptedException {
        AuthProvider provider = AuthProvider.amazon();
        Activity activity = new Activity();

        // Arrange a failure
        AuthException failure = new AuthException("Sign in with social provider", " has failed");
        doAnswer(invocation -> {
            // 0 = provider, 1 = activity, 2 = result consumer, 3 = failure consumer
            int positionOfFailureConsumer = 3;
            Consumer<AuthException> onFailure = invocation.getArgument(positionOfFailureConsumer);
            onFailure.accept(failure);
            return null;
        }).when(delegate).signInWithSocialWebUI(eq(provider), eq(activity), anyConsumer(), anyConsumer());

        // Act: call the binding
        TestObserver<AuthSignInResult> observer = auth.signInWithSocialWebUI(provider, activity).test();

        // Assert: failure is furnished the via the Rx Single
        observer.await(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        observer
            .assertNoValues()
            .assertError(failure);
    }

    /**
     * Validates that a successful call to sign-in with web UI will propagate the result
     * back through the binding.
     * @throws InterruptedException If test observer is interrupted while awaiting terminal event
     */
    @Test
    public void testSignInWithWebUISucceeds() throws InterruptedException {
        Activity activity = new Activity();

        // Arrange a result
        AuthSignInStep step = AuthSignInStep.CONFIRM_SIGN_IN_WITH_CUSTOM_CHALLENGE;
        AuthCodeDeliveryDetails details = new AuthCodeDeliveryDetails(RandomString.string(), DeliveryMedium.PHONE);
        AuthNextSignInStep nextStep = new AuthNextSignInStep(step, Collections.emptyMap(), details);
        AuthSignInResult result = new AuthSignInResult(false, nextStep);
        doAnswer(invocation -> {
            // 0 = activity, 1 = result consumer, 2 = failure consumer
            int positionOfResultConsumer = 1;
            Consumer<AuthSignInResult> onResult = invocation.getArgument(positionOfResultConsumer);
            onResult.accept(result);
            return null;
        }).when(delegate).signInWithWebUI(eq(activity), anyConsumer(), anyConsumer());

        // Act: call the binding
        TestObserver<AuthSignInResult> observer = auth.signInWithWebUI(activity).test();

        // Assert: result is furnished the via the Rx Single
        observer.await(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        observer
            .assertNoErrors()
            .assertValue(result);
    }

    /**
     * Validates that a failed call to sign-in with web UI will propagate the failure
     * back through the binding.
     * @throws InterruptedException If test observer is interrupted while awaiting terminal event
     */
    @Test
    public void testSignInWithWebUIFails() throws InterruptedException {
        Activity activity = new Activity();

        // Arrange a failure
        AuthException failure = new AuthException("Sign in with web UI", " has failed");
        doAnswer(invocation -> {
            // 0 = activity, 1 = result consumer, 2 = failure consumer
            int positionOfFailureConsumer = 2;
            Consumer<AuthException> onFailure = invocation.getArgument(positionOfFailureConsumer);
            onFailure.accept(failure);
            return null;
        }).when(delegate).signInWithWebUI(eq(activity), anyConsumer(), anyConsumer());

        // Act: call the binding
        TestObserver<AuthSignInResult> observer = auth.signInWithWebUI(activity).test();

        // Assert: failure is furnished the via the Rx Single
        observer.await(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        observer
            .assertNoValues()
            .assertError(failure);
    }

    /**
     * Validates that a request to handle a web UI sign-in response
     * passes the response down into the delegate.
     */
    @Test
    public void testHandleWebUISignInResponse() {
        Intent intent = new Intent();
        auth.handleWebUISignInResponse(intent);
        verify(delegate).handleWebUISignInResponse(eq(intent));
    }

    /**
     * Tests that a successful call to fetch the auth session will propagate the session object
     * back up through the binding.
     * @throws InterruptedException If test observer is interrupted while awaiting terminal event
     */
    @Test
    public void testFetchAuthSessionSucceeds() throws InterruptedException {
        // Arrange an auth session object to return when delegate is called
        AuthSession expected = new AuthSession(false);
        doAnswer(invocation -> {
            // 0 = onResult, 1 = onFailure
            int positionOfResultConsumer = 0;
            Consumer<AuthSession> onResult = invocation.getArgument(positionOfResultConsumer);
            onResult.accept(expected);
            return null;
        }).when(delegate).fetchAuthSession(anyConsumer(), anyConsumer());

        // Act: call the Rx binding
        TestObserver<AuthSession> observer = auth.fetchAuthSession().test();

        // Assert: AuthSession is furnished to the Rx Single.
        observer.await(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        observer
            .assertNoErrors()
            .assertValue(expected);
    }

    /**
     * Tests that a failed call to fetch the auth session will propagate the failure
     * back up through the binding.
     * @throws InterruptedException If test observer is interrupted while awaiting terminal event
     */
    @Test
    public void testFetchAuthSessionFails() throws InterruptedException {
        // Arrange a failure when the delegate is called
        AuthException failure = new AuthException("Fetch session", " has failed.");
        doAnswer(invocation -> {
            // 0 = onResult, 1 = onFailure
            int positionOfFailureConsumer = 1;
            Consumer<AuthException> onFailure = invocation.getArgument(positionOfFailureConsumer);
            onFailure.accept(failure);
            return null;
        }).when(delegate).fetchAuthSession(anyConsumer(), anyConsumer());

        // Act: call the Rx binding
        TestObserver<AuthSession> observer = auth.fetchAuthSession().test();

        // Assert: AuthException is furnished to the Rx Single.
        observer.await(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        observer
            .assertNoValues()
            .assertError(failure);
    }

    /**
     * Tests that a successful request to reset the password will propagate a result
     * back through the binding.
     * @throws InterruptedException If test observer is interrupted while awaiting terminal event
     */
    @Test
    public void testResetPasswordSucceeds() throws InterruptedException {
        String username = RandomString.string();

        // Arrange delegate to furnish a result
        AuthResetPasswordStep step = AuthResetPasswordStep.CONFIRM_RESET_PASSWORD_WITH_CODE;
        AuthCodeDeliveryDetails details = new AuthCodeDeliveryDetails(RandomString.string(), DeliveryMedium.PHONE);
        AuthNextResetPasswordStep nextStep = new AuthNextResetPasswordStep(step, Collections.emptyMap(), details);
        AuthResetPasswordResult expected = new AuthResetPasswordResult(true, nextStep);
        doAnswer(invocation -> {
            // 0 = username, 1 = onResult, 2 = onFailure
            int positionOfResultConsumer = 1;
            Consumer<AuthResetPasswordResult> onResult = invocation.getArgument(positionOfResultConsumer);
            onResult.accept(expected);
            return null;
        }).when(delegate).resetPassword(eq(username), anyConsumer(), anyConsumer());

        // Act: call the binding
        TestObserver<AuthResetPasswordResult> observer = auth.resetPassword(username).test();

        // Assert: result was furnished via Rx Single
        observer.await(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        observer
            .assertNoErrors()
            .assertValue(expected);
    }

    /**
     * Tests that a failed request to reset the password will propagate a failure
     * back through the binding.
     * @throws InterruptedException If test observer is interrupted while awaiting terminal event
     */
    @Test
    public void testResetPasswordFails() throws InterruptedException {
        String username = RandomString.string();

        // Arrange delegate to furnish a failure
        AuthException failure = new AuthException("Reset password", " has failed.");
        doAnswer(invocation -> {
            // 0 = username, 1 = onResult, 2 = onFailure
            int positionOfFailureConsumer = 2;
            Consumer<AuthException> onFailure = invocation.getArgument(positionOfFailureConsumer);
            onFailure.accept(failure);
            return null;
        }).when(delegate).resetPassword(eq(username), anyConsumer(), anyConsumer());

        // Act: call the binding
        TestObserver<AuthResetPasswordResult> observer = auth.resetPassword(username).test();

        // Assert: failure was furnished via Rx Single
        observer.await(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        observer
            .assertNoValues()
            .assertError(failure);
    }

    /**
     * Tests that a successful request to confirm password reset will propagate a completion
     * back through the binding.
     * @throws InterruptedException If test observer is interrupted while awaiting terminal event
     */
    @Test
    public void testConfirmResetPasswordSucceeds() throws InterruptedException {
        String newPassword = RandomString.string();
        String confirmationCode = RandomString.string();

        // Arrange completion callback to be invoked
        doAnswer(invocation -> {
            // 0 = new pass, 1 = confirmation code, 2 = onComplete, 3 = onFailure
            int positionOfCompletionAction = 2;
            Action onComplete = invocation.getArgument(positionOfCompletionAction);
            onComplete.call();
            return null;
        }).when(delegate).confirmResetPassword(eq(newPassword), eq(confirmationCode), anyAction(), anyConsumer());

        // Act: call the binding
        TestObserver<Void> observer =
            auth.confirmResetPassword(newPassword, confirmationCode).test();

        // Assert: Completable was completed successfully
        observer.await(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        observer
            .assertNoErrors()
            .assertComplete();
    }

    /**
     * Tests that a failed request to confirm password reset will propagate a failure
     * back through the binding.
     * @throws InterruptedException If test observer is interrupted while awaiting terminal event
     */
    @Test
    public void testConfirmResetPasswordFails() throws InterruptedException {
        String newPassword = RandomString.string();
        String confirmationCode = RandomString.string();

        // Arrange delegate to furnish a failure
        AuthException failure = new AuthException("Confirm password reset ", " has failed.");
        doAnswer(invocation -> {
            // 0 = new pass, 1 = confirmation code, 2 = onComplete, 3 = onFailure
            int positionOfFailureConsumer = 3;
            Consumer<AuthException> onFailure = invocation.getArgument(positionOfFailureConsumer);
            onFailure.accept(failure);
            return null;
        }).when(delegate).confirmResetPassword(eq(newPassword), eq(confirmationCode), anyAction(), anyConsumer());

        // Act: call the binding
        TestObserver<Void> observer =
            auth.confirmResetPassword(newPassword, confirmationCode).test();

        // Assert: Completable terminated with failure
        observer.await(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        observer
            .assertNotComplete()
            .assertError(failure);
    }

    /**
     * Tests that a successful request to update a user's password will propagate a completion
     * back through the binding.
     * @throws InterruptedException If test observer is interrupted while awaiting terminal event
     */
    @Test
    public void testUpdatePasswordSucceeds() throws InterruptedException {
        String oldPassword = RandomString.string();
        String newPassword = RandomString.string();

        // Arrange an invocation of the success Action
        doAnswer(invocation -> {
            // 0 = old pass, 1 = new pass, 2 = onComplete, 3 = onFailure
            int positionOfCompletionAction = 2;
            Action onCompletion = invocation.getArgument(positionOfCompletionAction);
            onCompletion.call();
            return null;
        }).when(delegate).updatePassword(eq(oldPassword), eq(newPassword), anyAction(), anyConsumer());

        // Act: call the binding
        TestObserver<Void> observer = auth.updatePassword(oldPassword, newPassword).test();

        // Assert: Completable completes with success
        observer.await(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        observer
            .assertNoErrors()
            .assertComplete();
    }

    /**
     * Tests that a failed request to update a user's password will propagate a failure
     * back through the binding.
     * @throws InterruptedException If test observer is interrupted while awaiting terminal event
     */
    @Test
    public void testUpdatePasswordFails() throws InterruptedException {
        String oldPassword = RandomString.string();
        String newPassword = RandomString.string();

        // Arrange a callback on the failure consumer
        AuthException failure = new AuthException("Update password ", "has failed");
        doAnswer(invocation -> {
            // 0 = old pass, 1 = new pass, 2 = onComplete, 3 = onFailure
            int positionOfFailureConsumer = 3;
            Consumer<AuthException> onFailure = invocation.getArgument(positionOfFailureConsumer);
            onFailure.accept(failure);
            return null;
        }).when(delegate).updatePassword(eq(oldPassword), eq(newPassword), anyAction(), anyConsumer());

        // Act: call the binding
        TestObserver<Void> observer = auth.updatePassword(oldPassword, newPassword).test();

        // Assert: Completable terminates with failure
        observer.await(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        observer
            .assertNotComplete()
            .assertError(failure);
    }

    /**
     * Tests that a successful request to remember current auth device will propagate a completion
     * back through the binding.
     * @throws InterruptedException If test observer is interrupted while awaiting terminal event
     */
    @Test
    public void testRememberDevice() throws InterruptedException {
        // Arrange an invocation of the success Action
        doAnswer(invocation -> {
            // 0 = onComplete, 1 = onFailure
            Action onCompletion = invocation.getArgument(0);
            onCompletion.call();
            return null;
        }).when(delegate).rememberDevice(anyAction(), anyConsumer());

        // Act: call the binding
        TestObserver<Void> observer = auth.rememberDevice().test();

        // Assert: Completable completes with success
        observer.await(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        observer.assertNoErrors()
                .assertComplete();
    }

    /**
     * Tests that a successful request to forget current auth device will propagate a completion
     * back through the binding.
     * @throws InterruptedException If test observer is interrupted while awaiting terminal event
     */
    @Test
    public void testForgetCurrentDevice() throws InterruptedException {
        // Arrange an invocation of the success Action
        doAnswer(invocation -> {
            // 0 = onComplete, 1 = onFailure
            Action onCompletion = invocation.getArgument(0);
            onCompletion.call();
            return null;
        }).when(delegate).forgetDevice(anyAction(), anyConsumer());

        // Act: call the binding
        TestObserver<Void> observer = auth.forgetDevice().test();

        // Assert: Completable completes with success
        observer.await(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        observer.assertNoErrors()
                .assertComplete();
    }

    /**
     * Tests that a successful request to forget a specific auth device will propagate a completion
     * back through the binding.
     * @throws InterruptedException If test observer is interrupted while awaiting terminal event
     */
    @Test
    public void testForgetSpecificDevice() throws InterruptedException {
        // Arrange an invocation of the success Action
        doAnswer(invocation -> {
            // 0 = deviceToForget, 1 = onComplete, 2 = onFailure
            Action onCompletion = invocation.getArgument(1);
            onCompletion.call();
            return null;
        }).when(delegate).forgetDevice(any(), anyAction(), anyConsumer());

        // Act: call the binding
        AuthDevice deviceToForget = AuthDevice.fromId(RandomString.string());
        TestObserver<Void> observer = auth.forgetDevice(deviceToForget).test();

        // Assert: Completable completes with success
        observer.await(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        observer.assertNoErrors()
                .assertComplete();
    }

    /**
     * Tests that a successful request to fetch remembered auth devices will propagate a completion
     * back through the binding.
     * @throws InterruptedException If test observer is interrupted while awaiting terminal event
     */
    @Test
    public void testFetchDevices() throws InterruptedException {
        // Arrange delegate to furnish a result
        AuthDevice device = AuthDevice.fromId(RandomString.string());
        List<AuthDevice> expected = Collections.singletonList(device);
        doAnswer(invocation -> {
            // 0 = onComplete, 1 = onFailure
            Consumer<List<AuthDevice>> onCompletion = invocation.getArgument(0);
            onCompletion.accept(expected);
            return null;
        }).when(delegate).fetchDevices(anyConsumer(), anyConsumer());

        // Act: call the binding
        TestObserver<List<AuthDevice>> observer = auth.fetchDevices().test();

        // Assert: result was furnished via Rx Single
        observer.await(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        observer.assertNoErrors()
                .assertValue(expected);
    }

    /**
     * Tests that a successful request to fetch user attributes will propagate a completion
     * back through the binding.
     * @throws InterruptedException  If test observer is interrupted while awaiting terminal event
     */
    @Test
    public void testFetchUserAttributes() throws InterruptedException {
        // Arrange an invocation of the success Action
        List<AuthUserAttribute> expected = Collections.singletonList(
                new AuthUserAttribute(AuthUserAttributeKey.custom(ATTRIBUTE_KEY), ATTRIBUTE_VAL)
        );

        doAnswer(invocation -> {
            // 0 = onComplete, 1 = onFailure
            Consumer<List<AuthUserAttribute>> onCompletion = invocation.getArgument(0);
            onCompletion.accept(expected);
            return null;
        }).when(delegate)
                .fetchUserAttributes(anyConsumer(), anyConsumer());

        // Act: call the binding
        TestObserver<List<AuthUserAttribute>> observer = auth.fetchUserAttributes().test();

        // Assert: Completable completes with success
        observer.await(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        observer.assertNoErrors()
                .assertValue(expected);
    }

    /**
     * Tests that a successful request to update a user attribute will propagate a completion
     * back through the binding.
     * @throws InterruptedException If test observer is interrupted while awaiting terminal event
     */
    @Test
    public void testUpdateUserAttribute() throws InterruptedException {
        // Arrange an invocation of the success Action
        AuthUserAttribute attribute = new AuthUserAttribute(AuthUserAttributeKey.custom(ATTRIBUTE_KEY), ATTRIBUTE_VAL);
        AuthUpdateAttributeResult expected = new AuthUpdateAttributeResult(
                true,
                new AuthNextUpdateAttributeStep(
                        AuthUpdateAttributeStep.DONE,
                        Collections.emptyMap(),
                        null
                )
        );

        doAnswer(invocation -> {
            Consumer<AuthUpdateAttributeResult> onCompletion = invocation.getArgument(1);
            onCompletion.accept(expected);
            return null;
        }).when(delegate)
                .updateUserAttribute(any(), anyConsumer(), anyConsumer());

        // Act: call the binding
        TestObserver<AuthUpdateAttributeResult> observer = auth.updateUserAttribute(attribute).test();

        // Assert: Completable completes with success
        observer.await(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        observer.assertNoErrors()
                .assertValue(expected);
    }

    /**
     * Tests that a successful request to update user attributes will propagate a completion
     * back through the binding.
     * @throws InterruptedException If test observer is interrupted while awaiting terminal event
     */
    @Test
    public void testUpdateUserAttributes() throws InterruptedException {
        // Arrange an invocation of the success Action
        List<AuthUserAttribute> attributes = new ArrayList<>();
        AuthUserAttributeKey attributeKey = AuthUserAttributeKey.custom(ATTRIBUTE_KEY);
        AuthUserAttributeKey attributeKeyWithoutCode = AuthUserAttributeKey.custom(ATTRIBUTE_KEY_WITHOUT_CODE_DELIVERY);
        attributes.add(new AuthUserAttribute(attributeKey, ATTRIBUTE_VAL));
        attributes.add(new AuthUserAttribute(attributeKeyWithoutCode, ATTRIBUTE_VAL_WITHOUT_CODE_DELIVERY));

        Map<AuthUserAttributeKey, AuthUpdateAttributeResult> attributeResultMap = new HashMap<>();
        attributeResultMap.put(attributeKey, new AuthUpdateAttributeResult(
                true,
                new AuthNextUpdateAttributeStep(
                        AuthUpdateAttributeStep.CONFIRM_ATTRIBUTE_WITH_CODE,
                        Collections.emptyMap(),
                        new AuthCodeDeliveryDetails(
                                DESTINATION,
                                DeliveryMedium.EMAIL,
                                ATTRIBUTE_NAME
                        )
                )
        ));
        attributeResultMap.put(attributeKeyWithoutCode, new AuthUpdateAttributeResult(
                true,
                new AuthNextUpdateAttributeStep(
                        AuthUpdateAttributeStep.DONE,
                        Collections.emptyMap(),
                        null)
        ));

        doAnswer(invocation -> {
            Consumer<Map<AuthUserAttributeKey, AuthUpdateAttributeResult>> onCompletion =
                    invocation.getArgument(1);
            onCompletion.accept(attributeResultMap);
            return null;
        }).when(delegate)
                .updateUserAttributes(any(), anyConsumer(), anyConsumer());

        // Act: call the binding
        TestObserver<Map<AuthUserAttributeKey, AuthUpdateAttributeResult>> observer =
                auth.updateUserAttributes(attributes).test();

        // Assert: Completable completes with success
        observer.await(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        observer.assertNoErrors()
                .assertValue(attributeResultMap);
    }

    /**
     * Tests that a successful request to resend user attribute confirmation code will propagate a
     * completion back through the binding.
     * @throws InterruptedException If test observer is interrupted while awaiting terminal event
     */
    @Test
    public void testResendUserAttributeConfirmationCode() throws InterruptedException {
        // Arrange an invocation of the success Action
        AuthUserAttributeKey attributeKey = AuthUserAttributeKey.custom(ATTRIBUTE_KEY);
        AuthCodeDeliveryDetails expected = new AuthCodeDeliveryDetails(
                DESTINATION,
                DeliveryMedium.EMAIL,
                ATTRIBUTE_NAME
        );

        doAnswer(invocation -> {
            Consumer<AuthCodeDeliveryDetails> onCompletion = invocation.getArgument(1);
            onCompletion.accept(expected);
            return null;
        }).when(delegate)
                .resendUserAttributeConfirmationCode(any(), anyConsumer(), anyConsumer());

        // Act: call the binding
        TestObserver<AuthCodeDeliveryDetails> observer = auth.resendUserAttributeConfirmationCode(attributeKey).test();

        // Assert: Completable completes with success
        observer.await(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        observer.assertNoErrors()
                .assertValue(expected);
    }

    /**
     * Validates that a successful request to confirm user attribute will propagate up into the binding.
     * @throws InterruptedException If test observer is interrupted while awaiting terminal event
     */
    @Test
    public void testConfirmUserAttribute() throws InterruptedException {
        // Arrange an invocation of the success Action
        AuthUserAttributeKey attributeKey = AuthUserAttributeKey.custom(ATTRIBUTE_KEY);

        doAnswer(invocation -> {
            Action onComplete = invocation.getArgument(2);
            onComplete.call();
            return null;
        }).when(delegate)
                .confirmUserAttribute(any(), any(), anyAction(), anyConsumer());

        // Act: call the binding
        TestObserver<Void> observer = auth.confirmUserAttribute(attributeKey, CONFIRMATION_CODE).test();

        // Assert: Completable completes successfully
        observer.await(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        observer.assertNoErrors()
                .assertComplete();
    }

    /**
     * Getting the current user should just pass through to the delegate, to return whatever
     * it would.
     * @throws InterruptedException If test observer is interrupted while awaiting terminal event
     */
    @Test
    public void testGetCurrentUser() throws InterruptedException {
        AuthUser authUser = new AuthUser("testUserId", "testUsername");
        doAnswer(invocation -> {
            // 0 = onComplete, 1 = onFailure
            int positionOfCompletionAction = 0;
            Consumer<AuthUser> onResult = invocation.getArgument(positionOfCompletionAction);
            onResult.accept(authUser);
            return null;
        }).when(delegate).getCurrentUser(anyConsumer(), anyConsumer());

        // Act: call the binding
        TestObserver<AuthUser> observer = auth.getCurrentUser().test();

        // Assert: Completable completes successfully
        observer.await(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        observer.assertNoErrors()
                .assertComplete();
    }

    /**
     * Validates that a successful sign-out will propagate up into the binding.
     * @throws InterruptedException If test observer is interrupted while awaiting terminal event
     */
    @Test
    public void testSignOutSucceeds() throws InterruptedException {
        // Arrange an invocation of the success action
        doAnswer(invocation -> {
            // 0 = onComplete, 1 = onFailure
            int positionOfCompletionAction = 0;
            Action onComplete = invocation.getArgument(positionOfCompletionAction);
            onComplete.call();
            return null;
        }).when(delegate).signOut(anyAction(), anyConsumer());

        // Act: call the binding
        TestObserver<Void> observer = auth.signOut().test();

        // Assert: Completable completes successfully
        observer.await(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        observer.assertNoErrors()
                .assertComplete();
    }

    /**
     * Validate that a sign-out failure is propagated up through the binding.
     * @throws InterruptedException If test observer is interrupted while awaiting terminal event
     */
    @Test
    public void testSignOutFails() throws InterruptedException {
        // Arrange a callback on the failure consumer
        AuthException failure = new AuthException("Sign out", "has failed");
        doAnswer(invocation -> {
            // 0 = onComplete, 1 = onFailure
            int positionOfFailureConsumer = 1;
            Consumer<AuthException> onFailure = invocation.getArgument(positionOfFailureConsumer);
            onFailure.accept(failure);
            return null;
        }).when(delegate).signOut(anyAction(), anyConsumer());

        // Act: call the binding
        TestObserver<Void> observer = auth.signOut().test();

        // Assert: failure is furnished via Rx Completable.
        observer.await(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        observer.assertNotComplete()
                .assertError(failure);
    }

    /**
     * Tests that a successful request to delete the currently signed in user will
     * propagate a completion back through the binding.
     * @throws InterruptedException If test observer is interrupted while awaiting terminal event
     */
    @Test
    public void testDeleteUser() throws InterruptedException {
        // Arrange an invocation of the success Action
        doAnswer(invocation -> {
            // 0 = onComplete, 1 = onFailure
            Action onCompletion = invocation.getArgument(0);
            onCompletion.call();
            return null;
        }).when(delegate).deleteUser(anyAction(), anyConsumer());
        
        // Act: call the binding
        TestObserver<Void> observer = auth.deleteUser().test();
        
        // Assert: Completable completes with success
        observer.await(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        observer.assertNoErrors()
                .assertComplete();
    }

    /**
     * Validate that a delete user failure is propagated up through the binding.
     * @throws InterruptedException If test observer is interrupted while awaiting terminal event
     */
    @Test
    public void testDeleteUserFails() throws InterruptedException {
        // Arrange a callback on the failure consumer
        AuthException failure = new AuthException("Delete user", "has failed");
        doAnswer(invocation -> {
            // 0 = onComplete, 1 = onFailure
            int positionOfFailureConsumer = 1;
            Consumer<AuthException> onFailure = invocation.getArgument(positionOfFailureConsumer);
            onFailure.accept(failure);
            return null;
        }).when(delegate).deleteUser(anyAction(), anyConsumer());
        
        // Act: call the binding
        TestObserver<Void> observer = auth.deleteUser().test();
        
        // Assert: failure is furnished via Rx Completable.
        observer.await(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        observer.assertNotComplete()
                .assertError(failure);
    }
}
