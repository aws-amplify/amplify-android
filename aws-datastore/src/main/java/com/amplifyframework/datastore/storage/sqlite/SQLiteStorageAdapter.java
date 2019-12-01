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
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;

import com.amplifyframework.core.Amplify;
import com.amplifyframework.core.Immutable;
import com.amplifyframework.core.ResultListener;
import com.amplifyframework.core.model.Model;
import com.amplifyframework.core.model.ModelField;
import com.amplifyframework.core.model.ModelProvider;
import com.amplifyframework.core.model.ModelSchema;
import com.amplifyframework.core.model.ModelSchemaRegistry;
import com.amplifyframework.core.model.PrimaryKey;
import com.amplifyframework.core.model.query.predicate.QueryPredicate;
import com.amplifyframework.core.model.types.JavaFieldType;
import com.amplifyframework.core.model.types.internal.TypeConverter;
import com.amplifyframework.datastore.DataStoreException;
import com.amplifyframework.datastore.storage.GsonStorageItemChangeConverter;
import com.amplifyframework.datastore.storage.LocalStorageAdapter;
import com.amplifyframework.datastore.storage.StorageItemChange;
import com.amplifyframework.datastore.storage.sqlite.adapter.SQLiteColumn;
import com.amplifyframework.datastore.storage.sqlite.adapter.SQLiteTable;
import com.amplifyframework.logging.Logger;
import com.amplifyframework.util.FieldFinder;
import com.amplifyframework.util.StringUtils;

import com.google.gson.Gson;

