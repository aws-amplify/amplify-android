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
import com.amplifyframework.auth.AuthException;
import com.amplifyframework.auth.AuthProvider;
import com.amplifyframework.auth.AuthSession;
import com.amplifyframework.auth.AuthUser;
import com.amplifyframework.auth.options.AuthSignUpOptions;
import com.amplifyframework.auth.result.AuthResetPasswordResult;
import com.amplifyframework.auth.result.AuthSignInResult;
import com.amplifyframework.auth.result.AuthSignUpResult;
import com.amplifyframework.auth.result.step.AuthNextResetPasswordStep;
import com.amplifyframework.auth.result.step.AuthNextSignInStep;
import com.amplifyframework.auth.result.step.AuthNextSignUpStep;
import com.amplifyframework.auth.result.step.AuthResetPasswordStep;
import com.amplifyframework.auth.result.step.AuthSignInStep;
import com.amplifyframework.auth.result.step.AuthSignUpStep;
import com.amplifyframework.core.Action;
import com.amplifyframework.core.Consumer;
import com.amplifyframework.testutils.random.RandomString;

import org.junit.Before;
import org.junit.Test;

import java.util.Collections;

import io.reactivex.observers.TestObserver;

import static com.amplifyframework.rx.Matchers.anyAction;
import static com.amplifyframework.rx.Matchers.anyConsumer;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests the {@link RxAuthBinding}.
 */
