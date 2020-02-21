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

package com.amplifyframework.datastore.network;

import androidx.annotation.NonNull;

import com.amplifyframework.core.Amplify;
import com.amplifyframework.core.model.Model;
import com.amplifyframework.datastore.DataStoreChannelEventName;
import com.amplifyframework.datastore.storage.StorageItemChange;
import com.amplifyframework.hub.HubChannel;
import com.amplifyframework.hub.HubEvent;
import com.amplifyframework.logging.Logger;

import java.util.Objects;

import io.reactivex.Single;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;

/**
 * The {@link MutationProcessor} observes the {@link MutationOutbox}, and publishes its items to an
 * {@link AppSyncEndpoint}.
 *
 * The responses to these mutations are themselves forwarded to the Merger (TODO: write a merger.)
 */
final class MutationProcessor {
    private static final Logger LOG = Amplify.Logging.forNamespace("amplify:aws-datastore");

    private final AppSyncEndpoint appSyncEndpoint;
    private final MutationOutbox mutationOutbox;
    private final CompositeDisposable disposable;

    MutationProcessor(
            @NonNull MutationOutbox mutationOutbox,
            @NonNull AppSyncEndpoint appSyncEndpoint) {
        this.appSyncEndpoint = Objects.requireNonNull(appSyncEndpoint);
        this.mutationOutbox = Objects.requireNonNull(mutationOutbox);
        this.disposable = new CompositeDisposable();
    }

    /**
     * Start observing the mutation outbox for locally-initiated changes.
     */
    void startDrainingMutationOutbox() {
        disposable.add(
            mutationOutbox.observe()
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.io())
                .flatMapSingle(this::publishToNetwork)
                .flatMapSingle(mutationOutbox::remove)
                .subscribe(
                    processedChange -> {
                        LOG.info("Change processed successfully! " + processedChange);
                        HubEvent<? extends Model> publishedToCloudEvent =
                            HubEvent.create(DataStoreChannelEventName.PUBLISHED_TO_CLOUD, processedChange.item());
                        Amplify.Hub.publish(HubChannel.DATASTORE, publishedToCloudEvent);
                    },
                    error -> LOG.warn("Error ended observation of mutation outbox: ", error),
                    () -> LOG.warn("Observation of mutation outbox was completed.")
                )
        );
    }

    /**
     * Don't process any more mutations.
     */
    void stopDrainingMutationOutbox() {
        disposable.dispose();
    }

    /**
     * To process a StorageItemChange, we try to publish it to the remote GraphQL
     * API. If that succeeds, then we can remove it from the outbox. Otherwise,
     * we have to keep the mutation in the outbox, so that we can try to publish
     * it again later, when network conditions become favorable again.
     * @param storageItemChange A storage item change to be published to remote API
     * @return A single which completes with the successfully published item, or errors
     *         if the publication fails
     */
    @SuppressWarnings("checkstyle:MethodTypeParameterName") // The generics are complex, so use meaningful names
    private <MODEL extends Model, SIC extends StorageItemChange<MODEL>> Single<SIC> publishToNetwork(
        final SIC storageItemChange) {
        //noinspection CodeBlock2Expr More readable as a block statement
        return Single.defer(() -> Single.create(subscriber -> {
            appSyncEndpoint.create(
                storageItemChange.item(),
                result -> {
                    if (result.hasErrors() || !result.hasData()) {
                        subscriber.onError(new RuntimeException("Failed to publish item to network."));
                    } else {
                        subscriber.onSuccess(storageItemChange);
                    }
                },
                subscriber::onError
            );
        }));
    }
}
