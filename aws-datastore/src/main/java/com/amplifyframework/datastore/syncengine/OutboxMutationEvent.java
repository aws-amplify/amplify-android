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
import com.amplifyframework.core.model.temporal.Temporal;
import com.amplifyframework.datastore.DataStoreChannelEventName;
import com.amplifyframework.datastore.appsync.ModelMetadata;
import com.amplifyframework.datastore.appsync.ModelWithMetadata;
import com.amplifyframework.hub.HubEvent;

import java.util.Objects;

/**
 * Event payload for the {@link DataStoreChannelEventName#OUTBOX_MUTATION_ENQUEUED} event
 * and {@link DataStoreChannelEventName#OUTBOX_MUTATION_PROCESSED} event.
 * @param <M> The class type of the model in the mutation outbox.
 */
public final class OutboxMutationEvent<M extends Model>
        implements HubEvent.Data<OutboxMutationEvent<M>> {
    private final String modelName;
    private final OutboxMutationEventElement<M> element;

    private OutboxMutationEvent(String modelName, OutboxMutationEventElement<M> element) {
        this.modelName = modelName;
        this.element = element;
    }

    /**
     * Constructs an outbox mutation event from a pending mutation.
     * This format will be used for representing a pending mutation that has
     * been successfully enqueued into the outbox.
     * @param pendingMutation Enqueued model.
     * @param <M> Class type of the model.
     * @return An OutboxMutationEvent
     */
    @NonNull
    public static <M extends Model> OutboxMutationEvent<M> fromPendingMutation(
            @NonNull PendingMutation<M> pendingMutation) {
        Objects.requireNonNull(pendingMutation);
        OutboxMutationEventElement<M> element =
            new OutboxMutationEventElement<>(pendingMutation.getMutatedItem(), null, null, null);
        return new OutboxMutationEvent<>(pendingMutation.getModelSchema().getName(), element);
    }

    /**
     * Constructs an outbox mutation event containing both the model and its
     * sync metadata.
     * This format will be used for representing a pending mutation that has
     * successfully undergone cloud publication.
     * @param modelName Name of the model that has been processed (e.g., "Blog".)
     * @param modelWithMetadata Processed model with its sync metadata.
     * @param <M> Class type of the model.
     * @return Outbox mutation event with sync metadata.
     */
    @NonNull
    public static <M extends Model> OutboxMutationEvent<M> create(
            @NonNull String modelName, @NonNull ModelWithMetadata<M> modelWithMetadata) {
        Objects.requireNonNull(modelName);
        Objects.requireNonNull(modelWithMetadata);

        M model = modelWithMetadata.getModel();
        ModelMetadata metadata = modelWithMetadata.getSyncMetadata();

        Integer version = metadata.getVersion();
        Temporal.Timestamp lastChangedAt = metadata.getLastChangedAt();
        Boolean deleted = metadata.isDeleted();

        OutboxMutationEventElement<M> element =
            new OutboxMutationEventElement<>(model, version, lastChangedAt, deleted);

        return new OutboxMutationEvent<>(modelName, element);
    }

    /**
     * Returns the name of the model being mutated.
     * @return the model name.
     */
    @NonNull
    public String getModelName() {
        return modelName;
    }

    /**
     * Returns the element of the model being mutated. The element
     * will contain the contents of the model as well as its sync
     * metadata if relevant.
     * @return the model element.
     */
    @NonNull
    public OutboxMutationEventElement<M> getElement() {
        return element;
    }

    @Override
    public int hashCode() {
        int result = modelName.hashCode();
        result = 31 * result + element.hashCode();
        return result;
    }

    @Override
    public boolean equals(@Nullable Object thatObject) {
        if (this == thatObject) {
            return true;
        }
        if (thatObject == null || getClass() != thatObject.getClass()) {
            return false;
        }

        OutboxMutationEvent<?> that = (OutboxMutationEvent<?>) thatObject;

        return ObjectsCompat.equals(modelName, that.modelName) &&
                ObjectsCompat.equals(element, that.element);
    }

    @NonNull
    @Override
    public String toString() {
        return "OutboxMutationEvent{" +
                "modelName='" + modelName + '\'' +
                ", element='" + element + '\'' +
                '}';
    }

    @Override
    public HubEvent<OutboxMutationEvent<M>> toHubEvent() {
        if (getElement().getVersion() == null) {
            return HubEvent.create(DataStoreChannelEventName.OUTBOX_MUTATION_ENQUEUED, this);
        }
        return HubEvent.create(DataStoreChannelEventName.OUTBOX_MUTATION_PROCESSED, this);
    }

    /**
     * An element representing the data that changed in this outbox event.
     * @param <M> A model type
     */
    public static final class OutboxMutationEventElement<M extends Model> {
        private final M model;
        private final Integer version;
        private final Temporal.Timestamp lastChangedAt;
        private final Boolean deleted;

        private OutboxMutationEventElement(
                M model, Integer version, Temporal.Timestamp lastChangedAt, Boolean deleted) {
            this.model = model;
            this.version = version;
            this.lastChangedAt = lastChangedAt;
            this.deleted = deleted;
        }

        /**
         * Checks if the model has been deleted.
         * @return True if the model is deleted, now.
         */
        @Nullable
        public Boolean isDeleted() {
            return deleted;
        }

        /**
         * The last time the model was updated locally, if available.
         * @return Last time the model was updated locally
         */
        @Nullable
        public Temporal.Timestamp getLastChangedAt() {
            return lastChangedAt;
        }

        /**
         * The version of the {@link #getModel()}, if available.
         * @return The version of the model if available
         */
        @Nullable
        public Integer getVersion() {
            return version;
        }

        /**
         * Gets the model on which the outbox event occurred.
         * @return Model that is the subject of an outbox event
         */
        @NonNull
        public M getModel() {
            return model;
        }

        @NonNull
        @Override
        public String toString() {
            return "OutboxMutationEventElement{" +
                "model=" + model +
                ", version=" + version +
                ", lastChangedAt=" + lastChangedAt +
                ", deleted=" + deleted +
                '}';
        }

        @Override
        public boolean equals(@Nullable Object thatObject) {
            if (this == thatObject) {
                return true;
            }
            if (thatObject == null || getClass() != thatObject.getClass()) {
                return false;
            }
            OutboxMutationEventElement<?> that = (OutboxMutationEventElement<?>) thatObject;
            return getModel().equals(that.getModel()) &&
                ObjectsCompat.equals(getVersion(), that.getVersion()) &&
                ObjectsCompat.equals(getLastChangedAt(), that.getLastChangedAt()) &&
                ObjectsCompat.equals(isDeleted(), that.isDeleted());
        }

        @Override
        public int hashCode() {
            return ObjectsCompat.hash(getModel(), getVersion(), getLastChangedAt(), isDeleted());
        }
    }
}
