package com.amplifyframework.core.model

import com.amplifyframework.core.model.query.QueryOptions


abstract class LazyModel< M: Model> {
    abstract fun getValue():M?
    abstract suspend fun get(predicate: QueryOptions): M?
    suspend fun require(predicate: QueryOptions): M {
        return get(predicate) ?: throw DataIntegrityException("Required model could not be found")
    }
    //abstract fun get(onSuccess: (Model?) -> Unit, onFailure: (ApiException) -> Unit)
}



//class APILazyModel<M: Model>(val propertyName: String) : LazyModel<M> {
//    var model: M? = null
//
//    override suspend fun get(): M? {
//        if (model == null) {
//            // build the correct predicate using the Model schema and propertyName
//            // and then query the API
//            model = Amplify.API.query(...)
//        }
//        return model
//    }
//
//}

// then in a model (just a incomplete example)

//class Post : Model {
//
//    var _blog: LazyModel<Blog>
//    suspend fun blog(): Blog = _blog.require() // it's required!
//
//    var _reviewer: LazyModel<User>
//    suspend fun reviewer(): User? = _reviewer.get() // not required!
//}