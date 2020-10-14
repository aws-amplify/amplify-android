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
import androidx.annotation.Nullable;
import androidx.core.util.ObjectsCompat;

import com.amplifyframework.core.model.Model;

/**
 * The possible decisions for a conflict resolution. Either we will try to apply the remote changes
 * to the local store, OR we will try to update the remote store using the local version of the model
 * OR we will retry a mutation against the remote store by using a custom version of the model object,
 * distinct from both the current local and remote versions.
 * @param <T> Type of model experiencing conflict
 */
public final class ConflictResolutionDecision<T extends Model> {
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
     * @param customModel Resolve the conflict by using this custom model instance,
     *                    If null, it means to delete the model instance
     * @param <T> Type of model experiencing a conflict
     * @return A decision to retry mutation with the provided model
     */
    @NonNull
    public static <T extends Model> ConflictResolutionDecision<T> retry(@Nullable T customModel) {
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
     * this returns the custom model with which to retry.
     *
     * Note that this value may be null for two reasons:
     *   1. The strategy is not RETRY, or;
     *   2. The strategy *is* RETRY, but the intent is to delete the model instance.
     *
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

    /**
     * An enumeration of the various strategies available for dealing with a model conflict.
     */
    public enum ResolutionStrategy {
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
        RETRY;
    }
}
