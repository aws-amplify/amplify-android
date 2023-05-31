/*
 * Copyright 2023 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package com.amplifyframework.datastore.model

import android.util.Log
import com.amplifyframework.AmplifyException
import com.amplifyframework.core.Consumer
import com.amplifyframework.core.model.LazyList
import com.amplifyframework.core.model.Model
import com.amplifyframework.core.model.query.Where
import com.amplifyframework.datastore.DataStoreException
import com.amplifyframework.kotlin.core.Amplify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.toList
import com.amplifyframework.core.Amplify as coreAmplify

class DataStoreLazyModelList<M : Model>(
    private val clazz: Class<M>,
    private val keyMap: Map<String, Any>,
    private val predicate: DatastoreLazyQueryPredicate<M>
) : LazyList<M>() {

    private var value: MutableList<M> = mutableListOf()

    override fun getItems(): List<M> {
        return value
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    override suspend fun getNextPage(): List<M> {
        /*value?.let { return value }
        try {
            value = Amplify.DataStore.query(clazz.kotlin, Where.matches(predicate.createPredicate(clazz, keyMap)))
                .toList()
        } catch (error: DataStoreException) {
            Log.e("MyAmplifyApp", "Query failure", error)
        }
        return value*/
        TODO()
    }

    override fun getNextPage(onSuccess: Consumer<List<M>>, onFailure: Consumer<AmplifyException>) {
        /*val onQuerySuccess = Consumer<Iterator<M>> {
            val result = it.asSequence().toList()
            value = result
            onSuccess.accept(result)
        }
        val onApiFailure = Consumer<DataStoreException> { onFailure.accept(it) }
        coreAmplify.DataStore.query(
            clazz,
            predicate.createPredicate(clazz, keyMap),
            onQuerySuccess,
            onApiFailure
        )*/
        TODO()
    }

    override fun hasNextPage(): Boolean {
        TODO()
    }
}
