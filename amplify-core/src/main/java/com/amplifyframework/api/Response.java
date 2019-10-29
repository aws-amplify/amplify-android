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

import com.amplifyframework.core.async.Result;

import java.util.ArrayList;
import java.util.List;

/**
 * Generic response to wrap API query result.
 * @param <T> data type of response
 */
public abstract class Response<T> implements Result {
    private final T data;
    private final List<T> dataAsList;

    /**
     * Constructs a response object with
     * singular data object.
     * @param data response body with singular
     *             object
     */
    public Response(T data) {
        this.data = data;
        this.dataAsList = new ArrayList<>();
    }

    /**
     * Constructs a response object with
     * a list of data objects.
     * @param data response body with list of
     *             objects
     */
    public Response(List<T> data) {
        this.data = null;
        this.dataAsList = new ArrayList<>();
        if (data != null) {
            this.dataAsList.addAll(data);
        }
    }

    /**
     * Gets the data sent back by API.
     * @return API response body
     */
    public T getData() {
        return data;
    }

    /**
     * Gets the list of data sent back by API.
     * @return API response body as list
     */
    public List<T> getDataAsList() {
        return dataAsList;
    }

    /**
     * Checks that data was returned.
     * @return true if data exists, false otherwise
     */
    public boolean hasData() {
        return data != null || !dataAsList.isEmpty();
    }
}
