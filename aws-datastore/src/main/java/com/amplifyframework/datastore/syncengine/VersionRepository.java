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

import com.amplifyframework.core.model.Model;
import com.amplifyframework.core.model.query.predicate.QueryField;
import com.amplifyframework.core.model.query.predicate.QueryPredicate;
import com.amplifyframework.datastore.DataStoreException;
import com.amplifyframework.datastore.appsync.ModelMetadata;
import com.amplifyframework.datastore.storage.LocalStorageAdapter;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

import io.reactivex.Single;

/**
 * The VersionRepository provides a higher-level facade to lookup the version
 * of the various models in the local storage.
 */
@SuppressWarnings("CodeBlock2Expr")
final class VersionRepository {
    private final LocalStorageAdapter localStorageAdapter;

    /**
     * Constructs a new VersionRepository.
     * @param localStorageAdapter A local storage repository, where model metadata can be found
     */
    VersionRepository(@NonNull LocalStorageAdapter localStorageAdapter) {
        this.localStorageAdapter = Objects.requireNonNull(localStorageAdapter);
    }

    /**
     * Find the current version of a model, that we have in the local store.
     * @param model A model
     * @param <T> Type of model
     * @return Current version known locally
     */
    <T extends Model> Single<Integer> findModelVersion(T model) {
        // The ModelMetadata for the model uses the same ID as an identifier.
        final QueryPredicate hasMatchingId = QueryField.field("id").eq(model.getId());
        return Single.create(emitter -> {
            localStorageAdapter.query(ModelMetadata.class, hasMatchingId, iterableResults -> {
                try {
                    emitter.onSuccess(extractVersion(model, iterableResults));
                } catch (DataStoreException badVersionFailure) {
                    emitter.onError(badVersionFailure);
                }
            }, emitter::onError);
        });
    }

    /**
     * Extract a model version from an metadata iterator.
     * @param model The model for which metadata is being interrogated, used only for creating error messages.
     * @param metadataIterator An iterator of ModelMetadata; the metadata is associated with the provided model
     * @param <T> The type of model
     * @return The version of the model, if available
     * @throws DataStoreException If there is no version for the model, or if the version cannot be obtained
     */
    private <T extends Model> int extractVersion(T model, Iterator<ModelMetadata> metadataIterator)
            throws DataStoreException {
        final List<ModelMetadata> results = new ArrayList<>();
        while (metadataIterator.hasNext()) {
            results.add(metadataIterator.next());
        }

        // There should be only one metadata for the model....
        if (results.size() != 1) {
            throw new DataStoreException(
                "Wanted 1 metadata for item with id = " + model.getId() + ", but had " + results.size() + ".",
                "This is likely a bug. please report to AWS."
            );
        }
        final Integer version = results.get(0).getVersion();
        if (version == null) {
            throw new DataStoreException(
                "Metadata for item with id = " + model.getId() + " had null version.",
                "This is likely a bug. Please report to AWS."
            );
        }

        return version;
    }
}
