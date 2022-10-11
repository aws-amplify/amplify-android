package com.amplifyframework.datastore;

import androidx.annotation.NonNull;

import com.amplifyframework.core.model.ModelProvider;
import com.amplifyframework.core.model.ModelSchema;

import java.util.Map;


public final class DefaultDataStoreSyncSupplier implements DataStoreSyncSupplier{

    @NonNull
    public static DefaultDataStoreSyncSupplier instance() {
        return new DefaultDataStoreSyncSupplier();
    }


    @Override
    public Map<String, ModelSchema> getModels(@NonNull ModelProvider modelProvider) {
        return modelProvider.modelSchemas();
    }
}