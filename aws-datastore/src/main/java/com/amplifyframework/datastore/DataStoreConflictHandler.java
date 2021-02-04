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

package com.amplifyframework.datastore;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.util.ObjectsCompat;

import com.amplifyframework.core.Consumer;
import com.amplifyframework.core.model.Model;

import java.util.Objects;

/**
 * A hook to handle a conflict between local and remote copies of a model.
 * The DataStore customer can implement their own version of this handler,
 * and provide that handler to the {@link DataStoreConfiguration} while constructing
 * the DataStore plugin using
 * {@link AWSDataStorePlugin#AWSDataStorePlugin(DataStoreConfiguration)}.
 */
public interface DataStoreConflictHandler {
    /**
     * Factory to obtain a handler that always applies the remote copy
     * of the data.
     * @return A DataStore conflict handler that always applies the remote
     *         copy of the data.
     */
    static DataStoreConflictHandler alwaysApplyRemote() {
        return new AlwaysApplyRemoteHandler();
    }

    /**
     * Factory to obtain a handler that always retries the local copy
     * of the data.
     * @return A DataStore conflict handler that always retries the
     *         local copy of the data.
     */
    static DataStoreConflictHandler alwaysRetryLocal() {
        return new AlwaysRetryLocalHandler();
    }

    /**
     * This callback method is invoked when the DataStore detects a conflict between
     * a local and remote version of a model instance. Such a conflict may occur
     * while the system is trying to upload a local change to the remote system.
     *
     * All code paths in the implementation of this handler must terminate by
     * calling the provided consumer with a conflict resolution decision.
     *
     * @param conflictData Data about the conflict, including the local and remote
     *                     copies of the model that are in conflict
     * @param onDecision An implementation of the DataStoreConflictHandler must
     *                   end by invoking one of the resolutions on this handle.
     */
    void onConflictDetected(
            @NonNull ConflictData<? extends Model> conflictData,
            @NonNull Consumer<ConflictResolutionDecision<? extends Model>> onDecision
    );

    /**
     * A handler which always elects to apply the remote version of the data.
     */
    final class AlwaysApplyRemoteHandler implements DataStoreConflictHandler {
        @Override
        public void onConflictDetected(
                @NonNull ConflictData<? extends Model> conflictData,
                @NonNull Consumer<ConflictResolutionDecision<? extends Model>> onDecision) {
            onDecision.accept(ConflictResolutionDecision.applyRemote());
        }
    }

    /**
     * A handler which always elects to re-send the local data in a new mutation.
     */
    final class AlwaysRetryLocalHandler implements DataStoreConflictHandler {
        @Override
        public void onConflictDetected(
                @NonNull ConflictData<? extends Model> conflictData,
                @NonNull Consumer<ConflictResolutionDecision<? extends Model>> onDecision) {
            onDecision.accept(ConflictResolutionDecision.retryLocal());
        }
    }

    /**
     * Represents a conflict between two models that has occurred during model synchronization.
     * One was found locally, and another found in the remote system.
     * @param <T> The type of the model instances that conflict (they are both of the same type.)
     */
    final class ConflictData<T extends Model> {
        private final T local;
        private final T remote;

        private ConflictData(T local, T remote) {
            this.local = local;
            this.remote = remote;
        }

        /**
         * Creates a new {@link ConflictData}.
         * @param local The instance of a model that was found locally
         * @param remote The instance of a model that was found on the server
         * @param <T> The type of the model instances experiencing conflict
         * @return Data about a model conflict
         */
        @NonNull
        public static <T extends Model> ConflictData<T> create(@NonNull T local, @NonNull T remote) {
            Objects.requireNonNull(local);
            Objects.requireNonNull(remote);
            return new ConflictData<>(local, remote);
        }

        /**
         * Gets the local model.
         * @return Local model
         */
        @NonNull
        public T getLocal() {
            return this.local;
        }

        /**
         * Gets the remote model.
         * @return Remote model
         */
        @NonNull
        public T getRemote() {
            return this.remote;
        }

        @Override
        public boolean equals(@Nullable Object thatObject) {
            if (this == thatObject) {
                return true;
            }
            if (thatObject == null || getClass() != thatObject.getClass()) {
                return false;
            }

            ConflictData<?> that = (ConflictData<?>) thatObject;

            if (!getLocal().equals(that.getLocal())) {
                return false;
            }
            return getRemote().equals(that.getRemote());
        }

