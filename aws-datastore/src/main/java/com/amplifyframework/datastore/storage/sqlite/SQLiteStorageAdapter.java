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
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.core.util.ObjectsCompat;

import com.amplifyframework.AmplifyException;
import com.amplifyframework.core.Action;
import com.amplifyframework.core.Amplify;
import com.amplifyframework.core.Consumer;
import com.amplifyframework.core.async.Cancelable;
import com.amplifyframework.core.model.Model;
import com.amplifyframework.core.model.ModelField;
import com.amplifyframework.core.model.ModelProvider;
import com.amplifyframework.core.model.ModelSchema;
import com.amplifyframework.core.model.ModelSchemaRegistry;
import com.amplifyframework.core.model.query.QueryOptions;
import com.amplifyframework.core.model.query.predicate.QueryField;
import com.amplifyframework.core.model.query.predicate.QueryPredicate;
import com.amplifyframework.core.model.query.predicate.QueryPredicateOperation;
import com.amplifyframework.datastore.DataStoreException;
import com.amplifyframework.datastore.model.CompoundModelProvider;
import com.amplifyframework.datastore.model.SystemModelsProviderFactory;
import com.amplifyframework.datastore.storage.LocalStorageAdapter;
import com.amplifyframework.datastore.storage.StorageItemChange;
import com.amplifyframework.datastore.storage.sqlite.adapter.SQLiteColumn;
import com.amplifyframework.datastore.storage.sqlite.adapter.SQLiteTable;
import com.amplifyframework.logging.Logger;
import com.amplifyframework.util.Immutable;
import com.amplifyframework.util.StringUtils;

import com.google.gson.Gson;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import io.reactivex.Completable;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.subjects.PublishSubject;

import static com.amplifyframework.core.model.query.QueryOptions.all;

/**
 * An implementation of {@link LocalStorageAdapter} using {@link android.database.sqlite.SQLiteDatabase}.
 */
public final class SQLiteStorageAdapter implements LocalStorageAdapter {
    private static final Logger LOG = Amplify.Logging.forNamespace("amplify:aws-datastore");

    // Database Version
    private static final int DATABASE_VERSION = 1;

    // Name of the database
    @VisibleForTesting @SuppressWarnings("checkstyle:all") // Keep logger first
    static final String DATABASE_NAME = "AmplifyDatastore.db";

    // Provider of the Models that will be warehouse-able by the DataStore
    // and models that are used internally for DataStore to track metadata
    private final ModelProvider modelsProvider;

    // ModelSchemaRegistry instance that gives the ModelSchema and Model objects
    // based on Model class name lookup mechanism.
    private final ModelSchemaRegistry modelSchemaRegistry;

    // ThreadPool for SQLite operations.
    private final ExecutorService threadPool;

    // Data is read from SQLite and de-serialized using GSON
    // into a strongly typed Java object.
    private final Gson gson;

    // Used to publish events to the observables subscribed.
    private final PublishSubject<StorageItemChange<? extends Model>> itemChangeSubject;

    // Map of tableName => Insert Prepared statement.
    private Map<String, SqlCommand> insertSqlPreparedStatements;

    // Represents a connection to the SQLite database. This database reference
    // can be used to do all SQL operations against the underlying database
    // that this handle represents.
    private SQLiteDatabase databaseConnectionHandle;

    // The helper object controls the lifecycle of database creation, update
    // and opening connection to database.
    private SQLiteStorageHelper sqliteStorageHelper;

    // Factory that produces SQL commands.
    private SQLCommandFactory sqlCommandFactory;

    // Stores the reference to disposable objects for cleanup
    private final CompositeDisposable toBeDisposed;

    /**
     * Construct the SQLiteStorageAdapter object.
     * @param modelSchemaRegistry A registry of schema for all models used by the system
     * @param userModelsProvider Provides the models that will be usable by the DataStore
     * @param systemModelsProvider Provides the models that are used by the DataStore system internally
     */
    private SQLiteStorageAdapter(
            ModelSchemaRegistry modelSchemaRegistry,
            ModelProvider userModelsProvider,
            ModelProvider systemModelsProvider) {
        this.modelSchemaRegistry = modelSchemaRegistry;
        this.modelsProvider = CompoundModelProvider.of(systemModelsProvider, userModelsProvider);
        this.threadPool = Executors.newCachedThreadPool();
        this.insertSqlPreparedStatements = Collections.emptyMap();
        this.gson = new Gson();
        this.itemChangeSubject = PublishSubject.create();
        this.toBeDisposed = new CompositeDisposable();
    }

