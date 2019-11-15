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

package com.amplifyframework.datastore.network;

import androidx.annotation.NonNull;

import com.amplifyframework.core.model.Model;
import com.amplifyframework.datastore.MutationEvent;

/**
 * A utility to help construct a GraphQL operation document,
 * provided a MutationEvent.
 */
final class MutationDocument {
    @SuppressWarnings("checkstyle:all") private MutationDocument() {}

    // TODO: Actually write this method, don't just return a String. 🙄
    @SuppressWarnings("LineLength")
    @NonNull
    static <T extends Model> String from(final MutationEvent<T> mutationEvent) {
        return "mutation CreateProduct($input: CreateProductInput!) { createProduct(input: $input) { id title content price rating } }";
    }
}
