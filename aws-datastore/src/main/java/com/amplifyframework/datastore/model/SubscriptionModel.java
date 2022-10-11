package com.amplifyframework.datastore.model;

import com.amplifyframework.api.graphql.SubscriptionType;
import com.amplifyframework.core.model.ModelSchema;
import java.util.Objects;

public class SubscriptionModel {
    private ModelSchema modelSchema;
    private SubscriptionType subscriptionType;

    public SubscriptionModel(ModelSchema modelSchema, SubscriptionType subscriptionType) {
        this.modelSchema = modelSchema;
        this.subscriptionType = subscriptionType;
    }

    public ModelSchema getModelSchema() {
        return modelSchema;
    }

    public SubscriptionType getSubscriptionType() {
        return subscriptionType;
    }

    @Override
    public String toString() {
        return "SubscriptionModel{" +
                "modelCls=" + modelSchema +
                ", subscriptionType=" + subscriptionType +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SubscriptionModel that = (SubscriptionModel) o;
        return Objects.equals(modelSchema, that.modelSchema) &&
                subscriptionType == that.subscriptionType;
    }

    @Override
    public int hashCode() {
        return Objects.hash(modelSchema, subscriptionType);
    }
}