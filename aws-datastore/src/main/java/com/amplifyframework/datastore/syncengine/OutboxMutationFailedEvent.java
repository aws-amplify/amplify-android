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

import com.amplifyframework.api.graphql.GraphQLResponse;
import com.amplifyframework.api.graphql.MutationType;
import com.amplifyframework.core.model.Model;
import com.amplifyframework.datastore.DataStoreChannelEventName;
import com.amplifyframework.datastore.appsync.AppSyncExtensions;
import com.amplifyframework.hub.HubEvent;

import java.util.Iterator;
import java.util.List;
import java.util.Objects;

/**
 * Event payload for the {@link DataStoreChannelEventName#OUTBOX_MUTATION_FAILED} event.
 * @param <M> The class type of the model in the mutation outbox.
 */
public final class OutboxMutationFailedEvent<M extends Model>
        implements HubEvent.Data<OutboxMutationFailedEvent<M>> {
    private final MutationErrorType errorType;
    private final MutationType operation;
    private final M model;

    private OutboxMutationFailedEvent(
            MutationErrorType errorType,
            MutationType operation,
            M model
    ) {
        this.errorType = errorType;
        this.operation = operation;
        this.model = model;
    }

    /**
     * Constructs an outbox mutation error event.
     * @param pendingMutation pending mutation that failed to publish
     * @param errors the list of graphQL errors that caused the failure
     * @param <M> Class type of the model.
     * @return Outbox mutation error event.
     */
    @NonNull
    public static <M extends Model> OutboxMutationFailedEvent<M> create(
            @NonNull PendingMutation<M> pendingMutation,
            @NonNull List<GraphQLResponse.Error> errors
    ) {
        Objects.requireNonNull(pendingMutation);
        Objects.requireNonNull(errors);
        MutationErrorType errorType = MutationErrorType.fromGraphQLErrors(errors);
        String operation = pendingMutation.getMutationType().name();
        MutationType opType = MutationType.valueOf(operation);
        return new OutboxMutationFailedEvent<>(errorType, opType, pendingMutation.getMutatedItem());
    }

    /**
     * Returns the type of error that caused mutation publication to fail.
     * @return the type of error from the cloud.
     */
    @NonNull
    public MutationErrorType getErrorType() {
        return errorType;
    }

    /**
     * Gets the mutation type.
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

    /**
     * Gets the name of the model.
     * @return the model name
     */
    @NonNull
    public String getModelName() {
        return model.getClass().getSimpleName();
    }

    @Override
    public HubEvent<OutboxMutationFailedEvent<M>> toHubEvent() {
        return HubEvent.create(DataStoreChannelEventName.OUTBOX_MUTATION_FAILED, this);
    }

    @Override
    public String toString() {
        return "OutboxMutationFailedEvent{" +
                "errorType=" + errorType +
                ", operation=" + operation +
                ", model=" + model +
                ", modelName=" + getModelName() +
                '}';
    }

    /**
     * Categorization of error types that caused a mutation publication to fail.
     */
    public enum MutationErrorType {
        /**
         * The mutation operation is not authorized for the user.
         */
        UNAUTHORIZED("Unauthorized"),

        /**
         * Fallback type for any error that is yet to be categorized.
         */
        UNKNOWN("Unknown");

        private final String value;

        MutationErrorType(String value) {
            this.value = value;
        }

        /**
         * Get the value of error type received from cloud.
         * @return value of error type received from cloud
         */
        @NonNull
        public String getValue() {
            return value;
        }

        /**
         * Returns an enum value of matching error type value.
         * @param value the type of error that caused mutation publication failure
         * @return An enum value of matching error type. If there is no match,
         *          then return {@link MutationErrorType#UNKNOWN}.
         */
        @NonNull
        public static MutationErrorType fromErrorType(@Nullable String value) {
            try {
                return MutationErrorType.valueOf(value);
            } catch (IllegalArgumentException exception) {
                return UNKNOWN;
            }
        }

        // Look at the first error to extract error type.
        private static MutationErrorType fromGraphQLErrors(List<GraphQLResponse.Error> errors) {
            Iterator<GraphQLResponse.Error> iterator = errors.iterator();
            if (iterator.hasNext()) {
                GraphQLResponse.Error firstError = iterator.next();
                AppSyncExtensions extensions = new AppSyncExtensions(firstError.getExtensions());
                return fromErrorType(extensions.getErrorType().getValue());
            }
            return UNKNOWN;
        }
    }
}
