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

package com.amplifyframework.api.graphql;

import com.amplifyframework.core.model.Model;

/**
 *
 * Represents a page of results returned from an API.  Specifically, contains the list of items in the page, as well as
 * metadata needed to obtain the next page.
 *
 * @param <T> Type of item in the list.  Must extend Model.
 */
public abstract class Page<T extends Model> {

    /**
     * Returns the list of items obtained from an API query.
     * @return Iterable of Model objects
     */
    public abstract Iterable<T> getItems();

    /**
     * Returns whether a subsequent page of results is available from the API.
     * @return boolean whether a subsequent page is available
     */
    public abstract boolean hasNextPage();

    /**
     * Returns a GraphQLRequest which can be used to obtain the next page of results.  The request itself is identical
     * to the GraphQLRequest used to obtain the current page of results, except that pagination metadata is added.
     *
     * @return GraphQLRequest to obtain the next page of results
     */
    public abstract GraphQLRequest<Page<T>> getRequestForNextPage();
}
