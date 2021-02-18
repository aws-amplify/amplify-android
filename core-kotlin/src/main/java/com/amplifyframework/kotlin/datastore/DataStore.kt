/*
 * Copyright 2021 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package com.amplifyframework.kotlin.datastore

import com.amplifyframework.core.model.Model
import com.amplifyframework.core.model.query.QueryOptions
import com.amplifyframework.core.model.query.Where
import com.amplifyframework.core.model.query.predicate.QueryPredicate
import com.amplifyframework.core.model.query.predicate.QueryPredicates
import com.amplifyframework.datastore.DataStoreException
import com.amplifyframework.datastore.DataStoreItemChange
import kotlin.reflect.KClass
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow

/**
 * A local object store with cloud synchronization.
 */
interface DataStore {
    /**
     * Save an item into the DataStore.
     * @param item Item to save
     * @param predicate Conditions that must be true before save can succeed.
     *                  If not specified, an "allow all" predicate is used.
     */
    @Throws(DataStoreException::class)
    suspend fun <T : Model> save(item: T, predicate: QueryPredicate = QueryPredicates.all())

    /**
     * Delete an item from the DataStore.
     * @param item Item to delete
     * @param predicate Conditions that must be true before delete can succeed.
     *                  If not specified, an "allow all" predicate is used.
     */
    @Throws(DataStoreException::class)
    suspend fun <T : Model> delete(item: T, predicate: QueryPredicate = QueryPredicates.all())

    /**
     * Delete item(s) of a given class from the DataStore.
     * @param byClass The class of item(s) being deleted
     * @param filter Items must additionally match this filter, to be targeted for deletion.
     *               If no filter is specified, an "allow all" predicate is used.
     */
    @Throws(DataStoreException::class)
    suspend fun <T : Model> delete(
        byClass: KClass<T>,
        filter: QueryPredicate = QueryPredicates.all()
    )

    /**
     * Query the DataStore for items meeting certain criteria.
     * @param itemClass Class of item to query
     * @param options Additional search filter to match items;
     *                if not provided, a "match all" option is used by default
     * @return A flow of items matching the search criteria
     */
    @ExperimentalCoroutinesApi
    @Throws(DataStoreException::class)
    fun <T : Model> query(itemClass: Class<T>, options: QueryOptions = Where.matchesAll()): Flow<T>

    /**
     * Observe all changes to items in the DataStore.
     * @return A flow of changes to the items in the DataStore
     */
    @ExperimentalCoroutinesApi
    @Throws(DataStoreException::class)
    fun observe(): Flow<DataStoreItemChange<out Model>>

    /**
     * Observe the DataStore for changes to a particular item.
     * @param itemClass The class of the model being observed
     * @param uniqueId The ID of the item being observed
     * @return A flow of changes to the requested model
     */
    @ExperimentalCoroutinesApi
    @Throws(DataStoreException::class)
    fun <T : Model> observe(itemClass: Class<T>, uniqueId: String): Flow<DataStoreItemChange<T>>

    /**
     * Observe the DataStore for changes to a particular type of model
     * where the items also match some additional search criteria.
     * @param itemClass Class of item being observed
     * @param selectionCriteria Only observe items meeting this criteria.
     *                          If not provided, a match-all predicate is used by default.
     * @return A flow of changes to the matched item(s)
     */
    @ExperimentalCoroutinesApi
    @Throws(DataStoreException::class)
    fun <T : Model> observe(
        itemClass: Class<T>,
        selectionCriteria: QueryPredicate = QueryPredicates.all()
    ): Flow<DataStoreItemChange<T>>

    /**
     * Start synchronizing data with the cloud.
     */
    @Throws(DataStoreException::class)
    suspend fun start()

    /**
     * Stop synchronizing data with the cloud.
     */
    @Throws(DataStoreException::class)
    suspend fun stop()

    /**
     * Stops synchronizing the local data with the cloud, and clears
     * all local data.
     */
    @Throws(DataStoreException::class)
    suspend fun clear()
}
