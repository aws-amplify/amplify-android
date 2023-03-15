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

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;

import com.amplifyframework.analytics.UserProfile;
import com.amplifyframework.core.Amplify;
import com.amplifyframework.notifications.NotificationsCategoryBehavior;
import com.amplifyframework.notifications.pushnotifications.PushNotificationsException;

import java.util.Objects;

import io.reactivex.rxjava3.core.Completable;

final class RxNotificationsBinding implements RxNotificationsCategoryBehavior {
    private final NotificationsCategoryBehavior delegate;

    RxNotificationsBinding() {
        this(Amplify.Notifications);
    }

    @VisibleForTesting
    RxNotificationsBinding(@NonNull NotificationsCategoryBehavior delegate) {
        this.delegate = Objects.requireNonNull(delegate);
    }

    @Override
    public Completable identifyUser(String userId) {
        return toCompletable(((onResult, onError) -> delegate.identifyUser(userId, onResult, onError)));
    }

    @Override
    public Completable identifyUser(String userId, @NonNull UserProfile profile) {
        return toCompletable(((onResult, onError) -> delegate.identifyUser(userId, profile, onResult, onError)));
    }

    private Completable toCompletable(RxAdapters.VoidBehaviors.ActionEmitter<PushNotificationsException> behavior) {
        return RxAdapters.VoidBehaviors.toCompletable(behavior);
    }
}
