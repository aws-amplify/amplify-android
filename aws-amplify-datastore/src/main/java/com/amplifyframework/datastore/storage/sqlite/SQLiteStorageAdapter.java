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
import com.amplifyframework.core.model.ModelProvider;
import com.amplifyframework.core.model.ModelSchema;
import com.amplifyframework.core.model.ModelSchemaRegistry;
import com.amplifyframework.core.model.types.JavaFieldType;
import com.amplifyframework.core.model.types.internal.TypeConverter;
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
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import io.reactivex.Observable;
import io.reactivex.subjects.PublishSubject;

/**
 * An implementation of {@link LocalStorageAdapter} using {@link android.database.sqlite.SQLiteDatabase}.
 */
public final class SQLiteStorageAdapter implements LocalStorageAdapter {

    // Database Version
    @VisibleForTesting
    static final int DATABASE_VERSION = 1;

    // Name of the database
    @VisibleForTesting
    static final String DATABASE_NAME = "AmplifyDatastore.db";

    // LogCat Tag.
    private static final String TAG = SQLiteStorageAdapter.class.getSimpleName();

    // ModelSchemaRegistry instance that gives the ModelSchema and Model objects
    // based on Model class name lookup mechanism.
    private final ModelSchemaRegistry modelSchemaRegistry;

    // ThreadPool for SQLite operations.
    private final ExecutorService threadPool;

    // Factory that produces SQL commands.
    private final SQLCommandFactory sqlCommandFactory;

    // Using Gson for deserializing data read from SQLite
    // into a strongly typed Java object.
    private final Gson gson;

    // Used to publish events to the observables subscribed.
    private final PublishSubject<MutationEvent<? extends Model>> mutationEventSubject;

    // Map of tableName => Insert Prepared statement.
    private Map<String, SqlCommand> insertSqlPreparedStatements;

    // Represents a connection to the SQLite database. This database reference
    // can be used to do all SQL operations against the underlying database
    // that this handle represents.
    private SQLiteDatabase databaseConnectionHandle;

    // The helper object controls the lifecycle of database creation, upgrade
    // and opening connection to database.
    private SQLiteOpenHelper sqLiteOpenHelper;

    /**
     * Construct the SQLiteStorageAdapter object.
     * @param modelSchemaRegistry modelSchemaRegistry that hosts the models and their schema.
     */
    public SQLiteStorageAdapter(@NonNull ModelSchemaRegistry modelSchemaRegistry) {
        this.modelSchemaRegistry = Objects.requireNonNull(modelSchemaRegistry);
        this.threadPool = Executors.newCachedThreadPool();
        this.insertSqlPreparedStatements = Collections.emptyMap();
        this.sqlCommandFactory = SQLiteCommandFactory.getInstance();
        this.gson = new Gson();
        this.mutationEventSubject = PublishSubject.create();
    }

    /**
     * Return the default instance of the SQLiteStorageAdapter.
     * @return the default instance of the SQLiteStorageAdapter.
     */
    public static SQLiteStorageAdapter defaultInstance() {
        return new SQLiteStorageAdapter(ModelSchemaRegistry.singleton());
    }

