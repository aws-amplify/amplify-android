package com.amplifyframework.datastore;

import androidx.annotation.NonNull;

import com.amplifyframework.datastore.syncengine.Orchestrator;

/**
 * A default implementation of the {@link DataStoreErrorHandler} which just logs the error
 * and moves on.
 */
public final class DefaultDataStoreTargetStateSupplier implements DataStoreTargetStateSupplier {

    private DefaultDataStoreTargetStateSupplier() {}

    @NonNull
    public static DefaultDataStoreTargetStateSupplier instance() {
        return new DefaultDataStoreTargetStateSupplier();
    }

    @Override
    public Orchestrator.State get() {
        return Orchestrator.State.SYNC_VIA_API;
    }
}