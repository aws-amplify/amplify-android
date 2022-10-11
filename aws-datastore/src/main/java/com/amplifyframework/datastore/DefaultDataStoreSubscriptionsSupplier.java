package com.amplifyframework.datastore;

import androidx.annotation.NonNull;

import com.amplifyframework.api.graphql.SubscriptionType;
import com.amplifyframework.core.model.Model;
import com.amplifyframework.core.model.ModelProvider;
import com.amplifyframework.core.model.ModelSchema;
import com.amplifyframework.datastore.model.SubscriptionModel;

import java.util.HashSet;
import java.util.Set;


public final class DefaultDataStoreSubscriptionsSupplier implements DataStoreSubscriptionsSupplier {

    @NonNull
    public static DefaultDataStoreSubscriptionsSupplier instance() {
        return new DefaultDataStoreSubscriptionsSupplier();
    }

    @Override
    public Set<SubscriptionModel>  getSubscriptions(@NonNull ModelProvider modelProvider) {
        Set<SubscriptionModel> subscriptions = new HashSet<>();
        for (ModelSchema modelSchema : modelProvider.modelSchemas().values()) {
            for (SubscriptionType subscriptionType : SubscriptionType.values()) {
                subscriptions.add(new SubscriptionModel(modelSchema, subscriptionType));
            }
        }
        return subscriptions;
    }
}