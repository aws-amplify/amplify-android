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

package com.amplifyframework.datastore.syncengine;

import androidx.annotation.NonNull;

import com.amplifyframework.core.reachability.Reachability;
import com.amplifyframework.datastore.DataStoreChannelEventName;
import com.amplifyframework.hub.HubCategoryBehavior;
import com.amplifyframework.hub.HubChannel;
import com.amplifyframework.hub.HubEvent;
import com.amplifyframework.hub.HubEventFilter;
import com.amplifyframework.hub.HubSubscriber;
import com.amplifyframework.hub.SubscriptionToken;

import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.disposables.Disposables;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subjects.PublishSubject;
import io.reactivex.subjects.Subject;

/**
 * Models the current internet connection state, online or offline.
 */
final class OnlineState implements HubEventFilter, HubSubscriber {
    private final HubCategoryBehavior hub;
    private final Subject<Boolean> subject;
    private final AwsReachability aws;
    private final CompositeDisposable hubDisposable;
    private final CompositeDisposable detectionDisposable;

    OnlineState(HubCategoryBehavior hub, Reachability reachability) {
        this.hub = hub;
        this.aws = new AwsReachability(reachability);
        this.subject = PublishSubject.<Boolean>create().toSerialized();
        this.hubDisposable = new CompositeDisposable();
        this.detectionDisposable = new CompositeDisposable();
    }

    synchronized Disposable startDetecting() {
        SubscriptionToken token = hub.subscribe(HubChannel.DATASTORE, this, this);
        hubDisposable.add(Disposables.fromAction(() -> hub.unsubscribe(token)));
        return hubDisposable;
    }

    Observable<Boolean> observe() {
        return subject
            .subscribeOn(Schedulers.single())
            .observeOn(Schedulers.single());
    }

    @Override
    public boolean filter(@NonNull HubEvent<?> hubEvent) {
        return DataStoreChannelEventName.LOST_CONNECTION.toString().equals(hubEvent.getName()) ||
            DataStoreChannelEventName.REGAINED_CONNECTION.toString().equals(hubEvent.getName());
    }

    @Override
    public void onEvent(@NonNull HubEvent<?> hubEvent) {
        if (DataStoreChannelEventName.LOST_CONNECTION.toString().equals(hubEvent.getName())) {
            awaitNextOnlineStatus();
            subject.onNext(false);
        } else if (DataStoreChannelEventName.REGAINED_CONNECTION.toString().equals(hubEvent.getName())) {
            subject.onNext(true);
        }
    }

    Single<Boolean> check() {
        return aws.isReachable()
            .doOnSuccess(isOnline -> {
                if (!isOnline) {
                    awaitNextOnlineStatus();
                }
            });
    }

    private void awaitNextOnlineStatus() {
        if (detectionDisposable.size() < 1) {
            detectionDisposable.add(aws.awaitReachable().subscribe(() -> {
                hub.publish(HubChannel.DATASTORE, HubEvent.create(DataStoreChannelEventName.REGAINED_CONNECTION));
                detectionDisposable.clear();
            }));
        }
    }
}
