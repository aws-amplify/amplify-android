package com.atlasv.android.amplify.simpleappsync

import com.amplifyframework.core.model.Model
import com.amplifyframework.core.model.query.predicate.QueryField
import com.amplifyframework.core.model.query.predicate.QueryPredicate

/**
 * weiping@atlasv.com
 * 2022/12/8
 */

fun <T : Model> Class<T>.queryField(fieldName: String): QueryField? {
    return declaredFields.find { it.name == fieldName }?.let {
        QueryField.field(fieldName)
    }
}

fun QueryPredicate.andIfNotNull(newPredicate: QueryPredicate?): QueryPredicate {
    newPredicate ?: return this
    return this.and(newPredicate)
}