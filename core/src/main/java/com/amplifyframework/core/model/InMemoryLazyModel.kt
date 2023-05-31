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

class InMemoryLazyModel<M : Model>(model: M? = null) : LazyModel<M> () {

    private var value: M? = model

    override fun getValue(): M? {
        return value
    }

    override suspend fun get(): M? {
        return value
    }

    override fun get(onSuccess: Consumer<M>, onFailure: Consumer<AmplifyException>) {
        if (value != null) {
            onSuccess.accept(value!!)
        }
    }
}

class InMemoryLazyList<M : Model>(modelList: List<M> = emptyList()) : LazyList<M>() {
    private var value: List<M> = modelList
    override fun getItems(): List<M> {
        return value
    }

    override suspend fun getNextPage(): List<M> {
        return emptyList()
    }

    override fun getNextPage(onSuccess: Consumer<List<M>>, onFailure: Consumer<AmplifyException>) {
        onSuccess.accept(emptyList())
    }

    override fun hasNextPage(): Boolean {
        return false
    }
}
