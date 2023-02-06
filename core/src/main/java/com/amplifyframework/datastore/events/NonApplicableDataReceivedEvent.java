package com.amplifyframework.datastore.events;

import com.amplifyframework.api.graphql.GraphQLResponse;
import com.amplifyframework.datastore.DataStoreChannelEventName;
import com.amplifyframework.hub.HubEvent;

import java.util.List;

public class NonApplicableDataReceivedEvent implements HubEvent.Data<NonApplicableDataReceivedEvent> {
    private final List<GraphQLResponse.Error> errors;
    private final String model;

    public NonApplicableDataReceivedEvent(List<GraphQLResponse.Error> errors, String model) {
        this.errors = errors;
        this.model = model;
    }

    public List<GraphQLResponse.Error> getErrors() {
        return errors;
    }

    public String getModel() {
        return model;
    }

    @Override
    public HubEvent<NonApplicableDataReceivedEvent> toHubEvent() {
        return HubEvent.create(DataStoreChannelEventName.NON_APPLICABLE_DATA_RECEIVED, this);
    }

    @Override
    public String toString() {
        return "NonApplicableDataReceivedEvent{" +
                "errors=" + errors +
                ", model='" + model + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        NonApplicableDataReceivedEvent that = (NonApplicableDataReceivedEvent) o;

        if (errors != null ? !errors.equals(that.errors) : that.errors != null) return false;
        return model != null ? model.equals(that.model) : that.model == null;
    }

    @Override
    public int hashCode() {
        int result = errors != null ? errors.hashCode() : 0;
        result = 31 * result + (model != null ? model.hashCode() : 0);
        return result;
    }
}
