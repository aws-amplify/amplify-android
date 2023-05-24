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

    private var value: List<M>? = null

    override fun getValue(): List<M>? {
        return value
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    override suspend fun get(): List<M>? {
        value?.let { return value }
        try {
            value = Amplify.DataStore.query(clazz.kotlin, Where.matches(predicate.createPredicate(clazz, keyMap)))
                .toList()
        } catch (error: DataStoreException) {
            Log.e("MyAmplifyApp", "Query failure", error)
        }
        return value
    }

    override fun get(onSuccess: Consumer<List<M>>, onFailure: Consumer<AmplifyException>) {
        val onQuerySuccess = Consumer<Iterator<M>> {
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
        )
    }
}