        @Override
        public int hashCode() {
            int result = getLocal().hashCode();
            result = 31 * result + getRemote().hashCode();
            return result;
        }

        @Override
        public String toString() {
            return "DataStoreConflictData{" +
                "local=" + local +
                ", remote=" + remote +
                '}';
        }
    }

    /**
     * An enumeration of the various strategies available for dealing with a model conflict.
     */
    enum ResolutionStrategy {
        /**
         * Conflict handled by discarding the local (client-side) changes, preferring whatever
         * was on the server.
         */
        APPLY_REMOTE,

        /**
         * The conflict was handled by retrying to update the remote store with the local model.
         */
        RETRY_LOCAL,

        /**
         * Conflict was handled by passing in a new version of the model to the
         * remote store which will eventually sync with the local store.
         */
        RETRY
    }

    /**
     * The possible decisions for a conflict resolution. Either we will try to apply the remote changes
     * to the local store, OR we will try to update the remote store using the local version of the model
     * OR we will retry a mutation against the remote store by using a custom version of the model object,
     * distinct from both the current local and remote versions.
     * @param <T> Type of model experiencing conflict
     */
    final class ConflictResolutionDecision<T extends Model> {
        private final ResolutionStrategy resolutionStrategy;
        private final T customModel;

        private ConflictResolutionDecision(ResolutionStrategy resolutionStrategy, T customModel) {
            this.resolutionStrategy = resolutionStrategy;
            this.customModel = customModel;
        }

        /**
         * Create a decision to apply the remote data.
         * @param <T> The type of model in conflict
         * @return A decision to use and apply the remote data.
         */
        @NonNull
        public static <T extends Model> ConflictResolutionDecision<T> applyRemote() {
            return new ConflictResolutionDecision<>(ResolutionStrategy.APPLY_REMOTE, null);
        }

        /**
         * Creates a decision to retry application of the local data.
         * @param <T> The type of model in conflict
         * @return A decision to retry application of the local data
         */
        @NonNull
        public static <T extends Model> ConflictResolutionDecision<T> retryLocal() {
            return new ConflictResolutionDecision<>(ResolutionStrategy.RETRY_LOCAL, null);
        }

        /**
         * Creates a decision to retry a mutation, but by applying a provided model.
         * The provided model may be a hybrid of the remote/local data, or perhaps it is unrelated
         * entirely except by its model ID (which must match).
         * @param customModel Resolve the conflict by using this custom model instance
         * @param <T> Type of model experiencing a conflict
         * @return A decision to retry mutation with the provided model
         */
        @NonNull
        public static <T extends Model> ConflictResolutionDecision<T> retry(@NonNull T customModel) {
            Objects.requireNonNull(customModel);
            return new ConflictResolutionDecision<>(ResolutionStrategy.RETRY, customModel);
        }

        /**
         * Gets the strategy that will be employed to resolve the conflict.
         * @return Conflict resolution strategy
         */
        @NonNull
        public ResolutionStrategy getResolutionStrategy() {
            return resolutionStrategy;
        }

        /**
         * When the conflict resolution strategy is {@link ResolutionStrategy#RETRY},
         * this returns the custom model with which to retry. Note that this value may
         * be null if the strategy is not RETRY.
         * @return Custom model with which to retry when the resolution strategy is
         *         {@link ResolutionStrategy#RETRY}
         */
        @Nullable
        public T getCustomModel() {
            return customModel;
        }

        @Override
        public boolean equals(@Nullable Object thatObject) {
            if (this == thatObject) {
                return true;
            }
            if (thatObject == null || getClass() != thatObject.getClass()) {
                return false;
            }

            ConflictResolutionDecision<?> that = (ConflictResolutionDecision<?>) thatObject;

            if (!ObjectsCompat.equals(this.getResolutionStrategy(), that.getResolutionStrategy())) {
                return false;
            }

            return ObjectsCompat.equals(this.getCustomModel(), that.getCustomModel());
        }

        @Override
        public int hashCode() {
            int result = getResolutionStrategy().hashCode();
            result = 31 * result + (getCustomModel() != null ? getCustomModel().hashCode() : 0);
            return result;
        }

        @NonNull
        @Override
        public String toString() {
            return "ConflictResolutionDecision{" +
                "resolutionStrategy=" + resolutionStrategy +
                ", customModel=" + customModel +
                '}';
        }
    }
}
