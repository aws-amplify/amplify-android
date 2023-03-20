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
import com.amplifyframework.notifications.pushnotifications.NotificationContentProvider;
import com.amplifyframework.notifications.pushnotifications.NotificationPayload;
import com.amplifyframework.notifications.pushnotifications.PushNotificationResult;
import com.amplifyframework.notifications.pushnotifications.PushNotificationsCategoryBehavior;
import com.amplifyframework.notifications.pushnotifications.PushNotificationsException;
import com.amplifyframework.testutils.random.RandomString;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.Collections;
import java.util.concurrent.TimeUnit;

import io.reactivex.rxjava3.observers.TestObserver;

import static com.amplifyframework.AmplifyException.REPORT_BUG_TO_AWS_SUGGESTION;
import static com.amplifyframework.rx.Matchers.anyAction;
import static com.amplifyframework.rx.Matchers.anyConsumer;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

/**
 * Tests the {@link RxPushNotificationsBinding}.
 */
public final class RxPushNotificationsBindingTest {
    private static final long TIMEOUT_SECONDS = 2;

    private PushNotificationsCategoryBehavior delegate;
    private RxPushNotificationsBinding push;

    private final NotificationPayload payload = new NotificationPayload
        .Builder(new NotificationContentProvider.FCM(Collections.emptyMap())).build();

    /**
     * Creates an {@link RxPushNotificationsBinding} instance to test.
     * It is tested by arranging behaviors on its {@link PushNotificationsCategoryBehavior} delegate.
     */
    @Before
    public void setup() {
        this.delegate = mock(PushNotificationsCategoryBehavior.class);
        this.push = new RxPushNotificationsBinding(delegate);
    }

