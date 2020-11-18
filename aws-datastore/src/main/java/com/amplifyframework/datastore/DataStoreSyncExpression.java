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

import com.amplifyframework.core.Action;
import com.amplifyframework.core.Consumer;
import com.amplifyframework.core.model.Model;
import com.amplifyframework.core.model.query.predicate.QueryPredicate;

/**
 * Used to specify a QueryPredicate for a {@link Model} that will filter what is synced to the client.  A DataStore
 * customer should provide their implementation to the {@link DataStoreConfiguration} while
 * constructing the DataStore plugin using {@link AWSDataStorePlugin#AWSDataStorePlugin(DataStoreConfiguration)}.
 */
public interface DataStoreSyncExpression {
    /**
     * This will be called each time DataStore is started.  This allows the QueryPredicate to be modified at runtime,
     * by calling {@link DataStoreCategoryBehavior#stop(Action, Consumer)} followed by
     * {@link DataStoreCategoryBehavior#start(Action, Consumer)}.
     * @return QueryPredicate to filter what is synced.
     */
    QueryPredicate resolvePredicate();
}
