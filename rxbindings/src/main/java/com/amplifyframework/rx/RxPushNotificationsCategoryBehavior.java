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

import com.amplifyframework.notifications.pushnotifications.NotificationPayload;
import com.amplifyframework.notifications.pushnotifications.PushNotificationResult;
import com.amplifyframework.notifications.pushnotifications.PushNotificationsException;

import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Single;

/**
 * Tests the {@link RxNotificationsCategoryBehavior}.
 */
public interface RxPushNotificationsCategoryBehavior extends RxNotificationsCategoryBehavior {

    /**
     * Registers that a notification was received while the app was in the foreground/background/kill.
     * @param payload campaign/journey data
     * @return An Rx {@link Completable} which completes successfully if notification received event is recorded,
     *         emits an {@link PushNotificationsException} otherwise
     */
    Completable recordNotificationReceived(NotificationPayload payload);

    /**
     * Registers that a user opened a notification.
     * @param payload campaign/journey data
     * @return An Rx {@link Completable} which completes successfully if notification opened event is recorded,
     *         emits an {@link PushNotificationsException} otherwise
     */
    Completable recordNotificationOpened(NotificationPayload payload);

    /**
     * Returns whether Amplify can handle the notification payload.
     * @param payload notification payload
     * @return true is Amplify can handle the notification payload
     */
    Boolean shouldHandleNotification(NotificationPayload payload);

    /**
     * Displays notification on the system tray if app is background/killed state.
     * @param payload notification payload
     * @return An Rx {@link Single} which emits an {@link PushNotificationResult} on success, or an
     *         {@link PushNotificationsException} on failure
     */
    Single<PushNotificationResult> handleNotificationReceived(NotificationPayload payload);

    /**
     * Registers device token from FCM with the service. This API creates/updates the service with token.
     * @param token device registration token
     * @return An Rx {@link Completable} which completes successfully if device token is registered with the service,
     *         emits an {@link PushNotificationsException} otherwise
     */
    Completable registerDevice(String token);
}
