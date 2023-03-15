/*
 * Copyright 2023 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

import com.amplifyframework.analytics.UserProfile;
import com.amplifyframework.core.Action;
import com.amplifyframework.core.Consumer;
import com.amplifyframework.notifications.NotificationsCategoryBehavior;
import com.amplifyframework.notifications.pushnotifications.PushNotificationsException;
import com.amplifyframework.testutils.random.RandomString;

import org.junit.Before;
import org.junit.Test;

import java.util.concurrent.TimeUnit;

import io.reactivex.rxjava3.observers.TestObserver;

import static com.amplifyframework.AmplifyException.REPORT_BUG_TO_AWS_SUGGESTION;
import static com.amplifyframework.rx.Matchers.anyAction;
import static com.amplifyframework.rx.Matchers.anyConsumer;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;

/**
 * Tests the {@link RxNotificationsBinding}.
 */
public final class RxNotificationsBindingTest {
    private static final long TIMEOUT_SECONDS = 2;

    private NotificationsCategoryBehavior delegate;
    private RxNotificationsBinding notifications;

    /**
     * Creates an {@link RxNotificationsBinding} instance to test.
     * It is tested by arranging behaviors on its {@link NotificationsCategoryBehavior} delegate.
     */
    @Before
    public void setup() {
        this.delegate = mock(NotificationsCategoryBehavior.class);
        this.notifications = new RxNotificationsBinding(delegate);
    }

    /**
     * Tests that a successful request to identify a user will propagate a completion back through the binding.
     * @throws InterruptedException If test observer is interrupted while awaiting terminal event
     */
    @Test
    public void testIdentifyUser() throws InterruptedException {
        String userId = RandomString.string();

        // Arrange an invocation of the success Action
        doAnswer(invocation -> {
            // 0 = userId, 1 = onComplete, 2 = onFailure
            Action onCompletion = invocation.getArgument(1);
            onCompletion.call();
            return null;
        }).when(delegate).identifyUser(eq(userId), anyAction(), anyConsumer());

        // Act: call the binding
        TestObserver<Void> observer = notifications.identifyUser(userId).test();

        // Assert: Completable completes with success
        observer.await(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        observer.assertNoErrors().assertComplete();
    }

    /**
     * Tests that a successful request to identify a user will propagate a completion back through the binding.
     * @throws InterruptedException If test observer is interrupted while awaiting terminal event
     */
    @Test
    public void testIdentifyUserWithProfile() throws InterruptedException {
        String userId = RandomString.string();
        UserProfile profile = UserProfile.builder().name("test").build();

        // Arrange an invocation of the success Action
        doAnswer(invocation -> {
            // 0 = userId, 1 = profile, 2 = onComplete, 3 = onFailure
            Action onCompletion = invocation.getArgument(2);
            onCompletion.call();
            return null;
        }).when(delegate).identifyUser(eq(userId), eq(profile), anyAction(), anyConsumer());

        // Act: call the binding
        TestObserver<Void> observer = notifications.identifyUser(userId, profile).test();

        // Assert: Completable completes with success
        observer.await(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        observer.assertNoErrors().assertComplete();
    }

    /**
     * Identify user failure is propagated up through the binding.
     * @throws InterruptedException If test observer is interrupted while awaiting terminal event
     */
    @Test
    public void testIdentifyUserFails() throws InterruptedException {
        String userId = RandomString.string();

        // Arrange a callback on the failure consumer
        PushNotificationsException failure = new PushNotificationsException(
                "Failed to identify user with the service.",
                REPORT_BUG_TO_AWS_SUGGESTION,
                new Exception()
        );
        doAnswer(invocation -> {
            // 0 = userId, 1 = onComplete, 2 = onFailure
            int positionOfFailureConsumer = 2;
            Consumer<PushNotificationsException> onFailure = invocation.getArgument(positionOfFailureConsumer);
            onFailure.accept(failure);
            return null;
        }).when(delegate).identifyUser(eq(userId), anyAction(), anyConsumer());

        // Act: call the binding
        TestObserver<Void> observer = notifications.identifyUser(userId).test();

        // Assert: failure is furnished via Rx Completable.
        observer.await(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        observer.assertNotComplete().assertError(failure);
    }

    /**
     * Identify user failure is propagated up through the binding.
     * @throws InterruptedException If test observer is interrupted while awaiting terminal event
     */
    @Test
    public void testIdentifyUserWithProfileFails() throws InterruptedException {
        String userId = RandomString.string();
        UserProfile profile = UserProfile.builder().name("test").build();

        // Arrange a callback on the failure consumer
        PushNotificationsException failure = new PushNotificationsException(
                "Failed to identify user with the service.",
                REPORT_BUG_TO_AWS_SUGGESTION,
                new Exception()
        );
        doAnswer(invocation -> {
            // 0 = userId, 1 = profile, 2 = onComplete, 3 = onFailure
            int positionOfFailureConsumer = 3;
            Consumer<PushNotificationsException> onFailure = invocation.getArgument(positionOfFailureConsumer);
            onFailure.accept(failure);
            return null;
        }).when(delegate).identifyUser(eq(userId), eq(profile), anyAction(), anyConsumer());

        // Act: call the binding
        TestObserver<Void> observer = notifications.identifyUser(userId, profile).test();

        // Assert: failure is furnished via Rx Completable.
        observer.await(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        observer.assertNotComplete().assertError(failure);
    }
}
