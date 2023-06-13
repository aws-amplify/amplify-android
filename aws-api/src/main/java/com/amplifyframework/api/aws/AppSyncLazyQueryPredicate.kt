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

package com.amplifyframework.api.aws

import com.amplifyframework.core.model.Model
import com.amplifyframework.core.model.query.predicate.QueryField
import com.amplifyframework.core.model.query.predicate.QueryPredicate
import com.amplifyframework.core.model.query.predicate.QueryPredicates

class AppSyncLazyQueryPredicate<M : Model> {
    fun createPredicate(clazz: Class<M>, keyMap: Map<String, Any>): QueryPredicate {
        var queryPredicate = QueryPredicates.all()
        keyMap.forEach {
            queryPredicate = queryPredicate.and(QueryField.field(clazz.simpleName, it.key).eq(it.value))
        }
        return queryPredicate
    }
}