    /**
     * Gets a SQLiteStorageAdapter that can be initialized to use the provided models.
     * @param modelSchemaRegistry Registry of schema for all models in the system
     * @param userModelsProvider A provider of models that will be represented in SQL
     * @return A SQLiteStorageAdapter that will host the provided models in SQL tables
     */
    @NonNull
    public static SQLiteStorageAdapter forModels(
            @NonNull ModelSchemaRegistry modelSchemaRegistry,
            @NonNull ModelProvider userModelsProvider) {
        return new SQLiteStorageAdapter(
            modelSchemaRegistry,
            Objects.requireNonNull(userModelsProvider),
            SystemModelsProviderFactory.create()
        );
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized void initialize(
            @NonNull Context context,
            @NonNull Consumer<List<ModelSchema>> onSuccess,
            @NonNull Consumer<DataStoreException> onError) {
        Objects.requireNonNull(context);
        Objects.requireNonNull(onSuccess);
        Objects.requireNonNull(onError);

        threadPool.submit(() -> {
            try {
                /*
                 * Start with a fresh registry.
                 */
                modelSchemaRegistry.clear();
                /*
                 * Create {@link ModelSchema} objects for the corresponding {@link Model}.
                 * Any exception raised during this when inspecting the Model classes
                 * through reflection will be notified via the `onError` callback.
                 */
                modelSchemaRegistry.load(modelsProvider.models());

                /*
                 * Create the CREATE TABLE and CREATE INDEX commands for each of the
                 * Models. Instantiate {@link SQLiteStorageHelper} to execute those
                 * create commands.
                 */
                this.sqlCommandFactory = new SQLiteCommandFactory(modelSchemaRegistry);
                CreateSqlCommands createSqlCommands = getCreateCommands(modelsProvider.models());
                sqliteStorageHelper = SQLiteStorageHelper.getInstance(
                        context,
                        DATABASE_NAME,
                        DATABASE_VERSION,
                        createSqlCommands);

                /*
                 * Create and/or open a database. This also invokes
                 * {@link SQLiteStorageHelper#onCreate(SQLiteDatabase)} which executes the tasks
                 * to create tables and indexes. When the function returns without any exception
                 * being thrown, invoke the `onError` callback.
                 *
                 * Errors are thrown when there is no write permission to the database, no space
                 * left in the database for any write operation and other errors thrown while
                 * creating and opening a database. All errors are passed through the
                 * `onError` callback.
                 *
                 * databaseConnectionHandle represents a connection handle to the database.
                 * All database operations will happen through this handle.
                 */
                databaseConnectionHandle = sqliteStorageHelper.getWritableDatabase();
                this.sqlCommandFactory = new SQLiteCommandFactory(modelSchemaRegistry, databaseConnectionHandle);

                /*
                 * Create INSERT INTO TABLE_NAME statements for all SQL tables
                 * and compile them and store in an in-memory map. Later, when a
                 * {@link #save(T, Consumer, Consumer)} operation needs to insert
                 * an object (sql rows) into the database, it can bind the input
                 * values with the prepared insert statement.
                 *
                 * This is done to improve performance of database write operations.
                 */
                this.insertSqlPreparedStatements = getInsertSqlPreparedStatements();

                /*
                 * Detect if the version of the models stored in SQLite is different
                 * from the version passed in through {@link ModelProvider#version()}.
                 * Delete the database if there is a version change.
                 */
                toBeDisposed.add(updateModels().subscribe(
                    () -> onSuccess.accept(
                        Immutable.of(new ArrayList<>(modelSchemaRegistry.getModelSchemaMap().values()))
                    ),
                    throwable -> onError.accept(new DataStoreException(
                        "Error in initializing the SQLiteStorageAdapter",
                        throwable, AmplifyException.TODO_RECOVERY_SUGGESTION
                    ))
                ));
            } catch (Exception exception) {
                onError.accept(new DataStoreException(
                    "Error in initializing the SQLiteStorageAdapter",
                    exception, "See attached exception"
                ));
            }
        });
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T extends Model> void save(
            @NonNull T item,
            @NonNull StorageItemChange.Initiator initiator,
            @NonNull Consumer<StorageItemChange<T>> onSuccess,
            @NonNull Consumer<DataStoreException> onError) {
        Objects.requireNonNull(item);
        Objects.requireNonNull(initiator);
        Objects.requireNonNull(onSuccess);
        Objects.requireNonNull(onError);
        save(item, initiator, null, onSuccess, onError);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T extends Model> void save(
            @NonNull T item,
            @NonNull StorageItemChange.Initiator initiator,
            @Nullable QueryPredicate predicate,
            @NonNull Consumer<StorageItemChange<T>> onSuccess,
            @NonNull Consumer<DataStoreException> onError) {
        Objects.requireNonNull(item);
        Objects.requireNonNull(initiator);
        // Objects.requireNonNull(predicate); Not required!
        Objects.requireNonNull(onSuccess);
        Objects.requireNonNull(onError);

        threadPool.submit(() -> {
            try {
                final ModelSchema modelSchema =
                    modelSchemaRegistry.getModelSchemaForModelInstance(item);
                final SQLiteTable sqliteTable = SQLiteTable.fromSchema(modelSchema);
                final String primaryKeyName = sqliteTable.getPrimaryKeyColumnName();
                final SqlCommand sqlCommand;
                final ModelConflictStrategy modelConflictStrategy;
                final StorageItemChange.Type type;

                if (dataExistsInSQLiteTable(sqliteTable.getName(), primaryKeyName, item.getId())) {
                    type = StorageItemChange.Type.UPDATE;

                    // update model stored in SQLite
                    // update always checks for ID first
                    final QueryPredicateOperation<?> idCheck =
                        QueryField.field(primaryKeyName).eq(item.getId());
                    final QueryPredicate condition = predicate != null
                        ? idCheck.and(predicate)
                        : idCheck;
                    sqlCommand = sqlCommandFactory.updateFor(modelSchema, item, condition);
                    if (!sqlCommand.hasCompiledSqlStatement()) {
                        onError.accept(new DataStoreException(
                            "Error in saving the model. No update statement " +
                                "found for the Model: " + modelSchema.getName(),
                            AmplifyException.TODO_RECOVERY_SUGGESTION
                        ));
                        return;
                    }
                    modelConflictStrategy = ModelConflictStrategy.OVERWRITE_EXISTING;
                } else {
                    // insert model in SQLite
                    type = StorageItemChange.Type.CREATE;

                    sqlCommand = insertSqlPreparedStatements.get(modelSchema.getName());
                    if (sqlCommand == null || !sqlCommand.hasCompiledSqlStatement()) {
                        onError.accept(new DataStoreException(
                            "No insert statement found for the Model: " + modelSchema.getName(),
                            AmplifyException.TODO_RECOVERY_SUGGESTION
                        ));
                        return;
                    }
                    modelConflictStrategy = ModelConflictStrategy.THROW_EXCEPTION;
                }

                saveModel(item, modelSchema, sqlCommand, modelConflictStrategy);
                @SuppressWarnings("unchecked")
                // item.getClass() is Class<? extends Model>, builder wants Class<T>.
                final StorageItemChange<T> change = StorageItemChange.<T>builder()
                    .changeId(item.getId())
                    .item(item)
                    .itemClass((Class<T>) item.getClass())
                    .type(type)
                    .predicate(predicate)
                    .initiator(initiator)
                    .build();
                itemChangeSubject.onNext(change);
                onSuccess.accept(change);
            } catch (DataStoreException dataStoreException) {
                itemChangeSubject.onError(dataStoreException);
                onError.accept(dataStoreException);
            } catch (Exception someOtherTypeOfException) {
                String modelToString = item.getClass().getSimpleName() + "[id=" + item.getId() + "]";
                DataStoreException dataStoreException = new DataStoreException(
                    "Error in saving the model: " + modelToString,
                    someOtherTypeOfException, "See attached exception for details."
                );
                itemChangeSubject.onError(dataStoreException);
                onError.accept(dataStoreException);
            }
        });
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T extends Model> void query(
            @NonNull Class<T> itemClass,
            @NonNull Consumer<Iterator<T>> onSuccess,
            @NonNull Consumer<DataStoreException> onError) {
        Objects.requireNonNull(itemClass);
        Objects.requireNonNull(onSuccess);
        Objects.requireNonNull(onError);
        query(itemClass, all(), onSuccess, onError);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T extends Model> void query(
            @NonNull Class<T> itemClass,
            @NonNull QueryOptions options,
            @NonNull Consumer<Iterator<T>> onSuccess,
            @NonNull Consumer<DataStoreException> onError) {
        Objects.requireNonNull(itemClass);
        Objects.requireNonNull(options);
        Objects.requireNonNull(onSuccess);
        Objects.requireNonNull(onError);

        threadPool.submit(() -> {
            try (Cursor cursor = getQueryAllCursor(itemClass.getSimpleName(), options)) {
                LOG.debug("Querying item for: " + itemClass.getSimpleName());

                final Set<T> models = new HashSet<>();
                final ModelSchema modelSchema =
                    modelSchemaRegistry.getModelSchemaForModelClass(itemClass.getSimpleName());

                if (cursor == null) {
                    onError.accept(new DataStoreException(
                        "Error in getting a cursor to the table for class: " + itemClass.getSimpleName(),
                        AmplifyException.TODO_RECOVERY_SUGGESTION
                    ));
                    return;
                }

                if (cursor.moveToFirst()) {
                    do {
                        final Map<String, Object> mapForModel = buildMapForModel(
                            itemClass, modelSchema, cursor);
                        models.add(deserializeModelFromRawMap(mapForModel, itemClass));
                    } while (cursor.moveToNext());
                }

                onSuccess.accept(models.iterator());
            } catch (Exception exception) {
                onError.accept(new DataStoreException(
                    "Error in querying the model.", exception,
                    "See attached exception for details."
                ));
            }
        });
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T extends Model> void delete(
            @NonNull T item,
            @NonNull StorageItemChange.Initiator initiator,
            @NonNull Consumer<StorageItemChange<T>> onSuccess,
            @NonNull Consumer<DataStoreException> onError
    ) {
        delete(item, initiator, null, onSuccess, onError);
    }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings({"unchecked", "ConstantConditions"}) // item.getClass() has Class<?>, but we assume Class<T>
    @Override
    public <T extends Model> void delete(
            @NonNull T item,
            @NonNull StorageItemChange.Initiator initiator,
            @Nullable QueryPredicate predicate,
            @NonNull Consumer<StorageItemChange<T>> onSuccess,
            @NonNull Consumer<DataStoreException> onError
    ) {
        Objects.requireNonNull(item);
        Objects.requireNonNull(initiator);
        Objects.requireNonNull(onSuccess);
        Objects.requireNonNull(onError);

        threadPool.submit(() -> {
            try {
                final ModelSchema modelSchema = modelSchemaRegistry.getModelSchemaForModelInstance(item);
                final SQLiteTable sqliteTable = SQLiteTable.fromSchema(modelSchema);
                final String primaryKeyName = sqliteTable.getPrimaryKeyColumnName();

                LOG.debug("Deleting item in table: " + sqliteTable.getName() +
                    " identified by ID: " + item.getId());

                // delete always checks for ID first
                final QueryPredicateOperation<?> idCheck =
                    QueryField.field(primaryKeyName).eq(item.getId());
                final QueryPredicate condition = predicate != null
                    ? idCheck.and(predicate)
                    : idCheck;
                final SqlCommand sqlCommand = sqlCommandFactory.deleteFor(modelSchema, item, condition);
                if (sqlCommand == null || sqlCommand.sqlStatement() == null
                    || !sqlCommand.hasCompiledSqlStatement()) {
                    onError.accept(new DataStoreException(
                        "No delete statement found for the Model: " + modelSchema.getName(),
                        AmplifyException.TODO_RECOVERY_SUGGESTION
                    ));
                    return;
                }

                DataStoreException problem = null;
                synchronized (sqlCommand.getCompiledSqlStatement()) {
                    final SQLiteStatement compiledSqlStatement = sqlCommand.getCompiledSqlStatement();
                    compiledSqlStatement.clearBindings();
                    bindStatementToValues(sqlCommand, null);
                    // executeUpdateDelete returns the number of rows affected.
                    final int rowsDeleted = compiledSqlStatement.executeUpdateDelete();
                    if (rowsDeleted != 1) {
                        problem = new DataStoreException(
                            "Wanted to delete one row, but deleted " + rowsDeleted + " rows.",
                            "This is likely a bug. Please report to AWS."
                        );
                    }
                    compiledSqlStatement.clearBindings();
                    if (problem != null) {
                        throw problem;
                    }
                }
                final StorageItemChange<T> change = StorageItemChange.<T>builder()
                    .changeId(item.getId())
                    .item(item)
                    .itemClass((Class<T>) item.getClass())
                    .type(StorageItemChange.Type.DELETE)
                    .predicate(predicate)
                    .initiator(initiator)
                    .build();
                itemChangeSubject.onNext(change);
                onSuccess.accept(change);
            } catch (DataStoreException dataStoreException) {
                itemChangeSubject.onError(dataStoreException);
                onError.accept(dataStoreException);
            } catch (Exception someOtherTypeOfException) {
                DataStoreException dataStoreException = new DataStoreException(
                    "Error in deleting the model.", someOtherTypeOfException,
                    "See attached exception for details."
                );
                itemChangeSubject.onError(dataStoreException);
                onError.accept(dataStoreException);
            }
        });
    }

    /**
     * {@inheritDoc}
     */
    @NonNull
    @Override
    public Cancelable observe(
            @NonNull Consumer<StorageItemChange<? extends Model>> onItemChanged,
            @NonNull Consumer<DataStoreException> onObservationError,
            @NonNull Action onObservationComplete) {
        Objects.requireNonNull(onItemChanged);
        Objects.requireNonNull(onObservationError);
        Objects.requireNonNull(onObservationComplete);
        Disposable disposable = itemChangeSubject.subscribe(
            onItemChanged::accept,
            failure -> {
                if (failure instanceof DataStoreException) {
                    onObservationError.accept((DataStoreException) failure);
                    return;
                }
                onObservationError.accept(new DataStoreException(
                    "Failed to observe items in storage adapter.",
                    failure,
                    "Inspect the failure details."
                ));
            },
            onObservationComplete::call
        );
        return disposable::dispose;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized void terminate() throws DataStoreException {
        try {
            insertSqlPreparedStatements = null;

            if (toBeDisposed != null) {
                toBeDisposed.dispose();
            }
            if (itemChangeSubject != null) {
                itemChangeSubject.onComplete();
            }
            if (threadPool != null) {
                threadPool.shutdown();
            }
            if (databaseConnectionHandle != null) {
                databaseConnectionHandle.close();
            }
            if (sqliteStorageHelper != null) {
                sqliteStorageHelper.close();
            }
        } catch (Exception exception) {
            throw new DataStoreException("Error in terminating the SQLiteStorageAdapter.", exception,
                    "See attached exception for details.");
        }
    }

    private CreateSqlCommands getCreateCommands(@NonNull Set<Class<? extends Model>> models) {
        final Set<SqlCommand> createTableCommands = new HashSet<>();
        final Set<SqlCommand> createIndexCommands = new HashSet<>();
        for (Class<? extends Model> model : models) {
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
                modelSchemaRegistry.getModelSchemaMap().entrySet();
        for (final Map.Entry<String, ModelSchema> entry : modelSchemaEntrySet) {
            final String tableName = entry.getKey();
            final ModelSchema modelSchema = entry.getValue();
            modifiableMap.put(
                    tableName,
                    sqlCommandFactory.insertFor(modelSchema)
            );
        }
        return Immutable.of(modifiableMap);
    }

    // Binds each value inside list onto compiled statement in order
    private void bindStatementToValues(
            @NonNull SqlCommand sqlCommand,
            @Nullable Model model
    ) throws DataStoreException {
        final SQLiteStatement compiledSqlStatement = sqlCommand.getCompiledSqlStatement();
        // 1-based index for columns
        int columnIndex = 1;

        // bind model field values to sql columns
        if (model != null) {
            final Class<? extends Model> modelClass = model.getClass();
            final SQLiteModelFieldTypeConverter converter =
                    new SQLiteModelFieldTypeConverter(modelClass, modelSchemaRegistry, gson);
            final ModelSchema schema = modelSchemaRegistry.getModelSchemaForModelInstance(model);
            final Map<String, ModelField> modelFields = schema.getFields();

            final List<SQLiteColumn> columns = sqlCommand.getColumns();

            for (SQLiteColumn column : columns) {
                final ModelField modelField = Objects.requireNonNull(modelFields.get(column.getFieldName()));
                final Object fieldValue = converter.convertValueFromTarget(model, modelField);
                bindValueToStatement(compiledSqlStatement, columnIndex, fieldValue);
                columnIndex++;
            }
        }

        // apply stored bindings after columns were bound
        for (Object binding : sqlCommand.getBindings()) {
            bindValueToStatement(compiledSqlStatement, columnIndex, binding);
            columnIndex++;
        }
    }

    private void bindValueToStatement(
            @NonNull SQLiteStatement statement,
            int columnIndex,
            @Nullable Object value
    ) throws DataStoreException {
        System.out.println("SQLiteStorageAdapter.bindValueToStatement");
        System.out.println("value = " + value);
        if (value == null) {
            statement.bindNull(columnIndex);
        } else if (value instanceof String) {
            statement.bindString(columnIndex, (String) value);
        } else if (value instanceof Long) {
            statement.bindLong(columnIndex, (Long) value);
        } else if (value instanceof Integer) {
            statement.bindLong(columnIndex, (Integer) value);
        } else if (value instanceof Float) {
            statement.bindDouble(columnIndex, (Float) value);
        } else {
            throw new DataStoreException("", "");
        }
    }

    private <T extends Model> Map<String, Object> buildMapForModel(
            @NonNull Class<T> modelClass,
            @NonNull ModelSchema modelSchema,
            @NonNull Cursor cursor) throws DataStoreException {
        final Map<String, Object> mapForModel = new HashMap<>();

        final SQLiteModelFieldTypeConverter modelFieldTypeConverter =
                new SQLiteModelFieldTypeConverter(modelClass, modelSchemaRegistry, gson);

        for (Map.Entry<String, ModelField> entry : modelSchema.getFields().entrySet()) {
            final String fieldName = entry.getKey();
            final Object value = modelFieldTypeConverter.convertValueFromSource(cursor, entry.getValue());
            mapForModel.put(fieldName, value);
        }
        return mapForModel;
    }

    // Extract the values of the fields of a model and bind the values to the SQLiteStatement
    // and execute the statement.
    // throws DataStoreException on failure to save
    private <T extends Model> void saveModel(
            @NonNull T model,
            @NonNull ModelSchema modelSchema,
            @NonNull SqlCommand sqlCommand,
            @NonNull ModelConflictStrategy modelConflictStrategy)
            throws IllegalAccessException, DataStoreException {
        Objects.requireNonNull(model);
        Objects.requireNonNull(modelSchema);
        Objects.requireNonNull(sqlCommand);

        LOG.debug("Writing data to table for: " + model.toString());

        // SQLiteStatement object that represents the pre-compiled/prepared SQLite statements
        // are not thread-safe. Adding a synchronization barrier to access it.
        synchronized (sqlCommand.getCompiledSqlStatement()) {
            final SQLiteStatement compiledSqlStatement = sqlCommand.getCompiledSqlStatement();
            compiledSqlStatement.clearBindings();

            bindStatementToValues(sqlCommand, model);

            DataStoreException problem = null;
            switch (modelConflictStrategy) {
                case OVERWRITE_EXISTING:
                    // executeUpdateDelete returns the number of rows affected.
                    final int rowsUpdated = compiledSqlStatement.executeUpdateDelete();
                    if (rowsUpdated != 1) {
                        problem = new DataStoreException(
                            "Wanted to update 1 row, but updated " + rowsUpdated + " rows!",
                            "This is likely a bug; please report to AWS."
                        );
                    }
                    break;
                case THROW_EXCEPTION:
                    // executeInsert returns id if successful, -1 otherwise.
                    if (compiledSqlStatement.executeInsert() == -1) {
                        problem = new DataStoreException(
                            "Failed to insert any item in to database.",
                            "This is likely a bug; please report to AWS."
                        );
                    }
                    break;
                default:
                    problem = new DataStoreException(
                        "ModelConflictStrategy " + modelConflictStrategy + " is not supported.",
                        "This is likely a bug; please report to AWS."
                    );
            }

            compiledSqlStatement.clearBindings();

            if (problem != null) {
                throw problem;
            }
        }
    }

    private boolean dataExistsInSQLiteTable(
            @NonNull String tableName,
            @NonNull String columnName,
            @NonNull String columnValue) {
        // SELECT * FROM '{tableName}' WHERE {columnName} = '{columnValue}'
        final String queryString = new StringBuilder()
                .append(SqlKeyword.SELECT).append(SqlKeyword.DELIMITER)
                .append("*").append(SqlKeyword.DELIMITER)
                .append(SqlKeyword.FROM).append(SqlKeyword.DELIMITER)
                .append(StringUtils.singleQuote(tableName)).append(SqlKeyword.DELIMITER)
                .append(SqlKeyword.WHERE).append(SqlKeyword.DELIMITER)
                .append(columnName).append(SqlKeyword.DELIMITER)
                .append(SqlKeyword.EQUAL).append(SqlKeyword.DELIMITER)
                .append(StringUtils.singleQuote(columnValue))
                .toString();
        try (Cursor cursor = databaseConnectionHandle.rawQuery(queryString, null)) {
            return cursor.getCount() > 0;
        }
    }

    /*
     * Detect if the version of the models stored in SQLite is different
     * from the version passed in through {@link ModelProvider#version()}.
     * Drop all tables if the version has changed.
     */
    private Completable updateModels() {
        return PersistentModelVersion.fromLocalStorage(this)
                .flatMap(iterator -> {
                    if (iterator.hasNext()) {
                        LOG.verbose("Successfully read model version from local storage. " +
                                "Checking if the model version need to be updated...");
                        PersistentModelVersion persistentModelVersion = iterator.next();
                        String oldVersion = persistentModelVersion.getVersion();
                        String newVersion = modelsProvider.version();
                        if (!ObjectsCompat.equals(oldVersion, newVersion)) {
                            LOG.debug("Updating version as it has changed from " +
                                    oldVersion + " to " + newVersion);
                            Objects.requireNonNull(sqliteStorageHelper);
                            Objects.requireNonNull(databaseConnectionHandle);
                            sqliteStorageHelper.update(
                                    databaseConnectionHandle,
                                    oldVersion,
                                    newVersion);
                        }
                    }
                    return PersistentModelVersion.saveToLocalStorage(
                            this,
                            new PersistentModelVersion(modelsProvider.version()));
                }).ignoreElement();
    }

    private <T extends Model> T deserializeModelFromRawMap(
            @NonNull Map<String, Object> mapForModel,
            @NonNull Class<T> itemClass) throws IOException {
        final String modelInJsonFormat = gson.toJson(mapForModel);
        return gson.getAdapter(itemClass).fromJson(modelInJsonFormat);
    }

    @VisibleForTesting
    Cursor getQueryAllCursor(@NonNull String tableName) throws DataStoreException {
        return getQueryAllCursor(tableName, all());
    }

    @SuppressWarnings("WeakerAccess")
    @VisibleForTesting
    Cursor getQueryAllCursor(@NonNull String tableName,
                             @NonNull QueryOptions options) throws DataStoreException {
        final ModelSchema schema = modelSchemaRegistry.getModelSchemaForModelClass(tableName);
        final SqlCommand sqlCommand = sqlCommandFactory.queryFor(schema, options);
        final String rawQuery = sqlCommand.sqlStatement();
        final String[] bindings = sqlCommand.getBindingsAsArray();
        return this.databaseConnectionHandle.rawQuery(rawQuery, bindings);
    }
}
