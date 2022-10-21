package com.amplifyframework.api.aws

import com.amplifyframework.core.model.Model
import com.amplifyframework.core.model.query.predicate.QueryField
import com.amplifyframework.core.model.query.predicate.QueryPredicate
import com.amplifyframework.core.model.query.predicate.QueryPredicates

class AppSyncLazyQueryPredicate<M : Model>() {
    fun createPredicate(clazz: Class<M>, keyMap: Map<String, Any>): QueryPredicate {
        val queryPredicate = QueryPredicates.all()
        keyMap.forEach { queryPredicate.and(QueryField.field(clazz.simpleName, it.key).eq(it.value)) }
        return queryPredicate
    }
}