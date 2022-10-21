package com.amplifyframework.core.model

import com.amplifyframework.AmplifyException
import com.amplifyframework.core.Consumer


class InMemoryLazyModel<M: Model>(val model: M? = null) : LazyModel<M> (){

    private var value: M? = model

    override fun getValue(): M? {
        return value
    }

    override suspend fun get():M? {
        model?.let { value = model }
        return model
    }

    override fun get(onSuccess: Consumer<M>, onFailure: Consumer<AmplifyException>) {
        if (model != null) {
            onSuccess.accept(model)
        }
    }
}

class InMemoryLazyList <M: Model>(private val modelList: List<M>? = null) : LazyList<M>() {
    private var value: List<M>? = modelList
    override fun getValue():List<M>? {
        return value
    }

    override suspend fun get(): List<M>? {
        modelList?.let {
            value = modelList
        }
        return modelList
    }

    override fun get(onSuccess: Consumer<List<M>>, onFailure: Consumer<AmplifyException>) {
        if (modelList != null) {
            onSuccess.accept(modelList)
        }
    }
}