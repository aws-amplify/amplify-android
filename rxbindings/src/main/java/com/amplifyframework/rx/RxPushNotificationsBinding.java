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

import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;

import com.amplifyframework.analytics.UserProfile;
import com.amplifyframework.core.Amplify;
import com.amplifyframework.notifications.pushnotifications.PushNotificationResult;
import com.amplifyframework.notifications.pushnotifications.PushNotificationsCategoryBehavior;
import com.amplifyframework.notifications.pushnotifications.PushNotificationsException;

import java.util.Map;
import java.util.Objects;

import io.reactivex.rxjava3.annotations.Nullable;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Single;

final class RxPushNotificationsBinding implements RxPushNotificationsCategoryBehavior {
    private final PushNotificationsCategoryBehavior delegate;

    RxPushNotificationsBinding() {
        this(Amplify.Notifications.Push);
    }

    @VisibleForTesting
    RxPushNotificationsBinding(@NonNull PushNotificationsCategoryBehavior delegate) {
        this.delegate = Objects.requireNonNull(delegate);
    }

    @Override
    public Completable identifyUser(String userId, @Nullable UserProfile profile) {
        return toCompletable(((onResult, onError) -> delegate.identifyUser(userId, profile, onResult, onError)));
    }

    @Override
    public Completable recordNotificationReceived(Bundle data) {
        return toCompletable(((onResult, onError) -> delegate.recordNotificationReceived(data, onResult, onError)));
    }

    @Override
    public Completable recordNotificationOpened(Bundle data) {
        return toCompletable(((onResult, onError) -> delegate.recordNotificationOpened(data, onResult, onError)));
    }

    @Override
    public Single<PushNotificationResult> handleNotificationReceived(Bundle details) {
        return toSingle(((onResult, onError) -> delegate.handleNotificationReceived(details, onResult, onError)));
    }

    @Override
    public Completable registerDevice(String token) {
        return toCompletable(((onResult, onError) -> delegate.registerDevice(token, onResult, onError)));
    }

    private <T> Single<T> toSingle(RxAdapters.VoidBehaviors.ResultEmitter<T, PushNotificationsException> behavior) {
        return RxAdapters.VoidBehaviors.toSingle(behavior);
    }

    private Completable toCompletable(RxAdapters.VoidBehaviors.ActionEmitter<PushNotificationsException> behavior) {
        return RxAdapters.VoidBehaviors.toCompletable(behavior);
    }
}
