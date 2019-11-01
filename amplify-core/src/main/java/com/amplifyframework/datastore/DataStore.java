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

package com.amplifyframework.datastore;

import com.amplifyframework.core.ResultListener;
import com.amplifyframework.core.async.Result;

/**
 * A high-level interface to an object repository.
 * @param <T> The type of object that this data store manages, e.g. Integer.
 */
public interface DataStore<T> {
    /**
     * Saves an object into the data store.
     * @param object The object to save
     * @param resultListener A listener which will be invoked when the save
     *                       is complete or if the save fails
     */
    void save(T object, ResultListener<Result> resultListener);

    /**
     * Deletes an object from the data store.
     * @param object The object to delete from the data store
     * @param resultListener A listener which will be invoked when the delete is
     *                       complete or if the delete fails
     */
    void delete(T object, ResultListener<Result> resultListener);

    /**
     * Query the data store to find objects of the provided type.
     * @param objectType The class type of the objects being queried
     * @param resultListener A listener which will be invoked when the query
     *                       returns results, or if there is a failure to query
     */
    void query(Class<T> objectType, ResultListener<Result> resultListener);

    /**
     * Observe changes to an object, in the data store.
     * @param object An object in the data store (matched somehow)
     * @param resultListener A listener that will be invoked when changes are
     *                       made to the object or on failure to observe
     *                       the data store
     */
    void observe(T object, ResultListener<Result> resultListener);
}
