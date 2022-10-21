package com.amplifyframework.datastore.model

import com.amplifyframework.core.model.query.predicate.QueryField
import com.amplifyframework.core.model.query.predicate.QueryPredicate

class DatastoreLazyQueryPredicate<M> {

    fun createPredicate(clazz: Class<M>, keyMap: Map<String, Any>): QueryPredicate {
        val keyValue = keyMap.iterator().next()
        return QueryField.field(clazz.simpleName, keyValue.key).eq(keyValue.value)
    }
}