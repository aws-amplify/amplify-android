package com.amplifyframework.datastore;

import androidx.annotation.NonNull;

import com.amplifyframework.core.model.ModelProvider;
import com.amplifyframework.datastore.model.SubscriptionModel;
import com.amplifyframework.datastore.syncengine.Orchestrator;

import java.util.Set;
import java.util.function.Supplier;

/**
 * Just a ~type-alias for a consumer of DataStoreException.
 */
public interface DataStoreSubscriptionsSupplier {

    Set<SubscriptionModel>  getSubscriptions(@NonNull ModelProvider modelProvider);
}