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

import androidx.annotation.Nullable;

import com.amplifyframework.core.async.AmplifyOperation;
import com.amplifyframework.core.async.Cancelable;
import com.amplifyframework.core.async.Listener;
import com.amplifyframework.core.category.CategoryType;

/**
 * Base operation type for the API category.
 * @param <T> data type of response
 * @param <I> data type of input request
 * @param <O> an instance of output {@link Response} object
 *           that wraps around API response
 */
public abstract class ApiOperation<T, I, O extends Response<T>> extends AmplifyOperation<I> implements Cancelable {
    private final ResponseFactory responseFactory;
    private final Class<T> classToCast;
    private final Listener<O> callback;

    /**
     * Constructs a new instance of a ApiOperation.
     * @param responseFactory an implementation of ResponseFactory
     * @param request the request object of the operation
     * @param classToCast class to cast the response to
     * @param callback local callback listener being registered
     */
    public ApiOperation(ResponseFactory responseFactory,
                        I request,
                        Class<T> classToCast,
                        @Nullable Listener<O> callback) {
        super(CategoryType.API, request);
        this.responseFactory = responseFactory;
        this.classToCast = classToCast;
        this.callback = callback;
    }

    /**
     * Gets the locally registered callback.
     * @return the local callback
     */
    protected final Listener<O> callback() {
        return callback;
    }

    /**
     * Check if callback was registered.
     * @return true if callback exists, false otherwise
     */
    protected final boolean hasCallback() {
        return callback != null;
    }

    /**
     * Converts the response json string to formatted
     * {@link Response} object to be used by callback.
     * @param jsonResponse json response from API to be converted
     * @return wrapped response object
     */
    @SuppressWarnings("unchecked")
    protected final O wrapResponse(String jsonResponse) {
        try {
            return (O) responseFactory.buildResponse(jsonResponse, classToCast);
        } catch (ClassCastException cce) {
            throw new ApiException.ObjectSerializationException();
        }
    }
}
