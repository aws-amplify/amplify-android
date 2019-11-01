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

package com.amplifyframework.datastore.storage;

import android.content.Context;
import androidx.annotation.NonNull;

import com.amplifyframework.core.ResultListener;
import com.amplifyframework.datastore.model.Model;

import java.util.List;

/**
 * The interface that defines the contract for local storage
 * engine implementation.
 */
public interface LocalStorageAdapter {
    /**
     * Setup the storage engine with the models.
     * For each {@link Model}, construct a
     * {@link com.amplifyframework.datastore.model.ModelSchema}
     * and setup the necessities for persisting a {@link Model}.
     * This setUp is a pre-requisite for all other operations
     * of a LocalStorageAdapter.
     *
     * @param context Android application context required to
     *                interact with a storage mechanism in Android.
     * @param models list of Model classes
     */
    void setUp(@NonNull Context context, @NonNull List<Class<? extends Model>> models);

    /**
     * Save a {@link Model} to the local storage engine.
     * The {@link ResultListener} will be invoked when the
     * save operation is completed to notify the success and
     * failure.
     *
     * @param model the Model object
     * @param listener the listener to be invoked when the
     *                 save operation is completed.
     */
    void save(@NonNull Model model, @NonNull ResultListener<Model> listener);
}