public final class RxAuthBindingTest {
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
     */
    @Test
    public void testSignUpSucceeds() {
        // Arrange a response from delegate
        String userId = RandomString.string();
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
            return (Void) null;
        }).when(delegate).signUp(eq(username), eq(password), eq(options), anyConsumer(), anyConsumer());

        // Act: call the binding
        TestObserver<AuthSignUpResult> observer = auth.signUp(username, password, options).test();

        // Assert: the result was furnished to the Rx Single
        observer.awaitTerminalEvent();
        observer
            .assertNoErrors()
            .assertValue(result);
    }

    /**
     * Validates that a sign-up failure are passed through the binding.
     */
    @Test
    public void testSignUpFails() {
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
            return (Void) null;
        }).when(delegate).signUp(eq(username), eq(password), eq(options), anyConsumer(), anyConsumer());

        // Act: call the binding
        TestObserver<AuthSignUpResult> observer = auth.signUp(username, password, options).test();

        // Assert: error is furnished via Rx single.
        observer.awaitTerminalEvent();
        observer
            .assertNoValues()
            .assertError(failure);
    }

    /**
     * Validates that a successful call to resend the sign-up code will propagate the result
     * back through the binding.
     */
    @Test
    public void testResendSignUpCodeSucceeds() {
        String userId = RandomString.string();
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
            return (Void) null;
        }).when(delegate).resendSignUpCode(eq(username), anyConsumer(), anyConsumer());

        // Act: call the binding
        TestObserver<AuthSignUpResult> observer = auth.resendSignUpCode(username).test();

        // Assert: the result was furnished to the Rx Single
        observer.awaitTerminalEvent();
        observer
            .assertNoErrors()
            .assertValue(result);
    }

    /**
     * Validates that a failed call to resend the sign-up code will propagate the failure
     * back through the binding.
     */
    @Test
    public void testResendSignUpCodeFails() {
        String username = RandomString.string();

        // Arrange a failure on the failure consumer
        AuthException failure = new AuthException("Reset sign up", " has failed.");
        doAnswer(invocation -> {
            // 0 = username, 1 = onResult, 2 = onFailure
            int positionOfFailureConsumer = 2;
            Consumer<AuthException> onFailure = invocation.getArgument(positionOfFailureConsumer);
            onFailure.accept(failure);
            return (Void) null;
        }).when(delegate).resendSignUpCode(eq(username), anyConsumer(), anyConsumer());

        // Act: call the binding
        TestObserver<AuthSignUpResult> observer = auth.resendSignUpCode(username).test();

        // Assert: the result was furnished to the Rx Single
        observer.awaitTerminalEvent();
        observer
            .assertNoValues()
            .assertError(failure);
    }

    /**
     * Validates that a successful call to sign-in will propagate the result
     * back through the binding.
     */
    @Test
    public void testSignInSucceeds() {
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
            return (Void) null;
        }).when(delegate).signIn(eq(username), eq(password), anyConsumer(), anyConsumer());

        // Act: call the binding
        TestObserver<AuthSignInResult> observer = auth.signIn(username, password).test();

        // Assert: the result was furnished to the Rx Single
        observer.awaitTerminalEvent();
        observer
            .assertNoErrors()
            .assertValue(result);
    }

    /**
     * Validates that a failed call to sign-in will propagate the result
     * back through the binding.
     */
    @Test
    public void testSignInFails() {
        String username = RandomString.string();
        String password = RandomString.string();

        // Arrange a failure on the failure consumer
        AuthException failure = new AuthException("Sign in", " has failed.");
        doAnswer(invocation -> {
            // 0 = username, 1 = password, 2 = onResult, 3 = onFailure
            int positionOfFailureConsumer = 3;
            Consumer<AuthException> onFailure = invocation.getArgument(positionOfFailureConsumer);
            onFailure.accept(failure);
            return (Void) null;
        }).when(delegate).signIn(eq(username), eq(password), anyConsumer(), anyConsumer());

        // Act: call the binding
        TestObserver<AuthSignInResult> observer = auth.signIn(username, password).test();

        // Assert: the failure was furnished to the Rx Single
        observer.awaitTerminalEvent();
        observer
            .assertNoValues()
            .assertError(failure);
    }

    /**
     * Validates that a successful call to confirm sign-in will propagate the result
     * back through the binding.
     */
    @Test
    public void testConfirmSignInSucceeds() {
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
            return (Void) null;
        }).when(delegate).confirmSignIn(eq(confirmationCode), anyConsumer(), anyConsumer());

        // Act: call the binding
        TestObserver<AuthSignInResult> observer = auth.confirmSignIn(confirmationCode).test();

        // Assert: result is furnished
        observer.awaitTerminalEvent();
        observer
            .assertNoErrors()
            .assertValue(expected);
    }

    /**
     * Validates that a failed call to confirm sign-in will propagate the failure
     * back through the binding.
     */
    @Test
    public void testConfirmSignInFails() {
        String confirmationCode = RandomString.string();

        // Arrange a failure.
        AuthException failure = new AuthException("Confirmation of sign in", " has failed.");
        doAnswer(invocation -> {
            // 0 = confirm code, 1 = onResult, 2 = onFailure
            int positionOfFailureConsumer = 2;
            Consumer<AuthException> onResult = invocation.getArgument(positionOfFailureConsumer);
            onResult.accept(failure);
            return (Void) null;
        }).when(delegate).confirmSignIn(eq(confirmationCode), anyConsumer(), anyConsumer());

        // Act: call the binding
        TestObserver<AuthSignInResult> observer = auth.confirmSignIn(confirmationCode).test();

        // Assert: failure is furnished
        observer.awaitTerminalEvent();
        observer
            .assertNoValues()
            .assertError(failure);
    }

    /**
     * Validates that a successful call to sign-in with social web UI will propagate the result
     * back through the binding.
     */
    @Test
    public void testSignInWithSocialWebUISucceeds() {
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
            return (Void) null;
        }).when(delegate).signInWithSocialWebUI(eq(provider), eq(activity), anyConsumer(), anyConsumer());

        // Act: call the binding
        TestObserver<AuthSignInResult> observer = auth.signInWithSocialWebUI(provider, activity).test();

        // Assert: result is furnished the via the Rx Single
        observer.awaitTerminalEvent();
        observer
            .assertNoErrors()
            .assertValue(result);
    }

    /**
     * Validates that a failed call to sign-in with social web UI will propagate the failure
     * back through the binding.
     */
    @Test
    public void testSignInWithSocialWebUIFails() {
        AuthProvider provider = AuthProvider.amazon();
        Activity activity = new Activity();

        // Arrange a failure
        AuthException failure = new AuthException("Sign in with social provider", " has failed");
        doAnswer(invocation -> {
            // 0 = provider, 1 = activity, 2 = result consumer, 3 = failure consumer
            int positionOfFailureConsumer = 3;
            Consumer<AuthException> onFailure = invocation.getArgument(positionOfFailureConsumer);
            onFailure.accept(failure);
            return (Void) null;
        }).when(delegate).signInWithSocialWebUI(eq(provider), eq(activity), anyConsumer(), anyConsumer());

        // Act: call the binding
        TestObserver<AuthSignInResult> observer = auth.signInWithSocialWebUI(provider, activity).test();

        // Assert: failure is furnished the via the Rx Single
        observer.awaitTerminalEvent();
        observer
            .assertNoValues()
            .assertError(failure);
    }

    /**
     * Validates that a successful call to sign-in with web UI will propagate the result
     * back through the binding.
     */
    @Test
    public void testSignInWithWebUISucceeds() {
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
            return (Void) null;
        }).when(delegate).signInWithWebUI(eq(activity), anyConsumer(), anyConsumer());

        // Act: call the binding
        TestObserver<AuthSignInResult> observer = auth.signInWithWebUI(activity).test();

        // Assert: result is furnished the via the Rx Single
        observer.awaitTerminalEvent();
        observer
            .assertNoErrors()
            .assertValue(result);
    }

    /**
     * Validates that a failed call to sign-in with web UI will propagate the failure
     * back through the binding.
     */
    @Test
    public void testSignInWithWebUIFails() {
        Activity activity = new Activity();

        // Arrange a failure
        AuthException failure = new AuthException("Sign in with web UI", " has failed");
        doAnswer(invocation -> {
            // 0 = activity, 1 = result consumer, 2 = failure consumer
            int positionOfFailureConsumer = 2;
            Consumer<AuthException> onFailure = invocation.getArgument(positionOfFailureConsumer);
            onFailure.accept(failure);
            return (Void) null;
        }).when(delegate).signInWithWebUI(eq(activity), anyConsumer(), anyConsumer());

        // Act: call the binding
        TestObserver<AuthSignInResult> observer = auth.signInWithWebUI(activity).test();

        // Assert: failure is furnished the via the Rx Single
        observer.awaitTerminalEvent();
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
     */
    @Test
    public void testFetchAuthSessionSucceeds() {
        // Arrange an auth session object to return when delegate is called
        AuthSession expected = new AuthSession(false);
        doAnswer(invocation -> {
            // 0 = onResult, 1 = onFailure
            int positionOfResultConsumer = 0;
            Consumer<AuthSession> onResult = invocation.getArgument(positionOfResultConsumer);
            onResult.accept(expected);
            return (Void) null;
        }).when(delegate).fetchAuthSession(anyConsumer(), anyConsumer());

        // Act: call the Rx binding
        TestObserver<AuthSession> observer = auth.fetchAuthSession().test();

        // Assert: AuthSession is furnished to the Rx Single.
        observer.awaitTerminalEvent();
        observer
            .assertNoErrors()
            .assertValue(expected);
    }

    /**
     * Tests that a failed call to fetch the auth session will propagate the failure
     * back up through the binding.
     */
    @Test
    public void testFetchAuthSessionFails() {
        // Arrange a failure when the delegate is called
        AuthException failure = new AuthException("Fetch session", " has failed.");
        doAnswer(invocation -> {
            // 0 = onResult, 1 = onFailure
            int positionOfFailureConsumer = 1;
            Consumer<AuthException> onFailure = invocation.getArgument(positionOfFailureConsumer);
            onFailure.accept(failure);
            return (Void) null;
        }).when(delegate).fetchAuthSession(anyConsumer(), anyConsumer());

        // Act: call the Rx binding
        TestObserver<AuthSession> observer = auth.fetchAuthSession().test();

        // Assert: AuthException is furnished to the Rx Single.
        observer.awaitTerminalEvent();
        observer
            .assertNoValues()
            .assertError(failure);
    }

    /**
     * Tests that a successful request to reset the password will propagate a result
     * back through the binding.
     */
    @Test
    public void testResetPasswordSucceeds() {
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
            return (Void) null;
        }).when(delegate).resetPassword(eq(username), anyConsumer(), anyConsumer());

        // Act: call the binding
        TestObserver<AuthResetPasswordResult> observer = auth.resetPassword(username).test();

        // Assert: result was furnished via Rx Single
        observer.awaitTerminalEvent();
        observer
            .assertNoErrors()
            .assertValue(expected);
    }

    /**
     * Tests that a failed request to reset the password will propagate a failure
     * back through the binding.
     */
    @Test
    public void testResetPasswordFails() {
        String username = RandomString.string();

        // Arrange delegate to furnish a failure
        AuthException failure = new AuthException("Reset password", " has failed.");
        doAnswer(invocation -> {
            // 0 = username, 1 = onResult, 2 = onFailure
            int positionOfFailureConsumer = 2;
            Consumer<AuthException> onFailure = invocation.getArgument(positionOfFailureConsumer);
            onFailure.accept(failure);
            return (Void) null;
        }).when(delegate).resetPassword(eq(username), anyConsumer(), anyConsumer());

        // Act: call the binding
        TestObserver<AuthResetPasswordResult> observer = auth.resetPassword(username).test();

        // Assert: failure was furnished via Rx Single
        observer.awaitTerminalEvent();
        observer
            .assertNoValues()
            .assertError(failure);
    }

    /**
     * Tests that a successful request to confirm password reset will propagate a completion
     * back through the binding.
     */
    @Test
    public void testConfirmResetPasswordSucceeds() {
        String newPassword = RandomString.string();
        String confirmationCode = RandomString.string();

        // Arrange completion callback to be invoked
        doAnswer(invocation -> {
            // 0 = new pass, 1 = confirmation code, 2 = onComplete, 3 = onFailure
            int positionOfCompletionAction = 2;
            Action onComplete = invocation.getArgument(positionOfCompletionAction);
            onComplete.call();
            return (Void) null;
        }).when(delegate).confirmResetPassword(eq(newPassword), eq(confirmationCode), anyAction(), anyConsumer());

        // Act: call the binding
        TestObserver<Void> observer =
            auth.confirmResetPassword(newPassword, confirmationCode).test();

        // Assert: Completable was completed successfully
        observer.awaitTerminalEvent();
        observer
            .assertNoErrors()
            .assertComplete();
    }

    /**
     * Tests that a failed request to confirm password reset will propagate a failure
     * back through the binding.
     */
    @Test
    public void testConfirmResetPasswordFails() {
        String newPassword = RandomString.string();
        String confirmationCode = RandomString.string();

        // Arrange delegate to furnish a failure
        AuthException failure = new AuthException("Confirm password reset ", " has failed.");
        doAnswer(invocation -> {
            // 0 = new pass, 1 = confirmation code, 2 = onComplete, 3 = onFailure
            int positionOfFailureConsumer = 3;
            Consumer<AuthException> onFailure = invocation.getArgument(positionOfFailureConsumer);
            onFailure.accept(failure);
            return (Void) null;
        }).when(delegate).confirmResetPassword(eq(newPassword), eq(confirmationCode), anyAction(), anyConsumer());

        // Act: call the binding
        TestObserver<Void> observer =
            auth.confirmResetPassword(newPassword, confirmationCode).test();

        // Assert: Completable terminated with failure
        observer.awaitTerminalEvent();
        observer
            .assertNotComplete()
            .assertError(failure);
    }

    /**
     * Tests that a successful request to update a user's password will propagate a completion
     * back through the binding.
     */
    @Test
    public void testUpdatePasswordSucceeds() {
        String oldPassword = RandomString.string();
        String newPassword = RandomString.string();

        // Arrange an invocation of the success Action
        doAnswer(invocation -> {
            // 0 = old pass, 1 = new pass, 2 = onComplete, 3 = onFailure
            int positionOfCompletionAction = 2;
            Action onCompletion = invocation.getArgument(positionOfCompletionAction);
            onCompletion.call();
            return (Void) null;
        }).when(delegate).updatePassword(eq(oldPassword), eq(newPassword), anyAction(), anyConsumer());

        // Act: call the binding
        TestObserver<Void> observer = auth.updatePassword(oldPassword, newPassword).test();

        // Assert: Completable completes with success
        observer.awaitTerminalEvent();
        observer
            .assertNoErrors()
            .assertComplete();
    }

    /**
     * Tests that a failed request to update a user's password will propagate a failure
     * back through the binding.
     */
    @Test
    public void testUpdatePasswordFails() {
        String oldPassword = RandomString.string();
        String newPassword = RandomString.string();

        // Arrange a callback on the failure consumer
        AuthException failure = new AuthException("Update password ", "has failed");
        doAnswer(invocation -> {
            // 0 = old pass, 1 = new pass, 2 = onComplete, 3 = onFailure
            int positionOfFailureConsumer = 3;
            Consumer<AuthException> onFailure = invocation.getArgument(positionOfFailureConsumer);
            onFailure.accept(failure);
            return (Void) null;
        }).when(delegate).updatePassword(eq(oldPassword), eq(newPassword), anyAction(), anyConsumer());

        // Act: call the binding
        TestObserver<Void> observer = auth.updatePassword(oldPassword, newPassword).test();

        // Assert: Completable terminates with failure
        observer.awaitTerminalEvent();
        observer
            .assertNotComplete()
            .assertError(failure);
    }

    /**
     * Getting the current user should just pass through to the delegate, to reutrn whatever
     * it would.
     */
    @Test
    public void testGetCurrentUser() {
        AuthUser expected = new AuthUser(RandomString.string(), RandomString.string());
        when(delegate.getCurrentUser()).thenReturn(expected);
        assertEquals(expected, auth.getCurrentUser());
    }

    /**
     * Validates that a successful sign-out will propagate up into the binding.
     */
    @Test
    public void testSignOutSucceeds() {
        // Arrange an invocation of the success action
        doAnswer(invocation -> {
            // 0 = onComplete, 1 = onFailure
            int positionOfCompletionAction = 0;
            Action onComplete = invocation.getArgument(positionOfCompletionAction);
            onComplete.call();
            return (Void) null;
        }).when(delegate).signOut(anyAction(), anyConsumer());

        // Act: call the binding
        TestObserver<Void> observer = auth.signOut().test();

        // Assert: Completable completes successfully
        observer.awaitTerminalEvent();
        observer
            .assertNoErrors()
            .assertComplete();
    }

    /**
     * Validate that a sign-out failure is propagated up through the binding.
     */
    @Test
    public void testSignOutFails() {
        // Arrange a callback on the failure consumer
        AuthException failure = new AuthException("Sign out", "has failed");
        doAnswer(invocation -> {
            // 0 = onComplete, 1 = onFailure
            int positionOfFailureConsumer = 1;
            Consumer<AuthException> onFailure = invocation.getArgument(positionOfFailureConsumer);
            onFailure.accept(failure);
            return (Void) null;
        }).when(delegate).signOut(anyAction(), anyConsumer());

        // Act: call the binding
        TestObserver<Void> observer = auth.signOut().test();

        // Assert: failure is furnished via Rx Completable.
        observer.awaitTerminalEvent();
        observer
            .assertNotComplete()
            .assertError(failure);
    }
}
