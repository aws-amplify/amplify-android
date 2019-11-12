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
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteStatement;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;

import com.amplifyframework.core.Immutable;
import com.amplifyframework.core.ResultListener;
import com.amplifyframework.core.model.Model;
import com.amplifyframework.core.model.ModelField;
import com.amplifyframework.core.model.ModelRegistry;
import com.amplifyframework.core.model.ModelSchema;
import com.amplifyframework.core.model.internal.types.TypeConverter;
import com.amplifyframework.datastore.DataStoreException;
import com.amplifyframework.datastore.MutationEvent;
import com.amplifyframework.datastore.storage.LocalStorageAdapter;
import com.amplifyframework.util.FieldFinder;

import com.google.gson.Gson;

import java.lang.reflect.Field;
import java.sql.Time;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
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

    // LogCat Tag.
    private static final String TAG = SQLiteStorageAdapter.class.getSimpleName();

    // ModelRegistry instance that gives the ModelSchema and Model objects
    // based on Model class name lookup mechanism.
    private final ModelRegistry modelRegistry;

    // Represents a connection to the writable SQLite database. This database reference
    // can be used to do all SQL write operations against the underlying database
    // that this handle represents.
    private SQLiteDatabase writableDatabaseConnectionHandle;

    // Represents a connection to the readable SQLite database. This database reference
    // can be used to do all SQL read operations against the underlying database
    // that this handle represents.
    private SQLiteDatabase readableDatabaseConnectionHandle;

    // The helper object controls the lifecycle of database creation, upgrade
    // and opening connection to database.
    private SQLiteOpenHelper sqLiteOpenHelper;

    // ThreadPool for SQLite operations.
    private final ExecutorService threadPool;

    // Factory that produces SQL commands.
    private final SQLCommandFactory sqlCommandFactory;

    // Map of tableName => Insert Prepared statement.
    private Map<String, SqlCommand> insertSqlPreparedStatements;

    // Using Gson for deserializing data read from SQLite
    // into a strongly typed Java object.
    private final Gson gson;

    /**
     * Construct the SQLiteStorageAdapter object.
     * @param modelRegistry modelRegistry that hosts the models and their schema.
     */
    public SQLiteStorageAdapter(@NonNull ModelRegistry modelRegistry) {
        this.modelRegistry = modelRegistry;
        this.threadPool = Executors.newCachedThreadPool();
        this.insertSqlPreparedStatements = Collections.emptyMap();
        this.sqlCommandFactory = SQLiteCommandFactory.getInstance();
        this.gson = new Gson();
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
                      @NonNull final ResultListener<List<ModelSchema>> listener) {
        threadPool.submit(() -> {
            try {
                /**
                 * Create {@link ModelSchema} objects for the corresponding {@link Model}.
                 * Any exception raised during this when inspecting the Model classes
                 * through reflection will be notified via the
                 * {@link ResultListener#onError(Throwable)} method.
                 */
                modelRegistry.load(models);

                /**
                 * Create the CREATE TABLE and CREATE INDEX commands for each of the
                 * Models. Instantiate {@link SQLiteStorageHelper} to execute those
                 * create commands.
                 */
                final Set<SqlCommand> createCommands = getCreateSqlCommands(models);
                sqLiteOpenHelper = SQLiteStorageHelper.getInstance(
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
                 * writableDatabaseConnectionHandle represents a connection handle to the database.
                 * All database operations will happen through this handle.
                 */
                writableDatabaseConnectionHandle = sqLiteOpenHelper.getWritableDatabase();

                /**
                 * Retrieve an instance to the readable database used for all SQL read operations.
                 */
                readableDatabaseConnectionHandle = sqLiteOpenHelper.getReadableDatabase();

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

                listener.onResult(
                        new ArrayList<>(modelRegistry.getModelSchemaMap().values())
                );
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
                if (sqlCommand == null || !sqlCommand.hasCompiledSqlStatement()) {
                    listener.onError(new DataStoreException("Error in saving the model. No insert statement " +
                            "found for the Model: " + modelSchema.getName()));
                    return;
                }

                Log.d(TAG, "Writing data to table for: " + model.toString());

                final SQLiteStatement preCompiledInsertStatement = sqlCommand.getCompiledSqlStatement();
                preCompiledInsertStatement.clearBindings();
                bindPreparedInsertSQLStatementWithValues(model, sqlCommand);
                preCompiledInsertStatement.executeInsert();
                preCompiledInsertStatement.clearBindings();

                Log.d(TAG, "Successfully written data to table for: " + model.toString());

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
        threadPool.submit(() -> {
            try {
                Log.d(TAG, "Querying data for: " + modelClass.getSimpleName());

                final Set<T> models = new HashSet<>();
                final ModelSchema modelSchema = modelRegistry
                        .getModelSchemaForModelClass(modelClass.getSimpleName());

                final Cursor cursor = getQueryAllCursor(modelClass.getSimpleName());
                while (cursor.moveToNext()) {
                    final Map<String, Object> mapForModel = buildMapForModel(modelSchema, cursor);
                    final String modelInJsonFormat = gson.toJson(mapForModel);
                    models.add(gson.getAdapter(modelClass).fromJson(modelInJsonFormat));
                }
                cursor.close();

                listener.onResult(models.iterator());
            } catch (Exception exception) {
                listener.onError(new DataStoreException("Error in querying the model.", exception));
            }
        });
    }

    private Map<String, Object> buildMapForModel(ModelSchema modelSchema, Cursor cursor) {
        final Map<String, Object> mapForModel = new HashMap<>();

        for (Map.Entry<String, ModelField> entry : modelSchema.getFields().entrySet()) {
            final String fieldName = entry.getKey();
            try {
                final String fieldGraphQLType = entry.getValue().getTargetType();
                final String fieldJavaType = TypeConverter.getJavaTypeForGraphQLType(fieldGraphQLType);

                final int columnIndex = cursor.getColumnIndexOrThrow(fieldName);
                switch (fieldJavaType) {
                    case "String":
                    case "Enum":
                        mapForModel.put(fieldName, cursor.getString(columnIndex));
                        break;
                    case "int":
                        mapForModel.put(fieldName, cursor.getInt(columnIndex));
                        break;
                    case "boolean":
                        mapForModel.put(fieldName, cursor.getInt(columnIndex) != 0);
                        break;
                    case "float":
                        mapForModel.put(fieldName, cursor.getFloat(columnIndex));
                        break;
                    case "long":
                        mapForModel.put(fieldName, cursor.getLong(columnIndex));
                        break;
                    case "Date":
                        final String dateInStringFormat = cursor.getString(columnIndex);
                        final Date dateInDateFormat = SimpleDateFormat
                                .getDateInstance()
                                .parse(dateInStringFormat);
                        mapForModel.put(fieldName, dateInDateFormat);
                        break;
                    case "Time":
                        final long timeInLongFormat = cursor.getLong(columnIndex);
                        mapForModel.put(fieldName, new Time(timeInLongFormat));
                        break;
                    default:
                        throw new UnsupportedTypeException(fieldJavaType + " is not supported.");
                }
            } catch (Exception exception) {
                Log.e(TAG, "Error in reading data for field: " + fieldName);
                mapForModel.put(fieldName, null);
            }
        }
        return mapForModel;
    }

    /**
     * Deletes an item from storage.
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
            modifiableMap.put(
                    tableName,
                    sqlCommandFactory.insertFor(tableName, modelSchema, writableDatabaseConnectionHandle)
            );
        }
        return Immutable.of(modifiableMap);
    }

    private <T> void bindPreparedInsertSQLStatementWithValues(@NonNull final T object,
                                                              @NonNull final SqlCommand sqlCommand)
            throws IllegalAccessException {
        final SQLiteStatement preCompiledInsertStatement = sqlCommand.getCompiledSqlStatement();
        final Set<Field> classFields = FieldFinder.findFieldsIn(object.getClass());
        final Iterator<Field> fieldIterator = classFields.iterator();

        final Cursor cursor = getQueryAllCursor(sqlCommand.tableName());
        if (cursor != null) {
            cursor.moveToFirst();
        }

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
                final Date dateValue = (Date) fieldValue;
                final String dateString = SimpleDateFormat
                        .getDateInstance()
                        .format(dateValue);
                preCompiledInsertStatement.bindString(columnIndex, dateString);
            } else if (field.getType().equals(Time.class)) {
                Time timeValue = (Time) fieldValue;
                preCompiledInsertStatement.bindLong(columnIndex, timeValue.getTime());
            }
        }

        if (cursor != null) {
            cursor.close();
        }
    }

    @VisibleForTesting
    Cursor getQueryAllCursor(@NonNull String tableName) {
        // Query all rows in table.
        return this.readableDatabaseConnectionHandle.query(tableName,
                null,
                null,
                null,
                null,
                null,
                null);
    }
}
