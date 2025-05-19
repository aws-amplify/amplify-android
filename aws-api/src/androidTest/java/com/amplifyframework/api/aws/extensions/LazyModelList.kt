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
