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
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;

import com.amplifyframework.core.Immutable;
import com.amplifyframework.core.ResultListener;
import com.amplifyframework.datastore.DataStoreException;
import com.amplifyframework.datastore.MutationEvent;
import com.amplifyframework.datastore.model.Model;
import com.amplifyframework.datastore.model.ModelField;
import com.amplifyframework.datastore.model.ModelRegistry;
import com.amplifyframework.datastore.model.ModelSchema;
import com.amplifyframework.datastore.storage.LocalStorageAdapter;

import java.lang.reflect.Field;
import java.sql.Time;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

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
    private SQLiteDatabase writableDatabaseConnectionHandle;

    // ThreadPool for SQLite operations.
    private ExecutorService threadPool;

    // Factory that produces SQL commands.
    private SQLCommandFactory sqlCommandFactory;

    // Map of tableName => Insert Prepared statement.
    private Map<String, SqlCommand> insertSqlPreparedStatements;

    /**
     * Construct the SQLiteStorageAdapter object.
     * @param modelRegistry modelRegistry that hosts the models and their schema.
     */
    public SQLiteStorageAdapter(@NonNull ModelRegistry modelRegistry) {
        this.modelRegistry = modelRegistry;
        this.threadPool = Executors.newCachedThreadPool();
        this.insertSqlPreparedStatements = Collections.emptyMap();
        this.sqlCommandFactory = SQLiteCommandFactory.getInstance();
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
                      @NonNull List<Class<? extends Model>> models,
                      @NonNull final ResultListener<Void> listener) {
        threadPool.submit(() -> {
            try {
                /**
                 * Create {@link ModelSchema} objects for the corresponding {@link Model}.
                 * Any exception raised during this when inspecting the Model classes
                 * through reflection will be notified via the
                 * {@link ResultListener#onError(Throwable)} method.
                 */
                modelRegistry.createModelSchemaForModels(models);

                /**
                 * Create the CREATE TABLE and CREATE INDEX commands for each of the
                 * Models. Instantiate {@link SQLiteStorageHelper} to execute those
                 * create commands.
                 */
                final Set<SqlCommand> createCommands = getCreateSqlCommands(models);
                final SQLiteStorageHelper dbHelper = SQLiteStorageHelper.getInstance(
                        context,
                        createCommands);

                /**
                 * Create and/or open a database. This also invokes
                 * {@link SQLiteStorageHelper#onCreate(SQLiteDatabase)} which executes the tasks
                 * to create tables and indexes. When the function returns without any exception
                 * being thrown, invoke the {@link ResultListener#onResult(Object)}.
                 *
                 * Errors are thrown when there is no write permission to the database, no space
                 * left in the database for any write operation and other errors thrown while
                 * creating and opening a database. All errors are passed through the
                 * {@link ResultListener#onError(Throwable)}.
                 *
                 * writableDatabaseConnectionHandle represents a connection handle to the database. All
                 * database operations will happen through this handle.
                 */
                writableDatabaseConnectionHandle = dbHelper.getWritableDatabase();

                /**
                 * Create INSERT INTO TABLE_NAME statements for all SQL tables
                 * and compile them and store in an in-memory map. Later, when a
                 * {@link #save(T, ResultListener)} operation needs to insert
                 * an object (sql rows) into the database, it can bind the input
                 * values with the prepared insert statement.
                 *
                 * This is done to improve performance of database write operations.
                 */
                this.insertSqlPreparedStatements = getInsertSqlPreparedStatements();

                listener.onResult(null);
            } catch (Exception exception) {
                listener.onError(new DataStoreException("Error in creating and opening a " +
                        "connection to the database." + exception));
            }
        });
    }

    /**
     * Save a {@link Model} to the local storage engine. The {@link ResultListener} will be invoked when the
     * save operation is completed to notify the success and failure.
     * @param model    the Model object
     * @param listener the listener to be invoked when the save operation completes
     * @param <T> parameter type of the Model
     */
    public <T extends Model> void save(@NonNull T model,
                                       @NonNull ResultListener<MutationEvent<T>> listener) {
        threadPool.submit(() -> {
            try {
                final ModelSchema modelSchema = modelRegistry
                        .getModelSchemaForModelClass(model.getClass().getSimpleName());
                final SqlCommand sqlCommand = insertSqlPreparedStatements.get(modelSchema.getName());
                if (sqlCommand == null) {
                    listener.onError(new DataStoreException("Error in saving the model. No insert statement " +
                            "found for the Model: " + modelSchema.getName()));
                    return;
                }

                final SQLiteStatement preCompiledInsertStatement = sqlCommand.getCompiledSqlStatement();
                preCompiledInsertStatement.clearBindings();
                bindPreparedInsertSQLStatementWithValues(model, sqlCommand);
                preCompiledInsertStatement.executeInsert();
                preCompiledInsertStatement.clearBindings();

                listener.onResult(MutationEvent.<T>builder()
                        .data(model)
                        .mutationType(MutationEvent.MutationType.INSERT)
                        .source(MutationEvent.Source.DATA_STORE)
                        .build());
            } catch (Exception exception) {
                listener.onError(new DataStoreException("Error in saving the model.", exception));
            }
        });
    }

    /**
     * Query the storage adapter for models of a given type.
     *
     * @param modelClass The class type of models for which to query
     * @param listener   A listener who will be notified of the result of the query
     */
    @Override
    public <T extends Model> void query(@NonNull Class<T> modelClass,
                                        @NonNull ResultListener<Iterator<T>> listener) {
        /* TODO */
    }

    /**
     * Delets and item from storage.
     *
     * @param item     Item to delete
     * @param listener Listener to callback with result
     */
    @Override
    public <T extends Model> void delete(@NonNull T item,
                                         @NonNull ResultListener<MutationEvent<T>> listener) {
        /* TODO */
    }

    @Override
    public Observable<MutationEvent<? extends Model>> observe() {
        return null;
    }

    private Set<SqlCommand> getCreateSqlCommands(@NonNull List<Class<? extends Model>> models) {
        final Set<SqlCommand> createCommands = new HashSet<>();
        for (Class<? extends Model> model: models) {
            final ModelSchema modelSchema = ModelRegistry.getInstance()
                    .getModelSchemaForModelClass(model.getSimpleName());
            sqlCommandFactory.createTableFor(modelSchema);
            createCommands.add(sqlCommandFactory.createTableFor(modelSchema));
            createCommands.add(sqlCommandFactory.createIndexFor(modelSchema));
        }
        return createCommands;
    }

    private Map<String, SqlCommand> getInsertSqlPreparedStatements() {
        final Map<String, SqlCommand> modifiableMap = new HashMap<>();
        final Set<Map.Entry<String, ModelSchema>> modelSchemaEntrySet =
                ModelRegistry.getInstance().getModelSchemaMap().entrySet();
        for (final Map.Entry<String, ModelSchema> entry: modelSchemaEntrySet) {
            final String tableName = entry.getKey();
            final ModelSchema modelSchema = entry.getValue();
            modifiableMap.put(tableName, getPreparedInsertSQLStatement(tableName, modelSchema));
        }
        return Immutable.of(modifiableMap);
    }

    private SqlCommand getPreparedInsertSQLStatement(@NonNull String tableName,
                                                     @NonNull ModelSchema modelSchema) {
        final StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("INSERT INTO ");
        stringBuilder.append(tableName);
        stringBuilder.append(" (");
        final Map<String, ModelField> fields = modelSchema.getFields();
        final Iterator<String> fieldsIterator = fields.keySet().iterator();
        while (fieldsIterator.hasNext()) {
            final String fieldName = fieldsIterator.next();
            stringBuilder.append(fieldName);
            if (fieldsIterator.hasNext()) {
                stringBuilder.append(", ");
            } else {
                stringBuilder.append(")");
            }
        }
        stringBuilder.append(" VALUES ");
        stringBuilder.append("(");
        for (int i = 0; i < fields.size(); i++) {
            if (i == fields.size() - 1) {
                stringBuilder.append("?");
            } else {
                stringBuilder.append("?, ");
            }
        }
        stringBuilder.append(")");
        final String preparedInsertStatement = stringBuilder.toString();
        final SQLiteStatement compiledInsertStatement =
                writableDatabaseConnectionHandle.compileStatement(preparedInsertStatement);
        return new SqlCommand(tableName, preparedInsertStatement, compiledInsertStatement);
    }

    private <T> void bindPreparedInsertSQLStatementWithValues(@NonNull final T object,
                                                              @NonNull final SqlCommand sqlCommand)
            throws IllegalAccessException {
        final Cursor cursor = getQueryAllCursor(sqlCommand.tableName());
        final SQLiteStatement preCompiledInsertStatement = sqlCommand.getCompiledSqlStatement();
        final Set<Field> classFields = findFields(object.getClass());
        final Iterator<Field> fieldIterator = classFields.iterator();

        while (fieldIterator.hasNext()) {
            final Field field = fieldIterator.next();
            field.setAccessible(true);
            final String fieldName = field.getName();
            final Object fieldValue = field.get(object);

            // Move the columns index to 1-based index.
            final int columnIndex = cursor.getColumnIndexOrThrow(fieldName) + 1;
            if (fieldValue == null) {
                preCompiledInsertStatement.bindNull(columnIndex);
                return;
            }

            if (field.getType().equals(float.class) || field.getType().equals(Float.class)) {
                preCompiledInsertStatement.bindDouble(columnIndex, (Float) fieldValue);
            } else if (field.getType().equals(int.class) || field.getType().equals(Integer.class)) {
                preCompiledInsertStatement.bindLong(columnIndex, (Integer) fieldValue);
            } else if (field.getType().equals(long.class) || field.getType().equals(Long.class)) {
                preCompiledInsertStatement.bindLong(columnIndex, (Long) fieldValue);
            } else if (field.getType().equals(double.class) || field.getType().equals(Double.class)) {
                preCompiledInsertStatement.bindDouble(columnIndex, (Double) fieldValue);
            } else if (field.getType().equals(String.class) || field.getType().equals(Enum.class)) {
                preCompiledInsertStatement.bindString(columnIndex, (String) fieldValue);
            } else if (field.getType().equals(boolean.class)) {
                boolean booleanValue = (boolean) fieldValue;
                preCompiledInsertStatement.bindLong(columnIndex, booleanValue ? 1 : 0);
            } else if (field.getType().equals(Date.class)) {
                Date dateValue = (Date) fieldValue;
                String dateString = SimpleDateFormat.getDateInstance().format(dateValue);
                preCompiledInsertStatement.bindString(columnIndex, dateString);
            } else if (field.getType().equals(Time.class)) {
                Time timeValue = (Time) fieldValue;
                preCompiledInsertStatement.bindString(columnIndex, timeValue.toString());
            }
        }

        if (cursor != null) {
            cursor.close();
        }
    }

    @VisibleForTesting
    Cursor getQueryAllCursor(@NonNull String tableName) {
        // Query all rows in table.
        Cursor cursor = this.writableDatabaseConnectionHandle.query(tableName,
                null,
                null,
                null,
                null,
                null,
                null);
        if (cursor != null) {
            // Move to first cursor.
            cursor.moveToFirst();
        }
        return cursor;
    }

    private static Set<Field> findFields(@NonNull Class<?> clazz) {
        Set<Field> set = new HashSet<>();
        Class<?> c = clazz;
        while (c != null) {
            for (Field field : c.getDeclaredFields()) {
                if (field.isAnnotationPresent(
                        com.amplifyframework.datastore.annotations.ModelField.class)) {
                    set.add(field);
                }
            }
            c = c.getSuperclass();
        }
        return set;
    }
}

