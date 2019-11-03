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
import com.amplifyframework.datastore.model.Model;

import io.reactivex.Observable;

/**
 * A high-level interface to an object repository.
 */
public interface DataStore {

    /**
     * Saves an object into the data store.
     * @param object The object to save
     * @param resultListener A listener which will be invoked when the save
     *                       is complete or if the save fails
     * @param <T> The type of the object being saved
     */
    <T extends Model> void save(T object, ResultListener<Result> resultListener);

    /**
     * Deletes an object from the data store.
     * @param object The object to delete from the data store
     * @param resultListener A listener which will be invoked when the delete is
     *                       complete or if the delete fails
     * @param <T> The type of the object being deleted
     */
    <T extends Model> void delete(T object, ResultListener<Result> resultListener);

    /**
     * Query the data store to find objects of the provided type.
     * @param objectType The class type of the objects being queried
     * @param resultListener A listener which will be invoked when the query
     *                       returns results, or if there is a failure to query
     * @param <T> the type of the objects for which a query is to be performed
     */
    <T extends Model> void query(Class<T> objectType, ResultListener<Result> resultListener);

    /**
     * Observe all changes in the DataStore.
     * @return An observable stream of DataStore change events,
     *         one for each and every change that occurs in the DataStore.
     */
    Observable<MutationEvent<? extends Model>> observe();

    /**
     * Observe changes to a certain type of object in the DataStore.
     * @param modelClass The class of the model objects to observe
     * @param <T> The type of the model objects to observe
     * @return An observable stream of data store change events, that
     *         will emit events for any changes that occur to the named
     *         model class.
     */
    <T extends Model> Observable<MutationEvent<T>> observe(Class<T> modelClass);

    /**
     * Observe changes to a specific object with the given model class,
     * and having the given model ID.
     * @param modelClass The class of the object being observed
     * @param uniqueId The unique ID of the object being observed
     * @param <T> The type of the object being observed
     * @return A stream of change events surrounding the specific object
     *         which is uniquely identified by the provide class type and
     *         unique id.
     */
    <T extends Model> Observable<MutationEvent<T>> observe(Class<T> modelClass, String uniqueId);

    /**
     * Observe changes to objects of a model type, only when those changes match the
     * criteria of the provide filtering predicate.
     * @param modelClass The class of object to observe
     * @param filteringPredicate A predicate which will be evaluated to determine
     *                           if a particular change on modelClass should be
     *                           emitted onto the returned observable.
     * @param <T> The type of the object to observe
     * @return An observable stream of model change events, for the requested model class,
     *         and considering the provided filtering predicate.
     */
    <T extends Model> Observable<MutationEvent<T>> observe(
            Class<T> modelClass,
            FilteringPredicate<MutationEvent<T>> filteringPredicate);
}

