/*
 * Copyright 2023 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package com.amplifyframework.datastore.events;

import com.amplifyframework.api.graphql.GraphQLResponse;
import com.amplifyframework.datastore.DataStoreChannelEventName;
import com.amplifyframework.hub.HubEvent;

import java.util.List;
import java.util.Objects;

/**
 * Event payload for the {@link DataStoreChannelEventName#NON_APPLICABLE_DATA_RECEIVED} event.
 */
public final class NonApplicableDataReceivedEvent implements HubEvent.Data<NonApplicableDataReceivedEvent> {
    private final List<GraphQLResponse.Error> errors;
    private final String model;

    /**
     * Constructs a {@link NonApplicableDataReceivedEvent} object.
     * @param errors The list of errors.
     * @param model The model.
     */
    public NonApplicableDataReceivedEvent(List<GraphQLResponse.Error> errors, String model) {
        this.errors = errors;
        this.model = model;
    }

    /**
     * Getter for the errors field.
     * @return The value of the error field.
     */
    public List<GraphQLResponse.Error> getErrors() {
        return errors;
    }

    /**
     * Getter for the model field.
     * @return The value of the model field.
     */
    public String getModel() {
        return model;
    }

    @Override
    public HubEvent<NonApplicableDataReceivedEvent> toHubEvent() {
        return HubEvent.create(DataStoreChannelEventName.NON_APPLICABLE_DATA_RECEIVED, this);
    }

    @Override
    public String toString() {
        return "NonApplicableDataReceivedEvent{" + "errors=" + errors + ", model='" + model + '\'' + '}';
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }

        NonApplicableDataReceivedEvent that = (NonApplicableDataReceivedEvent) obj;

        if (!Objects.equals(errors, that.errors)) {
            return false;
        }
        return Objects.equals(model, that.model);
    }

    @Override
    public int hashCode() {
        int result = errors != null ? errors.hashCode() : 0;
        result = 31 * result + (model != null ? model.hashCode() : 0);
        return result;
    }
}
