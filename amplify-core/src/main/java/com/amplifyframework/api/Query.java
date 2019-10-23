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

package com.amplifyframework.api;

/**
 * Generic query using which API calls will be made.
 */
public abstract class Query {
    private final String prefix;
    private final String document;

    /**
     * Constructs a query object.
     * @param prefix query prefix
     * @param query query document
     */
    public Query(String prefix, String query) {
        this.prefix = prefix;
        this.document = query;
    }

    /**
     * Processes query parameters into a query string.
     * @return processed query string
     */
    protected abstract String getContent();

    /**
     * Query prefix to specify the API operation type.
     * Ex) REST API's verb or GraphQL API's operation.
     * @return query prefix
     */
    protected String getPrefix() {
        return prefix;
    }

    /**
     * Query document to specify query details.
     * Ex) REST API's path or GraphQL API's query body.
     * @return query document
     */
    protected String getDocument() {
        return document;
    }
}