    /** {@inheritDoc} */
    @Override
    public synchronized void initialize(@NonNull Context context,
                           @NonNull ModelProvider modelProvider,
                           @NonNull final ResultListener<List<ModelSchema>> listener) {
        threadPool.submit(() -> {
            try {
                final Set<Class<? extends Model>> models = modelProvider.models();
                /*
                 * Create {@link ModelSchema} objects for the corresponding {@link Model}.
                 * Any exception raised during this when inspecting the Model classes
                 * through reflection will be notified via the
                 * {@link ResultListener#onError(Throwable)} method.
                 */
                modelSchemaRegistry.load(models);

                /*
                 * Create the CREATE TABLE and CREATE INDEX commands for each of the
                 * Models. Instantiate {@link SQLiteStorageHelper} to execute those
                 * create commands.
                 */
                CreateSqlCommands createSqlCommands = getCreateCommands(models);
                sqLiteOpenHelper = SQLiteStorageHelper.getInstance(
                        context,
                        DATABASE_NAME,
                        DATABASE_VERSION,
                        createSqlCommands);

                /*
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
                 * databaseConnectionHandle represents a connection handle to the database.
                 * All database operations will happen through this handle.
                 */
                databaseConnectionHandle = sqLiteOpenHelper.getWritableDatabase();

                /*
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
                        new ArrayList<>(modelSchemaRegistry.getModelSchemaMap().values())
                );
            } catch (Exception exception) {
                listener.onError(new DataStoreException("Error in initializing the " +
                        "SQLiteStorageAdapter", exception));
            }
        });
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @SuppressWarnings("unchecked") // model.getClass() has Class<?>, but we assume Class<T>
    public <T extends Model> void save(@NonNull T model,
                                       @NonNull ResultListener<MutationEvent<T>> listener) {
        threadPool.submit(() -> {
            try {
                final ModelSchema modelSchema = modelSchemaRegistry
                        .getModelSchemaForModelClass(model.getClass().getSimpleName());
                final SqlCommand sqlCommand = insertSqlPreparedStatements.get(modelSchema.getName());
                if (sqlCommand == null || !sqlCommand.hasCompiledSqlStatement()) {
                    throw new DataStoreException("No insert statement " +
                            "found for the Model: " + modelSchema.getName());
                }

                Log.d(TAG, "Writing data to table for: " + model.toString());

                final SQLiteStatement preCompiledInsertStatement = sqlCommand.getCompiledSqlStatement();
                preCompiledInsertStatement.clearBindings();
                bindPreparedInsertSQLStatementWithValues(model, sqlCommand);
                preCompiledInsertStatement.executeInsert();
                preCompiledInsertStatement.clearBindings();

                Log.d(TAG, "Successfully written data to table for: " + model.toString());

                final MutationEvent<T> mutationEvent = MutationEvent.<T>builder()
                        .data(model)
                        .dataClass((Class<T>) model.getClass())
                        .mutationType(MutationEvent.MutationType.INSERT)
                        .source(MutationEvent.Source.DATA_STORE)
                        .build();
                mutationEventSubject.onNext(mutationEvent);
                listener.onResult(mutationEvent);
            } catch (Exception exception) {
                mutationEventSubject.onError(exception);
                listener.onError(new DataStoreException("Error in saving the model.", exception));
            }
        });
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T extends Model> void query(@NonNull Class<T> modelClass,
                                        @NonNull ResultListener<Iterator<T>> listener) {
        threadPool.submit(() -> {
            try {
                Log.d(TAG, "Querying data for: " + modelClass.getSimpleName());

                final Set<T> models = new HashSet<>();
                final ModelSchema modelSchema = modelSchemaRegistry
                        .getModelSchemaForModelClass(modelClass.getSimpleName());

                final Cursor cursor = getQueryAllCursor(modelClass.getSimpleName());
                if (cursor == null) {
                    throw new DataStoreException("Error in getting a cursor to the " +
                            "table for class: " + modelClass.getSimpleName());
                }

                if (cursor.moveToFirst()) {
                    do {
                        final Map<String, Object> mapForModel = buildMapForModel(
                                modelClass, modelSchema, cursor);
                        final String modelInJsonFormat = gson.toJson(mapForModel);
                        models.add(gson.getAdapter(modelClass).fromJson(modelInJsonFormat));
                    } while (cursor.moveToNext());
                }
                if (!cursor.isClosed()) {
                    cursor.close();
                }

                listener.onResult(models.iterator());
            } catch (Exception exception) {
                listener.onError(new DataStoreException("Error in querying the model.", exception));
            }
        });
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T extends Model> void delete(@NonNull T item,
                                         @NonNull ResultListener<MutationEvent<T>> listener) {
        /* TODO */
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Observable<MutationEvent<? extends Model>> observe() {
        return mutationEventSubject;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized void terminate() {
        try {
            insertSqlPreparedStatements = null;

            if (mutationEventSubject != null) {
                mutationEventSubject.onComplete();
            }
            if (threadPool != null) {
                threadPool.shutdown();
            }
            if (databaseConnectionHandle != null) {
                databaseConnectionHandle.close();
            }
            if (sqLiteOpenHelper != null) {
                sqLiteOpenHelper.close();
            }
        } catch (Exception exception) {
            throw new DataStoreException("Error in terminating the SQLiteStorageAdapter.", exception);
        }
    }

    private CreateSqlCommands getCreateCommands(@NonNull Set<Class<? extends Model>> models) {
        final Set<SqlCommand> createTableCommands = new HashSet<>();
        final Set<SqlCommand> createIndexCommands = new HashSet<>();
        for (Class<? extends Model> model: models) {
            final ModelSchema modelSchema =
                modelSchemaRegistry.getModelSchemaForModelClass(model.getSimpleName());
            createTableCommands.add(sqlCommandFactory.createTableFor(modelSchema));
            createIndexCommands.add(sqlCommandFactory.createIndexFor(modelSchema));
        }
        return new CreateSqlCommands(createTableCommands, createIndexCommands);
    }

    private Map<String, SqlCommand> getInsertSqlPreparedStatements() {
        final Map<String, SqlCommand> modifiableMap = new HashMap<>();
        final Set<Map.Entry<String, ModelSchema>> modelSchemaEntrySet =
                ModelSchemaRegistry.singleton().getModelSchemaMap().entrySet();
        for (final Map.Entry<String, ModelSchema> entry: modelSchemaEntrySet) {
            final String tableName = entry.getKey();
            final ModelSchema modelSchema = entry.getValue();
            modifiableMap.put(
                    tableName,
                    sqlCommandFactory.insertFor(tableName, modelSchema, databaseConnectionHandle)
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
                continue;
            }

            JavaFieldType javaFieldType = Enum.class.isAssignableFrom(field.getType())
                    ? JavaFieldType.ENUM
                    : JavaFieldType.from(field.getType().getSimpleName());
            bindPreCompiledInsertStatementWithJavaFields(
                    preCompiledInsertStatement,
                    fieldValue,
                    columnIndex,
                    javaFieldType);
        }

        if (cursor != null && !cursor.isClosed()) {
            cursor.close();
        }
    }

    private void bindPreCompiledInsertStatementWithJavaFields(
            SQLiteStatement preCompiledInsertStatement,
            Object fieldValue,
            int columnIndex,
            JavaFieldType javaFieldType) {
        switch (javaFieldType) {
            case BOOLEAN:
                boolean booleanValue = (boolean) fieldValue;
                preCompiledInsertStatement.bindLong(columnIndex, booleanValue ? 1 : 0);
                break;
            case INTEGER:
                preCompiledInsertStatement.bindLong(columnIndex, (Integer) fieldValue);
                break;
            case LONG:
                preCompiledInsertStatement.bindLong(columnIndex, (Long) fieldValue);
                break;
            case FLOAT:
                preCompiledInsertStatement.bindDouble(columnIndex, (Float) fieldValue);
                break;
            case STRING:
                preCompiledInsertStatement.bindString(columnIndex, (String) fieldValue);
                break;
            case ENUM:
                preCompiledInsertStatement.bindString(columnIndex, gson.toJson(fieldValue));
                break;
            case DATE:
                final Date dateValue = (Date) fieldValue;
                final String dateString = SimpleDateFormat
                        .getDateInstance()
                        .format(dateValue);
                preCompiledInsertStatement.bindString(columnIndex, dateString);
                break;
            case TIME:
                Time timeValue = (Time) fieldValue;
                preCompiledInsertStatement.bindLong(columnIndex, timeValue.getTime());
                break;
            default:
                throw new UnsupportedTypeException(javaFieldType + " is not supported.");
        }
    }

    private <T extends Model> Map<String, Object> buildMapForModel(
            Class<T> modelClass,
            ModelSchema modelSchema,
            Cursor cursor) {
        final Map<String, Object> mapForModel = new HashMap<>();

        for (Map.Entry<String, ModelField> entry : modelSchema.getFields().entrySet()) {
            final String fieldName = entry.getKey();
            try {
                final ModelField modelField = entry.getValue();
                final String fieldGraphQLType = entry.getValue().getTargetType();
                final JavaFieldType fieldJavaType = modelField.isEnum()
                        ? JavaFieldType.ENUM
                        : TypeConverter.getJavaTypeForGraphQLType(fieldGraphQLType);

                final int columnIndex = cursor.getColumnIndexOrThrow(fieldName);
                switch (fieldJavaType) {
                    case STRING:
                        mapForModel.put(fieldName, cursor.getString(columnIndex));
                        break;
                    case ENUM:
                        String stringValueFromCursor = cursor.getString(columnIndex);
                        Class<?> enumType = modelClass.getDeclaredField(fieldName).getType();
                        Object enumValue = gson.getAdapter(enumType).fromJson(stringValueFromCursor);
                        mapForModel.put(fieldName, enumValue);
                        break;
                    case INTEGER:
                        mapForModel.put(fieldName, cursor.getInt(columnIndex));
                        break;
                    case BOOLEAN:
                        mapForModel.put(fieldName, cursor.getInt(columnIndex) != 0);
                        break;
                    case FLOAT:
                        mapForModel.put(fieldName, cursor.getFloat(columnIndex));
                        break;
                    case LONG:
                        mapForModel.put(fieldName, cursor.getLong(columnIndex));
                        break;
                    case DATE:
                        final String dateInStringFormat = cursor.getString(columnIndex);
                        final Date dateInDateFormat = SimpleDateFormat
                                .getDateInstance()
                                .parse(dateInStringFormat);
                        mapForModel.put(fieldName, dateInDateFormat);
                        break;
                    case TIME:
                        final long timeInLongFormat = cursor.getLong(columnIndex);
                        mapForModel.put(fieldName, new Time(timeInLongFormat));
                        break;
                    default:
                        throw new UnsupportedTypeException(fieldJavaType + " is not supported.");
                }
            } catch (Exception exception) {
                Log.e(TAG, "Error in reading data for field: " + fieldName, exception);
                mapForModel.put(fieldName, null);
            }
        }
        return mapForModel;
    }

    @VisibleForTesting
    Cursor getQueryAllCursor(@NonNull String tableName) {
        // Query all rows in table.
        return this.databaseConnectionHandle.query(tableName,
                null,
                null,
                null,
                null,
                null,
                null);
    }
}
