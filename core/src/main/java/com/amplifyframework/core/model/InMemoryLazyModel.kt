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

package com.amplifyframework.core.model

import com.amplifyframework.AmplifyException
import com.amplifyframework.core.Consumer

class InMemoryLazyModel<M : Model>(val model: M? = null) : LazyModel<M> () {

    private var value: M? = model

    override fun getValue(): M? {
        return value
    }

    override suspend fun get(): M? {
        model?.let { value = model }
        return model
    }

    override fun get(onSuccess: Consumer<M>, onFailure: Consumer<AmplifyException>) {
        if (model != null) {
            onSuccess.accept(model)
        }
    }
}

class InMemoryLazyList<M : Model>(private val modelList: List<M>? = null) : LazyList<M>() {
    private var value: List<M>? = modelList
    override fun getValue(): List<M>? {
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
