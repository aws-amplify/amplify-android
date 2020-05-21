package com.amplifyframework.datastore.syncengine;

import androidx.annotation.NonNull;

import com.amplifyframework.datastore.DataStoreChannelEventName;
import com.amplifyframework.hub.HubCategoryBehavior;
import com.amplifyframework.hub.HubChannel;
import com.amplifyframework.hub.HubEvent;
import com.amplifyframework.hub.HubEventFilter;
import com.amplifyframework.hub.HubSubscriber;
import com.amplifyframework.hub.SubscriptionToken;

import io.reactivex.Observable;
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

    OnlineState(HubCategoryBehavior hub, AwsReachability aws) {
        this.hub = hub;
        this.aws = aws;
        this.subject = PublishSubject.<Boolean>create().toSerialized();
    }

    synchronized Disposable startDetecting() {
        SubscriptionToken token = hub.subscribe(HubChannel.DATASTORE, this, this);
        return Disposables.fromAction(() -> {
            hub.unsubscribe(token);
            subject.onComplete();
        });
    }

    Observable<Boolean> observe() {
        return subject
            .startWith(aws.isReachable().toObservable())
            .subscribeOn(Schedulers.io())
            .observeOn(Schedulers.io());
    }

    @Override
    public boolean filter(@NonNull HubEvent<?> hubEvent) {
        return DataStoreChannelEventName.LOST_CONNECTION.toString().equals(hubEvent.getName()) ||
            DataStoreChannelEventName.REGAINED_CONNECTION.toString().equals(hubEvent.getName());
    }

    @Override
    public void onEvent(@NonNull HubEvent<?> hubEvent) {
        subject.onNext(DataStoreChannelEventName.REGAINED_CONNECTION.toString().equals(hubEvent.getName()));
    }
}