    /**
     * Tests that a successful request to category identify the user will
     * propagate a completion back through the binding.
     * @throws InterruptedException If test observer is interrupted while awaiting terminal event
     */
    @Test
    public void testIdentifyUserCategoryLevel() throws InterruptedException {
        NotificationsCategoryBehavior notificationDelegate = mock(NotificationsCategoryBehavior.class);
        RxNotificationsBinding notifications = new RxNotificationsBinding(notificationDelegate);

        String userId = "userId";

        // Arrange an invocation of the success Action
        doAnswer(invocation -> {
            // 0 = userId, 1 = onComplete, 2 = onFailure
            Action onCompletion = invocation.getArgument(1);
            onCompletion.call();
            return null;
        }).when(delegate).identifyUser(eq(userId), anyAction(), anyConsumer());

        doAnswer(invocation -> {
            // 0 = userId, 1 = onComplete, 2 = onFailure
            Action onCompletion = invocation.getArgument(1);
            delegate.identifyUser(userId, onCompletion, e -> { });
            return null;
        }).when(notificationDelegate).identifyUser(eq(userId), anyAction(), anyConsumer());

        // Act: call the binding
        TestObserver<Void> observer = notifications.identifyUser(userId).test();

        // Assert: Completable completes with success
        observer.await(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        observer.assertNoErrors().assertComplete();
    }

    /**
     * Tests that a successful request to category identify the user will
     * propagate a completion back through the binding.
     * @throws InterruptedException If test observer is interrupted while awaiting terminal event
     */
    @Test
    public void testIdentifyUserWithProfileCategoryLevel() throws InterruptedException {
        NotificationsCategoryBehavior notificationDelegate = mock(NotificationsCategoryBehavior.class);
        RxNotificationsBinding notifications = new RxNotificationsBinding(notificationDelegate);

        String userId = "userId";
        UserProfile profile = UserProfile.builder().name("test").build();

        // Arrange an invocation of the success Action
        doAnswer(invocation -> {
            // 0 = userId, 1 = profile, 2 = onComplete, 3 = onFailure
            Action onCompletion = invocation.getArgument(2);
            onCompletion.call();
            return null;
        }).when(delegate).identifyUser(eq(userId), eq(profile), anyAction(), anyConsumer());

        doAnswer(invocation -> {
            // 0 = userId, 1 = profile, 2 = onComplete, 3 = onFailure
            Action onCompletion = invocation.getArgument(2);
            delegate.identifyUser(userId, profile, onCompletion, e -> { });
            return null;
        }).when(notificationDelegate).identifyUser(eq(userId), eq(profile), anyAction(), anyConsumer());

        // Act: call the binding
        TestObserver<Void> observer = notifications.identifyUser(userId, profile).test();

        // Assert: Completable completes with success
        observer.await(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        observer.assertNoErrors().assertComplete();
    }

    /**
     * Tests that a successful request to identify the user will
     * propagate a completion back through the binding.
     * @throws InterruptedException If test observer is interrupted while awaiting terminal event
     */
    @Test
    public void testIdentifyUser() throws InterruptedException {
        String userId = "userId";

        // Arrange an invocation of the success Action
        doAnswer(invocation -> {
            // 0 = userId, 1 = onComplete, 2 = onFailure
            Action onCompletion = invocation.getArgument(1);
            onCompletion.call();
            return null;
        }).when(delegate).identifyUser(eq(userId), anyAction(), anyConsumer());

        // Act: call the binding
        TestObserver<Void> observer = push.identifyUser(userId).test();

        // Assert: Completable completes with success
        observer.await(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        observer.assertNoErrors().assertComplete();
    }

    /**
     * Tests that a successful request to identify the user will
     * propagate a completion back through the binding.
     * @throws InterruptedException If test observer is interrupted while awaiting terminal event
     */
    @Test
    public void testIdentifyUserWithProfile() throws InterruptedException {
        String userId = "userId";
        UserProfile profile = UserProfile.builder().name("test").build();

        // Arrange an invocation of the success Action
        doAnswer(invocation -> {
            // 0 = userId, 1 = profile, 2 = onComplete, 3 = onFailure
            Action onCompletion = invocation.getArgument(2);
            onCompletion.call();
            return null;
        }).when(delegate).identifyUser(eq(userId), eq(profile), anyAction(), anyConsumer());

        // Act: call the binding
        TestObserver<Void> observer = push.identifyUser(userId, profile).test();

        // Assert: Completable completes with success
        observer.await(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        observer.assertNoErrors().assertComplete();
    }

    /**
     * Identify the user failure is propagated up through the binding.
     * @throws InterruptedException If test observer is interrupted while awaiting terminal event
     */
    @Test
    public void testIdentifyUserFails() throws InterruptedException {
        String userId = "userId";

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
        TestObserver<Void> observer = push.identifyUser(userId).test();

        // Assert: failure is furnished via Rx Completable.
        observer.await(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        observer.assertNotComplete().assertError(failure);
    }

    /**
     * Identify the user failure is propagated up through the binding.
     * @throws InterruptedException If test observer is interrupted while awaiting terminal event
     */
    @Test
    public void testIdentifyUserWithProfileFails() throws InterruptedException {
        String userId = "userId";
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
        TestObserver<Void> observer = push.identifyUser(userId, profile).test();

        // Assert: failure is furnished via Rx Completable.
        observer.await(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        observer.assertNotComplete().assertError(failure);
    }

    /**
     * Tests that a successful request to register the device token will
     * propagate a completion back through the binding.
     * @throws InterruptedException If test observer is interrupted while awaiting terminal event
     */
    @Test
    public void testRegisterDevice() throws InterruptedException {
        String token = RandomString.string();

        // Arrange an invocation of the success Action
        doAnswer(invocation -> {
            // 0 = userId, 1 = onComplete, 2 = onFailure
            Action onCompletion = invocation.getArgument(1);
            onCompletion.call();
            return null;
        }).when(delegate).registerDevice(eq(token), anyAction(), anyConsumer());

        // Act: call the binding
        TestObserver<Void> observer = push.registerDevice(token).test();

        // Assert: Completable completes with success
        observer.await(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        observer.assertNoErrors().assertComplete();
    }

    /**
     * Register device token failure is propagated up through the binding.
     * @throws InterruptedException If test observer is interrupted while awaiting terminal event
     */
    @Test
    public void testRegisterDeviceFails() throws InterruptedException {
        String token = RandomString.string();

        // Arrange a callback on the failure consumer
        PushNotificationsException failure = new PushNotificationsException(
                "Failed to register FCM device token with the service.",
                REPORT_BUG_TO_AWS_SUGGESTION,
                new Exception()
        );
        doAnswer(invocation -> {
            // 0 = userId, 1 = onComplete, 2 = onFailure
            int positionOfFailureConsumer = 2;
            Consumer<PushNotificationsException> onFailure = invocation.getArgument(positionOfFailureConsumer);
            onFailure.accept(failure);
            return null;
        }).when(delegate).registerDevice(eq(token), anyAction(), anyConsumer());

        // Act: call the binding
        TestObserver<Void> observer = push.registerDevice(token).test();

        // Assert: failure is furnished via Rx Completable.
        observer.await(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        observer.assertNotComplete().assertError(failure);
    }

    /**
     * Tests that a successful request to record notification received event will
     * propagate a completion back through the binding.
     * @throws InterruptedException If test observer is interrupted while awaiting terminal event
     */
    @Test
    public void testRecordNotificationReceived() throws InterruptedException {
        // Arrange an invocation of the success Action
        doAnswer(invocation -> {
            // 0 = userId, 1 = onComplete, 2 = onFailure
            Action onCompletion = invocation.getArgument(1);
            onCompletion.call();
            return null;
        }).when(delegate).recordNotificationReceived(eq(payload), anyAction(), anyConsumer());

        // Act: call the binding
        TestObserver<Void> observer = push.recordNotificationReceived(payload).test();

        // Assert: Completable completes with success
        observer.await(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        observer.assertNoErrors().assertComplete();
    }

    /**
     * Record notification received event failure is propagated up through the binding.
     * @throws InterruptedException If test observer is interrupted while awaiting terminal event
     */
    @Test
    public void testRecordNotificationReceivedFails() throws InterruptedException {
        // Arrange a callback on the failure consumer
        PushNotificationsException failure = new PushNotificationsException(
                "Failed to record push notifications event $eventName.",
                REPORT_BUG_TO_AWS_SUGGESTION,
                new Exception()
        );
        doAnswer(invocation -> {
            // 0 = userId, 1 = onComplete, 2 = onFailure
            int positionOfFailureConsumer = 2;
            Consumer<PushNotificationsException> onFailure = invocation.getArgument(positionOfFailureConsumer);
            onFailure.accept(failure);
            return null;
        }).when(delegate).recordNotificationReceived(eq(payload), anyAction(), anyConsumer());

        // Act: call the binding
        TestObserver<Void> observer = push.recordNotificationReceived(payload).test();

        // Assert: failure is furnished via Rx Completable.
        observer.await(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        observer.assertNotComplete().assertError(failure);
    }

    /**
     * Tests that a successful request to record notification opened event will
     * propagate a completion back through the binding.
     * @throws InterruptedException If test observer is interrupted while awaiting terminal event
     */
    @Test
    public void testRecordNotificationOpened() throws InterruptedException {
        // Arrange an invocation of the success Action
        doAnswer(invocation -> {
            // 0 = userId, 1 = onComplete, 2 = onFailure
            Action onCompletion = invocation.getArgument(1);
            onCompletion.call();
            return null;
        }).when(delegate).recordNotificationOpened(eq(payload), anyAction(), anyConsumer());

        // Act: call the binding
        TestObserver<Void> observer = push.recordNotificationOpened(payload).test();

        // Assert: Completable completes with success
        observer.await(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        observer.assertNoErrors().assertComplete();
    }

    /**
     * Record notification opened event failure is propagated up through the binding.
     * @throws InterruptedException If test observer is interrupted while awaiting terminal event
     */
    @Test
    public void testRecordNotificationOpenedFails() throws InterruptedException {
        // Arrange a callback on the failure consumer
        PushNotificationsException failure = new PushNotificationsException(
            "Failed to record push notifications event $eventName.",
            REPORT_BUG_TO_AWS_SUGGESTION,
            new Exception()
        );
        doAnswer(invocation -> {
            // 0 = userId, 1 = onComplete, 2 = onFailure
            int positionOfFailureConsumer = 2;
            Consumer<PushNotificationsException> onFailure = invocation.getArgument(positionOfFailureConsumer);
            onFailure.accept(failure);
            return null;
        }).when(delegate).recordNotificationOpened(eq(payload), anyAction(), anyConsumer());

        // Act: call the binding
        TestObserver<Void> observer = push.recordNotificationOpened(payload).test();

        // Assert: failure is furnished via Rx Completable.
        observer.await(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        observer.assertNotComplete().assertError(failure);
    }

    /**
     * Tests that a successful request to should handle notification will
     * propagate a completion back through the binding.
     * @throws InterruptedException If test observer is interrupted while awaiting terminal event
     */
    @Test
    public void testShouldHandleNotification() throws InterruptedException {
        // Arrange an invocation of the success Action
        doReturn(true).when(delegate).shouldHandleNotification(eq(payload));

        // Act: call the binding
        boolean actual = push.shouldHandleNotification(payload);

        // Assert: Completable completes with success
        Assert.assertTrue(actual);
    }

    /**
     * Tests that a successful request to handle notification received event will
     * propagate a completion back through the binding.
     * @throws InterruptedException If test observer is interrupted while awaiting terminal event
     */
    @Test
    public void testHandleNotificationReceived() throws InterruptedException {
        PushNotificationResult expectedResult = PushNotificationResult.NotificationPosted.INSTANCE;

        // Arrange an invocation of the success Action
        doAnswer(invocation -> {
            // 0 = userId, 1 = onSuccess, 2 = onFailure
            int indexOfResultConsumer = 1;
            Consumer<PushNotificationResult> onResult = invocation.getArgument(indexOfResultConsumer);
            onResult.accept(expectedResult);
            return null;
        }).when(delegate).handleNotificationReceived(eq(payload), anyConsumer(), anyConsumer());

        // Act: call the binding
        TestObserver<PushNotificationResult> observer = push.handleNotificationReceived(payload).test();

        // Assert: Completable completes with success
        observer.await(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        observer.assertNoErrors().assertComplete();
    }

    /**
     * Handle notification received event failure is propagated up through the binding.
     * @throws InterruptedException If test observer is interrupted while awaiting terminal event
     */
    @Test
    public void testHandleNotificationReceivedFails() throws InterruptedException {
        // Arrange a callback on the failure consumer
        PushNotificationsException failure = new PushNotificationsException(
                "Failed to handle push notification message.",
                REPORT_BUG_TO_AWS_SUGGESTION,
                new Exception()
        );
        doAnswer(invocation -> {
            // 0 = userId, 1 = onComplete, 2 = onFailure
            int positionOfFailureConsumer = 2;
            Consumer<PushNotificationsException> onFailure = invocation.getArgument(positionOfFailureConsumer);
            onFailure.accept(failure);
            return null;
        }).when(delegate).handleNotificationReceived(eq(payload), anyConsumer(), anyConsumer());

        // Act: call the binding
        TestObserver<PushNotificationResult> observer = push.handleNotificationReceived(payload).test();

        // Assert: failure is furnished via Rx Completable.
        observer.await(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        observer.assertNotComplete().assertError(failure);
    }
}
