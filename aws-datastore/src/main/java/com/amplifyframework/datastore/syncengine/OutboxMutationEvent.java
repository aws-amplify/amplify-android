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
import com.amplifyframework.datastore.DataStoreChannelEventName;
import com.amplifyframework.datastore.appsync.ModelMetadata;
import com.amplifyframework.datastore.appsync.ModelWithMetadata;

import java.util.Objects;

/**
 * Event payload for the {@link DataStoreChannelEventName#OUTBOX_MUTATION_ENQUEUED} event
 * and {@link DataStoreChannelEventName#OUTBOX_MUTATION_PROCESSED} event.
 * @param <M> The class type of the model in the mutation outbox.
 */
public final class OutboxMutationEvent<M extends Model> {
    private final Class<M> model;
    private final ModelWithMetadata<M> element;

    private OutboxMutationEvent(Class<M> model, ModelWithMetadata<M> element) {
        this.model = model;
        this.element = element;
    }

    /**
     * Constructs an outbox mutation event with just the model. The resulting
     * event payload will not contain model sync metadata.
     * This format will be used for representing a pending mutation that has
     * been successfully enqueued into the outbox.
     * @param model Enqueued model.
     * @param <M> Class type of the model.
     * @return Outbox mutation event without sync metadata.
     */
    @NonNull
    public static <M extends Model> OutboxMutationEvent<M> fromModel(@NonNull M model) {
        Objects.requireNonNull(model);
        final ModelMetadata dummyMetadata = new ModelMetadata(
                model.getId(),
                null,
                null,
                null
        );
        return fromModelWithMetadata(new ModelWithMetadata<>(model, dummyMetadata));
    }

    /**
     * Constructs an outbox mutation event containing both the model and its
     * sync metadata.
     * This format will be used for representing a pending mutation that has
     * successfully undergone cloud publication.
     * @param modelWithMetadata Processed model with its sync metadata.
     * @param <M> Class type of the model.
     * @return Outbox mutation event with sync metadata.
     */
    @NonNull
    public static <M extends Model> OutboxMutationEvent<M> fromModelWithMetadata(
            @NonNull ModelWithMetadata<M> modelWithMetadata
    ) {
        Objects.requireNonNull(modelWithMetadata);
        final M model = modelWithMetadata.getModel();
        @SuppressWarnings("unchecked") // model's class will always be of type Class<M>
        final Class<M> modelType = (Class<M>) model.getClass();
        return new OutboxMutationEvent<>(modelType, modelWithMetadata);
    }

    /**
     * Returns the class type of the model being mutated.
     * @return the model class type.
     */
    @NonNull
    public Class<M> getModel() {
        return model;
    }

    /**
     * Returns the element of the model being mutated. The element
     * will contain the contents of the model as well as its sync
     * metadata if relevant.
     * @return the model element.
     */
    @NonNull
    public ModelWithMetadata<M> getElement() {
        return element;
    }

    @Override
    public int hashCode() {
        int result = model.hashCode();
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

        return ObjectsCompat.equals(model, that.model) &&
                ObjectsCompat.equals(element, that.element);
    }

    @NonNull
    @Override
    public String toString() {
        return "OutboxMutationEvent{" +
                "model='" + model + '\'' +
                ", element='" + element + '\'' +
                '}';
    }
}
