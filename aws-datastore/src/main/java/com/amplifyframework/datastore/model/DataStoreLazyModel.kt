package com.amplifyframework.datastore.model

import com.amplifyframework.core.model.LazyModel
import com.amplifyframework.core.model.Model
import com.amplifyframework.core.model.query.QueryOptions
import com.amplifyframework.kotlin.core.Amplify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.toList


public class DataStoreLazyModel<M: Model>(private val model: M? = null,
                                          private val clazz: Class<M>
) : LazyModel<M>(){

    private var value: M? = model

    override fun getValue(): M? {
        return value;
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    override suspend fun get(predicate: QueryOptions):M? {
        model?.let { value = model
            return model
        }
        //What happens when there is data
        return Amplify.DataStore.query(clazz.kotlin, predicate).toList()[0]
    }
}