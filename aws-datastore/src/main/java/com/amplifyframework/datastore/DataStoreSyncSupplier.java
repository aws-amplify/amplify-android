package com.amplifyframework.datastore;

import androidx.annotation.NonNull;

import com.amplifyframework.core.model.ModelProvider;
import com.amplifyframework.core.model.ModelSchema;

import java.util.Map;

/**
 * Just a ~type-alias for a consumer of DataStoreException.
 */
public interface DataStoreSyncSupplier {

    Map<String, ModelSchema> getModels(@NonNull ModelProvider modelProvider);
}