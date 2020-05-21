/*
 * Copyright 2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

import androidx.core.util.ObjectsCompat;

import java.util.ArrayList;
import java.util.List;

/**
 * A hypothetical model for some data returned from a GraphQL API.
 * This is a placeholder for the model that gets code generated by the
 * Amplify CLI.
 */
final class ListTodosResult {
    private final List<Todo> items;

    ListTodosResult() {
        this.items = new ArrayList<>();
    }

    ListTodosResult(final List<Todo> items) {
        this.items = new ArrayList<>();
        if (items != null) {
            this.items.addAll(items);
        }
    }

    List<Todo> getItems() {
        return ListTodosResult.this.items;
    }

    @Override
    public boolean equals(Object thatObject) {
        if (this == thatObject) {
            return true;
        }
        if (thatObject == null || getClass() != thatObject.getClass()) {
            return false;
        }

        ListTodosResult that = (ListTodosResult) thatObject;

        return ObjectsCompat.equals(items, that.items);
    }

    @Override
    public int hashCode() {
        return items != null ? items.hashCode() : 0;
    }
}

