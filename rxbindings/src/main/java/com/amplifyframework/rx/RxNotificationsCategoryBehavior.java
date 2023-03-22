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
import com.amplifyframework.notifications.NotificationsCategoryBehavior;
import com.amplifyframework.notifications.pushnotifications.PushNotificationsException;

import io.reactivex.rxjava3.annotations.NonNull;
import io.reactivex.rxjava3.core.Completable;

/**
 * An Rx-idiomatic expression of the {@link NotificationsCategoryBehavior}.
 */
public interface RxNotificationsCategoryBehavior {

    @SuppressWarnings("checkstyle:all") RxPushNotificationsCategoryBehavior Push = new RxPushNotificationsBinding();

    /**
     * Identifies the user with the service.
     * @param userId user identifier
     * @return An Rx {@link Completable} which completes successfully if user profile was updated with identity,
     *         emits an {@link PushNotificationsException} otherwise
     */
    Completable identifyUser(@NonNull String userId);

    /**
     * Identifies the user with the service.
     * @param userId user identifier
     * @param profile user profile
     * @return An Rx {@link Completable} which completes successfully if user profile was updated with identity,
     *         emits an {@link PushNotificationsException} otherwise
     */
    Completable identifyUser(@NonNull String userId, @NonNull UserProfile profile);
}
