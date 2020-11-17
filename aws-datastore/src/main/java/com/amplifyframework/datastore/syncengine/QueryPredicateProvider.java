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
import androidx.core.util.Pair;

import com.amplifyframework.core.model.Model;
import com.amplifyframework.core.model.query.predicate.QueryPredicate;
import com.amplifyframework.core.model.query.predicate.QueryPredicates;
import com.amplifyframework.datastore.DataStoreConfiguration;
import com.amplifyframework.datastore.DataStoreConfigurationProvider;
import com.amplifyframework.datastore.DataStoreException;
import com.amplifyframework.datastore.DataStoreSyncExpression;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import io.reactivex.rxjava3.core.Observable;

/**
 * Maintains a Map of QueryPredicates for each {@link Model} for the current DataStore session.
 */
final class QueryPredicateProvider {
    private final DataStoreConfigurationProvider dataStoreConfigurationProvider;
    private final Map<String, QueryPredicate> predicateMap = new HashMap<>();

    /**
     * Constructs a QueryPredicateProvider.
     * @param dataStoreConfigurationProvider a DataStoreConfigurationProvider.
     */
    QueryPredicateProvider(DataStoreConfigurationProvider dataStoreConfigurationProvider) {
        this.dataStoreConfigurationProvider = dataStoreConfigurationProvider;
    }

    /**
     * Evaluates any client provided {@link DataStoreSyncExpression}s and caches the resolved {@link QueryPredicate}s
     * for use in the current DataStore session. These are used to filter data received from AppSync, either during a
     * sync or over the real-time subscription.  This is called once each time DataStore is started.
     *
     * @throws DataStoreException on error obtaining the {@link DataStoreConfiguration}.
     */
    public void resolvePredicates() throws DataStoreException {
        Map<String, DataStoreSyncExpression> expressions =
                dataStoreConfigurationProvider.getConfiguration().getSyncExpressions();
        predicateMap.clear();
        predicateMap.putAll(Observable.fromIterable(expressions.entrySet())
                .map(entry -> Pair.create(entry.getKey(), entry.getValue().resolvePredicate()))
                .toMap(pair -> pair.first, pair -> pair.second)
                .blockingGet());
    }

    /**
     * Returns the {@link QueryPredicate} for the given modelName.
     * @param modelName name of the {@link Model}.
     * @return the {@link QueryPredicate} for the given modelName.
     */
    @NonNull
    public QueryPredicate getPredicate(@NonNull String modelName) {
        QueryPredicate predicate = predicateMap.get(Objects.requireNonNull(modelName));
        if (predicate == null) {
            predicate = QueryPredicates.all();
        }
        return predicate;
    }
}
