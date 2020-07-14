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

import android.content.Context;
import androidx.annotation.NonNull;

import com.amplifyframework.AmplifyException;
import com.amplifyframework.core.Amplify;
import com.amplifyframework.core.model.ModelProvider;
import com.amplifyframework.core.model.ModelSchemaRegistry;
import com.amplifyframework.datastore.DataStoreChannelEventName;
import com.amplifyframework.datastore.DataStoreConfigurationProvider;
import com.amplifyframework.datastore.DataStoreException;
import com.amplifyframework.datastore.appsync.AppSync;
import com.amplifyframework.datastore.storage.LocalStorageAdapter;
import com.amplifyframework.hub.HubChannel;
import com.amplifyframework.hub.HubEvent;
import com.amplifyframework.logging.Logger;

import org.json.JSONObject;

import java.util.Objects;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import io.reactivex.Completable;

/**
 * Synchronizes changed data between the {@link LocalStorageAdapter}
 * and {@link AppSync}.
 */
public final class Orchestrator {
    private static final Logger LOG = Amplify.Logging.forNamespace("amplify:aws-datastore");
    private static final long SYNC_TIMEOUT_MS = TimeUnit.SECONDS.toMillis(10);
    // This timeout has to be somewhat generous to account for situations where a request to
    // stop is made immediately after starting things up. This should only be the case
    // when the clear API is invoked right after the plugin starts.
    private static final long ACQUIRE_PERMIT_TIMEOUT_MS = TimeUnit.SECONDS.toMillis(30);

    private final SubscriptionProcessor subscriptionProcessor;
    private final SyncProcessor syncProcessor;
    private final MutationOutbox mutationOutbox;
    private final MutationProcessor mutationProcessor;
    private final StorageObserver storageObserver;
    private final AtomicReference<OrchestratorStatus> status;
    private final Semaphore startStopSemaphore;
    private Completable initializationCompletable;

    /**
     * Constructs a new Orchestrator.
     * The Orchestrator will synchronize data between the {@link AppSync}
     * and the {@link LocalStorageAdapter}.
     * @param modelProvider A provider of the models to be synchronized
     * @param modelSchemaRegistry A registry of model schema
     * @param localStorageAdapter Interface to local storage, used to
     *                       durably store offline changes until
     *                       then can be written to the network
     * @param appSync An AppSync Endpoint
     * @param dataStoreConfigurationProvider An instance that implements {@link DataStoreConfigurationProvider}
     *                       Note that the provider-style interface is needed because
     *                       at the time this constructor is called from the
     *                       {@link com.amplifyframework.datastore.AWSDataStorePlugin}'s constructor,
     *                       the plugin is not fully configured yet. The reference to the
     *                       variable returned by the provider only get set after the plugin's
     *                       #{@link com.amplifyframework.datastore.AWSDataStorePlugin#configure(JSONObject, Context)}
     *                       is invoked by Amplify.
     *
     */
    public Orchestrator(
            @NonNull final ModelProvider modelProvider,
            @NonNull final ModelSchemaRegistry modelSchemaRegistry,
            @NonNull final LocalStorageAdapter localStorageAdapter,
            @NonNull final AppSync appSync,
            @NonNull final DataStoreConfigurationProvider dataStoreConfigurationProvider) {
        Objects.requireNonNull(modelSchemaRegistry);
        Objects.requireNonNull(modelProvider);
        Objects.requireNonNull(appSync);
        Objects.requireNonNull(localStorageAdapter);
        this.status = new AtomicReference<>(OrchestratorStatus.STOPPED);
        this.mutationOutbox = new PersistentMutationOutbox(localStorageAdapter);
        VersionRepository versionRepository = new VersionRepository(localStorageAdapter);
        Merger merger = new Merger(mutationOutbox, versionRepository, localStorageAdapter);
        SyncTimeRegistry syncTimeRegistry = new SyncTimeRegistry(localStorageAdapter);

        this.mutationProcessor = new MutationProcessor(merger, versionRepository, mutationOutbox, appSync);
        this.syncProcessor = SyncProcessor.builder()
            .modelProvider(modelProvider)
            .modelSchemaRegistry(modelSchemaRegistry)
            .syncTimeRegistry(syncTimeRegistry)
            .appSync(appSync)
            .merger(merger)
            .dataStoreConfigurationProvider(dataStoreConfigurationProvider)
            .build();
        this.subscriptionProcessor = new SubscriptionProcessor(appSync, modelProvider, merger);
        this.storageObserver = new StorageObserver(localStorageAdapter, mutationOutbox);
        this.startStopSemaphore = new Semaphore(1);
    }

    /**
     * Checks whether the orchestrator is {@link OrchestratorStatus#STARTED}.
     * @return True if the orchestrator is started, false otherwise.
     */
    public boolean isStarted() {
        return OrchestratorStatus.STARTED.equals(status.get());
    }