import java.io.IOException;
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
    private static final Logger LOG = Amplify.Logging.forNamespace("amplify:aws-datastore");

    // Database Version
    private static final int DATABASE_VERSION = 1;

    // Name of the database
    private static final String DATABASE_NAME = "AmplifyDatastore.db";

    // Provider of the Models that will be warehouse-able by the DataStore
    private final ModelProvider modelProvider;

    // ModelSchemaRegistry instance that gives the ModelSchema and Model objects
    // based on Model class name lookup mechanism.
    private final ModelSchemaRegistry modelSchemaRegistry;

    // ThreadPool for SQLite operations.
    private final ExecutorService threadPool;

    // Data is read from SQLite and de-serialized using GSON
    // into a strongly typed Java object.
    private final Gson gson;

    // Used to publish events to the observables subscribed.
    private final PublishSubject<StorageItemChange.Record> itemChangeSubject;

    // Map of tableName => Insert Prepared statement.
    private Map<String, SqlCommand> insertSqlPreparedStatements;

    // Represents a connection to the SQLite database. This database reference
    // can be used to do all SQL operations against the underlying database
    // that this handle represents.
    private SQLiteDatabase databaseConnectionHandle;

    // The helper object controls the lifecycle of database creation, upgrade
    // and opening connection to database.
    private SQLiteOpenHelper sqliteOpenHelper;

    // Factory that produces SQL commands.
    private SQLCommandFactory sqlCommandFactory;

    // A utility to convert StorageItemChange to StorageItemChange.Record
    // and vice-versa
    private final GsonStorageItemChangeConverter storageItemChangeConverter;

    /**
     * Construct the SQLiteStorageAdapter object.
     * @param modelProvider Provides the models that will be usable by the DataStore
     */
    public SQLiteStorageAdapter(@NonNull ModelProvider modelProvider) {
        this.modelProvider = Objects.requireNonNull(modelProvider);
        this.modelSchemaRegistry = ModelSchemaRegistry.singleton();
        this.threadPool = Executors.newCachedThreadPool();
        this.insertSqlPreparedStatements = Collections.emptyMap();
        this.gson = new Gson();
        this.itemChangeSubject = PublishSubject.create();
        this.storageItemChangeConverter = new GsonStorageItemChangeConverter();
    }

    /**
     * Gets a SQLiteStorageAdapter that can be initialized to use the provided models.
     * @param modelProvider A provider of models that will be represented in SQL
     * @return A SQLiteStorageAdapter that will host the provided models in SQL tables
     */
    public static SQLiteStorageAdapter forModels(ModelProvider modelProvider) {
        return new SQLiteStorageAdapter(modelProvider);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized void initialize(
            @NonNull Context context,
            @NonNull final ResultListener<List<ModelSchema>> listener) {
        threadPool.submit(() -> {
            try {
                final Set<Class<? extends Model>> models = new HashSet<>();
                // StorageItemChange.Record.class is an internal system event
                // it is used to journal local storage changes
                models.add(StorageItemChange.Record.class);
                models.addAll(modelProvider.models());

                /*
                 * Start with a fresh registry.
                 */
                modelSchemaRegistry.clear();
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
                this.sqlCommandFactory = new SQLiteCommandFactory();
                CreateSqlCommands createSqlCommands = getCreateCommands(models);
                sqliteOpenHelper = SQLiteStorageHelper.getInstance(
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
                databaseConnectionHandle = sqliteOpenHelper.getWritableDatabase();
                this.sqlCommandFactory = new SQLiteCommandFactory(databaseConnectionHandle);

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
    public <T extends Model> void save(
            @NonNull T item,
            @NonNull StorageItemChange.Initiator initiator,
            @NonNull ResultListener<StorageItemChange.Record> itemSaveListener) {
        threadPool.submit(() -> {
            try {
                final ModelSchema modelSchema =
                    modelSchemaRegistry.getModelSchemaForModelClass(item.getClass().getSimpleName());
                final SQLiteTable sqLiteTable = SQLiteTable.fromSchema(modelSchema);

                if (dataExistsInSQLiteTable(
                        sqLiteTable.getName(),
                        PrimaryKey.fieldName(),
                        item.getId())) {
                    // update model stored in SQLite
                    final SqlCommand sqlCommand = sqlCommandFactory.updateFor(modelSchema, item);
                    if (sqlCommand == null || !sqlCommand.hasCompiledSqlStatement()) {
                        throw new DataStoreException("Error in saving the model. No update statement " +
                                "found for the Model: " + modelSchema.getName());
                    }
                    saveModel(item, modelSchema, sqlCommand, ModelConflictStrategy.OVERWRITE_EXISTING);
                } else {
                    // insert model in SQLite
                    final SqlCommand sqlCommand = insertSqlPreparedStatements.get(modelSchema.getName());
                    if (sqlCommand == null || !sqlCommand.hasCompiledSqlStatement()) {
                        throw new DataStoreException("No insert statement found for the Model: " +
                                modelSchema.getName());
                    }
                    saveModel(item, modelSchema, sqlCommand, ModelConflictStrategy.THROW_EXCEPTION);
                }

                final StorageItemChange.Record record = StorageItemChange.<T>builder()
                        .item(item)
                        .itemClass((Class<T>) item.getClass())
                        .type(StorageItemChange.Type.SAVE)
                        .initiator(initiator)
                        .build()
                        .toRecord(storageItemChangeConverter);
                itemChangeSubject.onNext(record);
                itemSaveListener.onResult(record);
            } catch (Exception exception) {
                itemChangeSubject.onError(exception);
                itemSaveListener.onError(new DataStoreException("Error in saving the model.", exception));
            }
        });
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T extends Model> void query(@NonNull Class<T> itemClass,
                                        @NonNull ResultListener<Iterator<T>> queryResultsListener) {
        query(itemClass, null, queryResultsListener);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T extends Model> void query(@NonNull Class<T> itemClass,
                                        @Nullable QueryPredicate predicate,
                                        @NonNull ResultListener<Iterator<T>> queryResultsListener) {
        threadPool.submit(() -> {
            try {
                LOG.debug("Querying item for: " + itemClass.getSimpleName());

                final Set<T> models = new HashSet<>();
                final ModelSchema modelSchema =
                    modelSchemaRegistry.getModelSchemaForModelClass(itemClass.getSimpleName());

                final Cursor cursor = getQueryAllCursor(itemClass.getSimpleName(), predicate);
                if (cursor == null) {
                    throw new DataStoreException("Error in getting a cursor to the " +
                            "table for class: " + itemClass.getSimpleName());
                }

                if (cursor.moveToFirst()) {
                    do {
                        final Map<String, Object> mapForModel = buildMapForModel(
                            itemClass, modelSchema, cursor);
                        models.add(deserializeModelFromRawMap(mapForModel, itemClass));
                    } while (cursor.moveToNext());
                }
                if (!cursor.isClosed()) {
                    cursor.close();
                }

                queryResultsListener.onResult(models.iterator());
            } catch (Exception exception) {
                queryResultsListener.onError(new DataStoreException("Error in querying the model.", exception));
            }
        });
    }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    @Override
    public <T extends Model> void delete(
            @NonNull T item,
            @NonNull StorageItemChange.Initiator initiator,
            @NonNull ResultListener<StorageItemChange.Record> itemDeleteListener) {
        threadPool.submit(() -> {
            try {
                final ModelSchema modelSchema =
                        modelSchemaRegistry.getModelSchemaForModelClass(item.getClass().getSimpleName());
                final SQLiteTable sqLiteTable = SQLiteTable.fromSchema(modelSchema);

                LOG.debug("Deleting item in table: " + sqLiteTable.getName() +
                        " identified by ID: " + item.getId());

                final SqlCommand sqlCommand = sqlCommandFactory.deleteFor(modelSchema, item);
                if (sqlCommand == null || sqlCommand.sqlStatement() == null) {
                    throw new DataStoreException("No delete statement found for the Model: " +
                            modelSchema.getName());
                }

                databaseConnectionHandle.beginTransaction();
                databaseConnectionHandle.execSQL(sqlCommand.sqlStatement());
                databaseConnectionHandle.setTransactionSuccessful();
                databaseConnectionHandle.endTransaction();

                final StorageItemChange.Record record = StorageItemChange.<T>builder()
                        .item(item)
                        .itemClass((Class<T>) item.getClass())
                        .type(StorageItemChange.Type.DELETE)
                        .initiator(initiator)
                        .build()
                        .toRecord(storageItemChangeConverter);
                itemChangeSubject.onNext(record);
                itemDeleteListener.onResult(record);
            } catch (Exception exception) {
                itemChangeSubject.onError(exception);
                itemDeleteListener.onError(
                        new DataStoreException("Error in deleting the model.", exception));
            }
        });
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Observable<StorageItemChange.Record> observe() {
        return itemChangeSubject;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized void terminate() {
        try {
            insertSqlPreparedStatements = null;

            if (itemChangeSubject != null) {
                itemChangeSubject.onComplete();
            }
            if (threadPool != null) {
                threadPool.shutdown();
            }
            if (databaseConnectionHandle != null) {
                databaseConnectionHandle.close();
            }
            if (sqliteOpenHelper != null) {
                sqliteOpenHelper.close();
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
            createIndexCommands.addAll(sqlCommandFactory.createIndexesFor(modelSchema));
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
                    sqlCommandFactory.insertFor(modelSchema)
            );
        }
        return Immutable.of(modifiableMap);
    }

    private <T> void bindPreparedSQLStatementWithValues(@NonNull final T object,
                                                        @NonNull final SqlCommand sqlCommand)
            throws IllegalAccessException {
        final String tableName = sqlCommand.tableName();
        final SQLiteStatement preCompiledInsertStatement = sqlCommand.getCompiledSqlStatement();
        final Set<Field> classFields = FieldFinder.findFieldsIn(object.getClass());
        final Iterator<Field> fieldIterator = classFields.iterator();

        final Cursor cursor = getQueryAllCursor(tableName, null);
        if (cursor == null) {
            throw new IllegalAccessException("Error in getting a cursor to table: " +
                    tableName);
        }
        cursor.moveToFirst();

        final ModelSchema modelSchema = ModelSchemaRegistry.singleton()
                .getModelSchemaForModelClass(tableName);
        final SQLiteTable sqliteTable = SQLiteTable.fromSchema(modelSchema);
        final Map<String, SQLiteColumn> columns = sqliteTable.getColumns();

        while (fieldIterator.hasNext()) {
            final Field field = fieldIterator.next();

            field.setAccessible(true);
            final String fieldName = field.getName();
            final Object fieldValue = field.get(object);

            // Skip if there is no equivalent column for field in object
            final SQLiteColumn column = columns.get(fieldName);
            if (column == null) {
                continue;
            }
            final String columnName = column.isPrimaryKey()
                    ? column.getAliasedName()
                    : column.getName();

            final JavaFieldType javaFieldType;
            if (Model.class.isAssignableFrom(field.getType())) {
                javaFieldType = JavaFieldType.MODEL;
            } else if (Enum.class.isAssignableFrom(field.getType())) {
                javaFieldType = JavaFieldType.ENUM;
            } else {
                javaFieldType = JavaFieldType.from(field.getType().getSimpleName());
            }

            // Move the columns index to 1-based index.
            final int columnIndex = cursor.getColumnIndexOrThrow(columnName) + 1;

            if (fieldValue == null) {
                preCompiledInsertStatement.bindNull(columnIndex);
                continue;
            }

            bindPreCompiledInsertStatementWithJavaFields(
                    preCompiledInsertStatement,
                    fieldValue,
                    columnIndex,
                    javaFieldType);
        }

        if (!cursor.isClosed()) {
            cursor.close();
        }
    }

    private void bindPreCompiledInsertStatementWithJavaFields(
            @NonNull SQLiteStatement preCompiledInsertStatement,
            @NonNull Object fieldValue,
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
            case MODEL:
                preCompiledInsertStatement.bindString(columnIndex, ((Model) fieldValue).getId());
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
            @NonNull Class<T> modelClass,
            @NonNull ModelSchema modelSchema,
            @NonNull Cursor cursor) {
        final Map<String, Object> mapForModel = new HashMap<>();
        final SQLiteTable sqliteTable = SQLiteTable.fromSchema(modelSchema);
        final Map<String, SQLiteColumn> columns = sqliteTable.getColumns();

        for (Map.Entry<String, ModelField> entry : modelSchema.getFields().entrySet()) {
            final String fieldName = entry.getKey();
            try {
                // Skip if there is no equivalent column for field in object
                final SQLiteColumn column = columns.get(fieldName);
                if (column == null) {
                    continue;
                }
                final String columnName = column.isPrimaryKey()
                        ? column.getAliasedName()
                        : column.getName();

                final ModelField modelField = entry.getValue();
                final String fieldGraphQlType = entry.getValue().getTargetType();
                final JavaFieldType fieldJavaType;
                if (modelField.isModel()) {
                    fieldJavaType = JavaFieldType.MODEL;
                } else if (modelField.isEnum()) {
                    fieldJavaType = JavaFieldType.ENUM;
                } else {
                    fieldJavaType = TypeConverter.getJavaTypeForGraphQLType(fieldGraphQlType);
                }

                final int columnIndex = cursor.getColumnIndexOrThrow(columnName);

                final String stringValueFromCursor;
                switch (fieldJavaType) {
                    case STRING:
                        mapForModel.put(fieldName, cursor.getString(columnIndex));
                        break;
                    case MODEL:
                        // Eager load model if the necessary columns are present inside the cursor.
                        // At the time of implementation, cursor should have been joined with these
                        // columns IF AND ONLY IF the model is a foreign key to the inner model.
                        Class<?> classType = modelClass.getDeclaredField(fieldName).getType();
                        @SuppressWarnings("unchecked") // Safe type casting since foreign key is always a model
                        Class<? extends Model> innerModelType = (Class<? extends Model>) classType;
                        String className = innerModelType.getSimpleName();
                        ModelSchema innerModelSchema = ModelSchemaRegistry.singleton()
                                .getModelSchemaForModelClass(className);
                        Map<String, Object> mapForInnerModel = buildMapForModel(
                                innerModelType,
                                innerModelSchema,
                                cursor);
                        mapForModel.put(fieldName, deserializeModelFromRawMap(mapForInnerModel, innerModelType));
                        break;
                    case ENUM:
                        stringValueFromCursor = cursor.getString(columnIndex);
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
                        if (dateInStringFormat != null) {
                            final Date dateInDateFormat = SimpleDateFormat
                                    .getDateInstance()
                                    .parse(dateInStringFormat);
                            mapForModel.put(fieldName, dateInDateFormat);
                        }
                        break;
                    case TIME:
                        final long timeInLongFormat = cursor.getLong(columnIndex);
                        mapForModel.put(fieldName, new Time(timeInLongFormat));
                        break;
                    default:
                        throw new UnsupportedTypeException(fieldJavaType + " is not supported.");
                }
            } catch (Exception exception) {
                mapForModel.put(fieldName, null);
            }
        }
        return mapForModel;
    }

    // Extract the values of the fields of a model and bind the values to the SQLiteStatement
    // and execute the statement.
    private <T extends Model> void saveModel(
            @NonNull T model,
            @NonNull ModelSchema modelSchema,
            @NonNull SqlCommand sqlCommand,
            ModelConflictStrategy modelConflictStrategy) throws IllegalAccessException {
        Objects.requireNonNull(model);
        Objects.requireNonNull(modelSchema);
        Objects.requireNonNull(sqlCommand);
        Objects.requireNonNull(sqlCommand.getCompiledSqlStatement());

        LOG.debug("Writing data to table for: " + model.toString());

        // SQLiteStatement object that represents the pre-compiled/prepared SQLite statements
        // are not thread-safe. Adding a synchronization barrier to access it.
        synchronized (sqlCommand.getCompiledSqlStatement()) {
            final SQLiteStatement compiledSqlStatement = sqlCommand.getCompiledSqlStatement();
            compiledSqlStatement.clearBindings();
            bindPreparedSQLStatementWithValues(model, sqlCommand);
            switch (modelConflictStrategy) {
                case OVERWRITE_EXISTING:
                    compiledSqlStatement.executeUpdateDelete();
                    break;
                case THROW_EXCEPTION:
                    compiledSqlStatement.executeInsert();
                    break;
                default:
                    throw new UnsupportedTypeException("ModelConflictStrategy " +
                            modelConflictStrategy + " is not supported.");
            }
            compiledSqlStatement.clearBindings();
        }

        LOG.debug("Successfully written data to table for: " + model.toString());
    }

    private boolean dataExistsInSQLiteTable(
            @NonNull String tableName,
            @NonNull String columnName,
            @NonNull String columnValue) {
        final Cursor cursor = databaseConnectionHandle.rawQuery(
                "SELECT * FROM " + StringUtils.singleQuote(tableName) +
                        " WHERE " + columnName + " = " +
                        StringUtils.singleQuote(columnValue), null);
        if (cursor.getCount() <= 0) {
            cursor.close();
            return false;
        }
        cursor.close();
        return true;
    }

    private <T extends Model> T deserializeModelFromRawMap(Map<String, Object> mapForModel,
                                                           Class<T> itemClass) throws IOException {
        final String modelInJsonFormat = gson.toJson(mapForModel);
        return gson.getAdapter(itemClass).fromJson(modelInJsonFormat);
    }

    @VisibleForTesting
    Cursor getQueryAllCursor(@NonNull String tableName) {
        return getQueryAllCursor(tableName, null);
    }

    @VisibleForTesting
    Cursor getQueryAllCursor(@NonNull String tableName,
                             @Nullable QueryPredicate predicate) {
        final ModelSchema schema = ModelSchemaRegistry.singleton()
                .getModelSchemaForModelClass(tableName);
        final String rawQuery = sqlCommandFactory.queryFor(schema, predicate).sqlStatement();
        return this.databaseConnectionHandle.rawQuery(rawQuery, null);
    }
}
