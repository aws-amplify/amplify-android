package com.amplifyframework.core.model

import com.amplifyframework.core.model.query.QueryOptions


public class DataIntegrityException(s: String) : Throwable() {

}

class InMemoryLazyModel<M: Model>(val model: M? = null) : LazyModel<M> (){

    private var value: M? = model

    override fun getValue(): M? {
        return value;
    }

    override suspend fun get(predicate: QueryOptions):M? {
        model?.let { value = model }
        return model
    }

//    override fun get(onSuccess: (Model?) -> Unit, onFailure: (ApiException) -> Unit) {
//        try {
//            runBlocking {
//                onSuccess(get());
//            }
//        } catch (exception: Exception){
//            onFailure(ApiException("AmplifyException", "Error retrieving related model."))
//        }
//    }
}

class InMemoryLazyList <M: Model>(val model: List<M>? = null) : ILazyList<M> {
    override lateinit var value: M
    override suspend fun get() = model
}