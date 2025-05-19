/*
 * Copyright 2025 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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
package com.amplifyframework.api.aws.extensions

import com.amplifyframework.core.model.LazyModelList
import com.amplifyframework.core.model.Model
import com.amplifyframework.core.model.PaginationToken

internal suspend fun <T : Model> LazyModelList<T>.fetchAllPages(): List<T> {
    var hasNextPage = true
    var nextToken: PaginationToken? = null
    val results = mutableListOf<T>()
    while (hasNextPage) {
        val page = this.fetchPage(nextToken)
        hasNextPage = page.hasNextPage
        nextToken = page.nextToken
        results.addAll(page.items)
    }
    return results
}
