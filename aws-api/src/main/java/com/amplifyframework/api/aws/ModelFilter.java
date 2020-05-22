/*
 * Copyright 2020 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package com.amplifyframework.api.aws;

import androidx.annotation.NonNull;

import com.amplifyframework.core.model.query.predicate.QueryField;
import com.amplifyframework.core.model.query.predicate.QueryPredicate;

/**
 * TODO write docs.
 */
public final class ModelFilter {

    @NonNull
    private QueryPredicate predicate;

    private ModelFilter(@NonNull QueryPredicate predicate) {
        this.predicate = predicate;
    }

    /**
     * TODO docs.
     * @param predicate todo.
     * @return todo.
     */
    @NonNull
    public static ModelFilter filter(@NonNull final QueryPredicate predicate) {
        return new ModelFilter(predicate);
    }

    /**
     * TODO write docs.
     * @param modelId todo.
     * @return todo.
     */
    @NonNull
    public static ModelFilter byId(@NonNull final String modelId) {
        return filter(QueryField.field("id").eq(modelId));
    }

    /**
     * TODO docs.
     * @return todo.
     */
    @NonNull
    public QueryPredicate getPredicate() {
        return predicate;
    }

}