    /**
     * Start performing sync operations between the local storage adapter
     * and the remote GraphQL endpoint.
     * @return A Completable operation to start the sync engine orchestrator
     */
    @NonNull
    public synchronized Completable start() {
        if (!transitionToState(OrchestratorStatus.STARTED)) {
            return Completable.error(new DataStoreException(
                "Unable to start the orchestrator because an operation is already in progress.",
                AmplifyException.TODO_RECOVERY_SUGGESTION)
            );
        }
        return mutationOutbox.load().andThen(
            Completable.fromAction(() -> {
                LOG.debug("Starting the orchestrator.");
                if (!storageObserver.isObservingStorageChanges()) {
                    LOG.debug("Starting local storage observer.");
                    storageObserver.startObservingStorageChanges();
                }
                if (!subscriptionProcessor.isObservingSubscriptionEvents()) {
                    LOG.debug("Starting subscription processor.");
                    subscriptionProcessor.startSubscriptions();
                }
                if (!syncProcessor.hydrate().blockingAwait(SYNC_TIMEOUT_MS, TimeUnit.MILLISECONDS)) {
                    throw new DataStoreException(
                        "Initial sync during DataStore initialization exceeded timeout of " + SYNC_TIMEOUT_MS,
                        AmplifyException.REPORT_BUG_TO_AWS_SUGGESTION
                    );
                }
                if (!mutationProcessor.isDrainingMutationOutbox()) {
                    LOG.debug("Starting mutation processor.");
                    mutationProcessor.startDrainingMutationOutbox();
                }
                if (!subscriptionProcessor.isDrainingMutationBuffer()) {
                    LOG.debug("Starting draining mutation buffer.");
                    subscriptionProcessor.startDrainingMutationBuffer();
                }
                status.compareAndSet(OrchestratorStatus.STARTING, OrchestratorStatus.STARTED);
                LOG.debug("Orchestrator started.");
                announceRemoteSyncStarted();
            })
        ).doFinally(startStopSemaphore::release);
    }

    /**
     * Stop all model synchronization.
     * @return A completable with the activities
     */
    public synchronized Completable stop() {
        if (!transitionToState(OrchestratorStatus.STOPPED)) {
            return Completable.error(new DataStoreException(
                "Unable to stop the orchestrator because an operation is already in progress.",
                AmplifyException.TODO_RECOVERY_SUGGESTION)
            );
        }
        return Completable.fromAction(() -> {
            LOG.info("Intentionally stopping cloud synchronization, now.");
            subscriptionProcessor.stopAllSubscriptionActivity();
            storageObserver.stopObservingStorageChanges();
            mutationProcessor.stopDrainingMutationOutbox();
            status.compareAndSet(OrchestratorStatus.STOPPING, OrchestratorStatus.STOPPED);
            LOG.debug("Stopped remote synchronization.");
            announceRemoteSyncStopped();
        })
        .doFinally(startStopSemaphore::release);
    }

    private synchronized boolean transitionToState(OrchestratorStatus targetStatus) {
        OrchestratorStatus expectedCurrentStatus;
        switch (targetStatus) {
            case STARTED:
                expectedCurrentStatus = OrchestratorStatus.STOPPED;
                break;
            case STOPPED:
                expectedCurrentStatus = OrchestratorStatus.STARTED;
                break;
            default:
                LOG.warn("Invalid attempt to transition orchestrator to " + targetStatus.name());
                return false;
        }
        try {
            LOG.debug("Requesting permit to set the orchestrator status to:" + targetStatus.name());
            boolean permitAcquired = startStopSemaphore.tryAcquire(ACQUIRE_PERMIT_TIMEOUT_MS, TimeUnit.MILLISECONDS);
            if (!permitAcquired) {
                LOG.warn("Unable to acquire permit to set the orchestrator status to:" + targetStatus.name());
                return false;
            }
            boolean statusSet = status.compareAndSet(expectedCurrentStatus, targetStatus);
            // only stop if it's started AND if we can get a permit.
            if (!statusSet) {
                LOG.warn(String.format("Failed to set orchestrator status to: %s. Current status: %s",
                    targetStatus.name(),
                    status.get())
                );
                // Since we acquired the permit but failed to set the status, let's release the permit.
                startStopSemaphore.release();
                return false;
            }
        } catch (InterruptedException exception) {
            LOG.warn("Orchestrator was interrupted while setting status to " + targetStatus.name());
            return false;
        }
        return true;
    }

    private void announceRemoteSyncStarted() {
        Amplify.Hub.publish(
            HubChannel.DATASTORE,
            HubEvent.create(DataStoreChannelEventName.REMOTE_SYNC_STARTED)
        );
    }

    private void announceRemoteSyncStopped() {
        Amplify.Hub.publish(
            HubChannel.DATASTORE,
            HubEvent.create(DataStoreChannelEventName.REMOTE_SYNC_STOPPED)
        );
    }

    /**
     * Represents possible status of the orchestrator.
     */
    enum OrchestratorStatus {
        /**
         * The orchestrator is in the process of shutting down all the necessary components. Any requests to
         * start it will be ignored.
         *
         * Upon completion, the state should be changed to {@link #STOPPED}.
         */
        STOPPING,
        /**
         * The orchestrator is stopped and it is currently not performing any background processing. At this point
         * it is safe to start it. Only possible transition from this state should be to {@link #STARTING}
         * which happens by invoking {@link #start()}
         */
        STOPPED,
        /**
         * The orchestrator is bootstrapping all the components needed to perform the different background
         * processes it orchestrates.
         *
         * Upon completion, the state should be changed to {@link #STARTED}
         */
        STARTING,
        /**
         * The orchestrator is started. Only possible transition from this state should be to {@link #STOPPING}
         * which happens by invoking {@link #stop()}
         */
        STARTED
    }
}
