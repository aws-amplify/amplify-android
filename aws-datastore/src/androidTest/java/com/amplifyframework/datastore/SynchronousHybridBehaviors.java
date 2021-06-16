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

package com.amplifyframework.datastore;

import androidx.annotation.NonNull;

import com.amplifyframework.core.model.SerializedModel;
import com.amplifyframework.core.model.query.Where;
import com.amplifyframework.testutils.sync.SynchronousDataStore;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.core.Single;

/**
 * This is a wrapper to make the Hybrid APIs synchronous, which is convenient
 * in our test code.
 *
 * What {@link SynchronousDataStore} is to {@link DataStoreCategoryBehavior},
 * this {@link SynchronousHybridBehaviors} is to the hybrid-platform functionality
 * that exists in {@link AWSDataStorePlugin}.
 *
 * The hybrid functionality isn't specified in core, only in the AWSDataStorePlugin
 * in aws-datastore. testutils should only depend on core, not on aws-datastore. As
 * a result, we have to put this utility in aws-datastore, not alongside
 * its similar {@link SynchronousDataStore}.
 */
public final class SynchronousHybridBehaviors {
    private static final long TIMEOUT_SECONDS = 5;

    private final AWSDataStorePlugin delegate;

    private SynchronousHybridBehaviors(AWSDataStorePlugin delegate) {
        this.delegate = delegate;
    }

    /**
     * Creates an {@link SynchronousHybridBehaviors} instance that delegates to the provided
     * {@link AWSDataStorePlugin}.
     * @param delegate A plugin that will execute the behaviors in an async way; this
     *                 utility will await completion of the async behaviors
     * @return An {@link SynchronousHybridBehaviors} instance
     */
    @NonNull
    public static SynchronousHybridBehaviors delegatingTo(@NonNull AWSDataStorePlugin delegate) {
        return new SynchronousHybridBehaviors(delegate);
    }

    /**
     * Saves serialized models.
     * @param models A variable argument list of serialized models to save
     */
    public void save(SerializedModel... models) {
        boolean ok = Observable.fromArray(models)
            .flatMapSingle(serializedModel ->
                Single.create(emitter ->
                    delegate.save(serializedModel, emitter::onSuccess, emitter::onError)
                )
            )
            .ignoreElements()
            .blockingAwait(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        if (!ok) {
            throw new RuntimeException("Failed to save serialized models = " + Arrays.toString(models));
        }
    }

    /**
     * Deletes serialized models.
     * @param models A variable argument list of serialized models to delete
     */
    public void delete(SerializedModel... models) {
        boolean ok = Observable.fromArray(models)
            .flatMapSingle(serializedModel ->
                Single.create(emitter ->
                    delegate.delete(serializedModel, emitter::onSuccess, emitter::onError)
                )
            )
            .ignoreElements()
            .blockingAwait(TIMEOUT_SECONDS, TimeUnit.SECONDS);
        if (!ok) {
            throw new RuntimeException("Failed to delete serialized models: " + Arrays.toString(models));
        }
    }

    /**
     * Lists all SerializedModels associated with the given model names.
     * @param modelNames A variable-argument list of model names, e.g., "Post", "Comment", etc.
     * @return A list of serialized models associated to the requested model names
     */
    public List<SerializedModel> list(String... modelNames) {
        return Observable.fromArray(modelNames)
            .flatMap(modelName ->
                Observable.<SerializedModel>create(emitter ->
                    delegate.query(modelName, Where.matchesAll(), iterator -> {
                        while (iterator.hasNext()) {
                            emitter.onNext((SerializedModel) iterator.next());
                        }
                        emitter.onComplete();
                    }, emitter::onError)
                )
            )
            .toList()
            .timeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .blockingGet();
    }
}
