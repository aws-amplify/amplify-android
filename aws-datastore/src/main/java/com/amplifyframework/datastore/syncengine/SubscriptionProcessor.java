/*
 * Copyright 2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

import com.amplifyframework.core.Amplify;
import com.amplifyframework.core.model.Model;
import com.amplifyframework.logging.Logger;

import java.util.Objects;

import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.subjects.ReplaySubject;

/**
 * Observes mutations occurring on a remote system, via the {@link RemoteModelMutations}.
 * For each, marries the data back into the local DataStore, through the {@link Merger}.
 */
final class SubscriptionProcessor {
    private static final Logger LOG = Amplify.Logging.forNamespace("amplify:aws-datastore");

    private final RemoteModelMutations remoteModelMutations;
    private final Merger merger;
    private final CompositeDisposable disposable;
    private final ReplaySubject<SubscriptionEvent<? extends Model>> buffer;

    /**
     * Constructs a new SubscriptionProcessor.
     * @param remoteModelMutations An observable stream of mutations occurring on other end of network
     * @param merger A merger, to apply data back into local storage
     */
    SubscriptionProcessor(
            @NonNull RemoteModelMutations remoteModelMutations,
            @NonNull Merger merger) {
        this.remoteModelMutations = Objects.requireNonNull(remoteModelMutations);
        this.merger = Objects.requireNonNull(merger);
        this.disposable = new CompositeDisposable();
        this.buffer = ReplaySubject.create();
    }

    /**
     * Start subscribing to model mutations.
     */
    void startSubscriptions() {
        disposable.add(remoteModelMutations.observe()
            .subscribe(
                mutationOnSubscription -> {
                    buffer.onNext(mutationOnSubscription);
                    LOG.info("Successfully enqueued mutation from subscription: " + mutationOnSubscription);
                },
                error -> LOG.warn("Error enqueuing mutation from subscription.", error),
                () -> LOG.warn("Subscription to remote model mutations is completed.")
            )
        );
    }

    /**
     * Start draining mutations out of the mutation buffer.
     * This should be called after {@link #startSubscriptions()}.
     */
    void startDrainingMutationBuffer() {
        disposable.add(
            buffer
                .flatMapCompletable(mutation -> merger.merge(mutation.modelWithMetadata()))
                .subscribe(
                    () -> LOG.warn("Reading from subscriptions buffer is completed."),
                    failure -> LOG.warn("Reading subscriptions buffer has failed.", failure)
                )
        );
    }

    /**
     * Stop any active subscriptions, and stop draining the mutation buffer.
     */
    void stopAllSubscriptionActivity() {
        disposable.clear();
    }
}
