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

package com.amplifyframework.datastore.storage.sqlite;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import androidx.annotation.NonNull;

import com.amplifyframework.core.ResultListener;
import com.amplifyframework.datastore.MutationEvent;
import com.amplifyframework.datastore.model.Model;
import com.amplifyframework.datastore.model.ModelRegistry;
import com.amplifyframework.datastore.model.ModelSchema;
import com.amplifyframework.datastore.storage.LocalStorageAdapter;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import io.reactivex.Observable;

/**
 * An implementation of {@link LocalStorageAdapter} using {@link android.database.sqlite.SQLiteDatabase}.
 */
public final class SQLiteStorageAdapter implements LocalStorageAdapter {

    // ModelRegistry instance that gives the ModelSchema and Model objects
    // based on Model class name lookup mechanism.
    private final ModelRegistry modelRegistry;

    // Represents a connection to the SQLite database. This database reference
    // can be used to do all SQL operations against the underlying database
    // that this handle represents.
    private SQLiteDatabase sqLiteDatabase;

    /**
     * Construct the SQLiteStorageAdapter object.
     * @param modelRegistry modelRegistry that hosts the models and their schema.
     */
    public SQLiteStorageAdapter(@NonNull ModelRegistry modelRegistry) {
        this.modelRegistry = modelRegistry;
    }

    /**
     * Return the default instance of the SQLiteStorageAdapter.
     * @return the default instance of the SQLiteStorageAdapter.
     */
    public static SQLiteStorageAdapter defaultInstance() {
        return new SQLiteStorageAdapter(ModelRegistry.getInstance());
    }

    /**
     * Setup the storage engine with the models. For each {@link Model}, construct a
     * {@link ModelSchema} and setup the necessities for persisting a {@link Model}.
     * This setUp is a pre-requisite for all other operations of a {@link LocalStorageAdapter}.
     *
     * The setup is synchronous and the completion of this method guarantees completion
     * of the creation of SQL database and tables for the corresponding data models
     * passed in.
     *
     * @param context Android application context required to
     *                interact with a storage mechanism in Android.
     * @param models  list of data {@link Model} classes
     */
    @Override
    public void setUp(@NonNull Context context,
                      @NonNull List<Class<? extends Model>> models) {
        final Set<CreateSqlCommand> createTableCommands = new HashSet<>();

        modelRegistry.createModelSchemaForModels(models);
        for (Class<? extends Model> model: models) {
            final ModelSchema modelSchema = ModelRegistry.getInstance()
                    .getModelSchemaForModelClass(model.getSimpleName());
            final CreateSqlCommand createSqlCommand = CreateSqlCommand.fromModelSchema(modelSchema);
            createTableCommands.add(createSqlCommand);
        }

        final SQLiteStorageHelper dbHelper = SQLiteStorageHelper.getInstance(
                context,
                createTableCommands);
        sqLiteDatabase = dbHelper.getWritableDatabase();
    }

    @Override
    public <T extends Model> void save(
            @NonNull final T model,
            @NonNull final ResultListener<MutationEvent<T>> listener) {
    }

    @Override
    public <T extends Model> void query(
            @NonNull final Class<T> modelClass,
            @NonNull final ResultListener<Iterator<T>> listener) {
    }

    @Override
    public <T extends Model> void delete(
            @NonNull final T item,
            @NonNull final ResultListener<MutationEvent<T>> listener) {
    }

    @Override
    public Observable<MutationEvent<? extends Model>> observe() {
        return null;
    }
}

