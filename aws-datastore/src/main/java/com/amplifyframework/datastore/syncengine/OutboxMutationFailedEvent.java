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

import com.amplifyframework.AmplifyException;
import com.amplifyframework.api.graphql.GraphQLResponse;
import com.amplifyframework.api.graphql.MutationType;
import com.amplifyframework.core.model.Model;
import com.amplifyframework.datastore.DataStoreChannelEventName;
import com.amplifyframework.datastore.DataStoreException;
import com.amplifyframework.hub.HubEvent;
import com.amplifyframework.util.Immutable;

import java.util.List;
import java.util.Objects;

/**
 * Event payload for the {@link DataStoreChannelEventName#OUTBOX_MUTATION_FAILED} event.
 * @param <M> The class type of the model in the mutation outbox.
 */
public final class OutboxMutationFailedEvent<M extends Model>
        implements HubEvent.Data<OutboxMutationFailedEvent<M>> {
    private final List<GraphQLResponse.Error> errors;
    private final MutationType operation;
    private final M model;

    private OutboxMutationFailedEvent(
        List<GraphQLResponse.Error> errors,
        MutationType operation,
        M model
    ) {
        this.errors = errors;
        this.operation = operation;
        this.model = model;
    }

    /**
     * Constructs an outbox mutation error event.
     * @param pendingMutation pending mutation that failed to publish
     * @param errors the list of graphQL errors that caused the failure
     * @param <M> Class type of the model.
     * @return Outbox mutation error event.
     * @throws DataStoreException if unexpected mutation type
     */
    @NonNull
    public static <M extends Model> OutboxMutationFailedEvent<M> create(
            @NonNull PendingMutation<M> pendingMutation,
            @NonNull List<GraphQLResponse.Error> errors
    ) throws DataStoreException {
        Objects.requireNonNull(pendingMutation);
        Objects.requireNonNull(errors);
        final MutationType opType;
        switch (pendingMutation.getMutationType()) {
            case CREATE:
                opType = MutationType.CREATE;
                break;
            case UPDATE:
                opType = MutationType.UPDATE;
                break;
            case DELETE:
                opType = MutationType.DELETE;
                break;
            default:
                throw new DataStoreException(
                    "Invalid operation type.",
                    AmplifyException.REPORT_BUG_TO_AWS_SUGGESTION
                );
        }
        return new OutboxMutationFailedEvent<>(errors, opType, pendingMutation.getMutatedItem());
    }

    /**
     * Returns the list of graphQL errors that caused publication failure.
     * @return the list of graphQL error responses.
     */
    @NonNull
    public List<GraphQLResponse.Error> getErrors() {
        return Immutable.of(errors);
    }

    /**
     * Gets the graphQL mutation type.
     * @return the mutation type
     */
    @NonNull
    public MutationType getOperation() {
        return operation;
    }

    /**
     * Gets the local model that failed to be mutated.
     * @return the local model
     */
    @NonNull
    public M getModel() {
        return model;
    }

    @Override
    public HubEvent<OutboxMutationFailedEvent<M>> toHubEvent() {
        return HubEvent.create(DataStoreChannelEventName.OUTBOX_MUTATION_FAILED, this);
    }
}
