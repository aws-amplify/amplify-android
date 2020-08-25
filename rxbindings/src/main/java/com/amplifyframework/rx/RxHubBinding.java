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

import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;

import com.amplifyframework.core.Amplify;
import com.amplifyframework.hub.HubCategory;
import com.amplifyframework.hub.HubCategoryBehavior;
import com.amplifyframework.hub.HubChannel;
import com.amplifyframework.hub.HubEvent;
import com.amplifyframework.hub.SubscriptionToken;

import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.disposables.Disposable;

final class RxHubBinding implements RxHubCategoryBehavior {

    private final HubCategoryBehavior hub;

    RxHubBinding() {
        this(Amplify.Hub);
    }

    @SuppressWarnings("WeakerAccess")
    @VisibleForTesting
    RxHubBinding(HubCategory hub) {
        this.hub = hub;
    }

    @NonNull
    @Override
    public <T> Completable publish(@NonNull HubChannel hubChannel, @NonNull HubEvent<T> hubEvent) {
        return Completable.defer(() -> Completable.fromAction(() -> hub.publish(hubChannel, hubEvent)));
    }

    @NonNull
    @Override
    public Observable<HubEvent<?>> on(@NonNull HubChannel hubChannel) {
        return Observable.defer(() -> Observable.create(emitter -> {
            SubscriptionToken token = hub.subscribe(hubChannel, emitter::onNext);
            emitter.setDisposable(Disposable.fromAction(() -> hub.unsubscribe(token)));
        }));
    }
}
