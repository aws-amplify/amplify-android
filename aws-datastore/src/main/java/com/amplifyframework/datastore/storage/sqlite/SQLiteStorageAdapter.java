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
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import androidx.annotation.NonNull;
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
import com.amplifyframework.core.model.query.Where;
import com.amplifyframework.core.model.query.predicate.QueryField;
import com.amplifyframework.core.model.query.predicate.QueryPredicate;
import com.amplifyframework.core.model.query.predicate.QueryPredicates;
import com.amplifyframework.datastore.DataStoreException;
import com.amplifyframework.datastore.appsync.ModelConverter;
import com.amplifyframework.datastore.appsync.SerializedModel;
import com.amplifyframework.datastore.model.CompoundModelProvider;
import com.amplifyframework.datastore.model.SystemModelsProviderFactory;
import com.amplifyframework.datastore.storage.LocalStorageAdapter;
import com.amplifyframework.datastore.storage.StorageItemChange;
import com.amplifyframework.datastore.storage.sqlite.adapter.SQLiteColumn;
import com.amplifyframework.datastore.storage.sqlite.adapter.SQLiteTable;
import com.amplifyframework.logging.Logger;
import com.amplifyframework.util.GsonFactory;
import com.amplifyframework.util.Immutable;

import com.google.gson.Gson;

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
import java.util.concurrent.TimeUnit;

import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.subjects.PublishSubject;
import io.reactivex.rxjava3.subjects.Subject;

/**
 * An implementation of {@link LocalStorageAdapter} using {@link android.database.sqlite.SQLiteDatabase}.
 */
public final class SQLiteStorageAdapter implements LocalStorageAdapter {
    private static final Logger LOG = Amplify.Logging.forNamespace("amplify:aws-datastore");
    private static final long THREAD_POOL_TERMINATE_TIMEOUT = TimeUnit.SECONDS.toMillis(5);
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
    private ExecutorService threadPool;

    // Data is read from SQLite and de-serialized using GSON
    // into a strongly typed Java object.
    private final Gson gson;

    // Used to publish events to the observables subscribed.
    private final Subject<StorageItemChange<? extends Model>> itemChangeSubject;

    // Represents a connection to the SQLite database. This database reference
    // can be used to do all SQL operations against the underlying database
    // that this handle represents.
    private SQLiteDatabase databaseConnectionHandle;

    // The helper object controls the lifecycle of database creation, update
    // and opening connection to database.
    private SQLiteStorageHelper sqliteStorageHelper;

    // Factory that produces SQL commands.
    private SQLCommandFactory sqlCommandFactory;

    // The helper object to iterate through associated models of a given model.
    private SQLiteModelTree sqliteModelTree;

    // Stores the reference to disposable objects for cleanup
    private final CompositeDisposable toBeDisposed;

    // Need to keep a reference to the app context so we can
    // re-initialize the adapter after deleting the file in the clear() method
    private Context context;

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
        this.gson = GsonFactory.instance();
        this.itemChangeSubject = PublishSubject.<StorageItemChange<? extends Model>>create().toSerialized();
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
        this.threadPool = Executors.newCachedThreadPool();
        this.context = context;
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
                modelSchemaRegistry.register(modelsProvider.modelSchemas());

                /*
                 * Create the CREATE TABLE and CREATE INDEX commands for each of the
                 * Models. Instantiate {@link SQLiteStorageHelper} to execute those
                 * create commands.
                 */
                this.sqlCommandFactory = new SQLiteCommandFactory(modelSchemaRegistry);
                CreateSqlCommands createSqlCommands = getCreateCommands(modelsProvider.modelNames());
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
                 * Create helper instance that can traverse through model relations.
                 */
                this.sqliteModelTree = new SQLiteModelTree(
                    modelSchemaRegistry,
                    databaseConnectionHandle
                );

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
            @NonNull QueryPredicate predicate,
            @NonNull Consumer<StorageItemChange<T>> onSuccess,
            @NonNull Consumer<DataStoreException> onError) {
        Objects.requireNonNull(item);
        Objects.requireNonNull(initiator);
        Objects.requireNonNull(predicate);
        Objects.requireNonNull(onSuccess);
        Objects.requireNonNull(onError);
        threadPool.submit(() -> {
            try {
                final String modelName = getModelName(item);
                final ModelSchema modelSchema = modelSchemaRegistry.getModelSchemaForModelClass(modelName);

                final StorageItemChange.Type writeType;
                SerializedModel patchItem = null;
                if (modelExists(item, QueryPredicates.all())) {
                    // if data exists already, then UPDATE the row
                    writeType = StorageItemChange.Type.UPDATE;

                    // Check if existing data meets the condition
                    if (!modelExists(item, predicate)) {
                        throw new DataStoreException(
                            "Save failed because condition did not match existing model instance.",
                            "The save will continue to fail until the model instance is updated."
                        );
                    }
                    if (initiator == StorageItemChange.Initiator.DATA_STORE_API) {
                        // When saving items via the DataStore API, compute a SerializedModel containing only the fields
                        // that differ from the model currently in the local storage.  This is not necessary when save
                        // is initiated by the sync engine, so skip it for optimization to avoid the extra SQL query.
                        patchItem = SerializedModel.difference(item, query(item), modelSchema);
                    }
                } else if (!QueryPredicates.all().equals(predicate)) {
                    // insert not permitted with a condition
                    throw new DataStoreException(
                        "Conditional update must be performed against an already existing data. " +
                            "Insertion is not permitted while using a predicate.",
                        "Please save without specifying a predicate."
                    );
                } else {
                    // if data doesn't exist yet, then INSERT a new row
                    writeType = StorageItemChange.Type.CREATE;
                }

                // execute local save
                writeData(item, writeType);

                // publish successful save
                StorageItemChange<T> change = StorageItemChange.<T>builder()
                        .item(item)
                        .patchItem(patchItem != null ? patchItem : SerializedModel.create(item, modelSchema))
                        .modelSchema(modelSchema)
                        .type(writeType)
                        .predicate(predicate)
                        .initiator(initiator)
                        .build();
                itemChangeSubject.onNext(change);
                onSuccess.accept(change);
            } catch (DataStoreException dataStoreException) {
                onError.accept(dataStoreException);
            } catch (Exception someOtherTypeOfException) {
                String modelToString = getModelName(item) + "[id=" + item.getId() + "]";
                DataStoreException dataStoreException = new DataStoreException(
                    "Error in saving the model: " + modelToString,
                    someOtherTypeOfException, "See attached exception for details."
                );
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

                final List<T> models = new ArrayList<>();
                final ModelSchema modelSchema =
                    modelSchemaRegistry.getModelSchemaForModelClass(itemClass.getSimpleName());
                final SQLiteModelFieldTypeConverter converter =
                    new SQLiteModelFieldTypeConverter(modelSchema, modelSchemaRegistry, gson);

                if (cursor == null) {
                    onError.accept(new DataStoreException(
                        "Error in getting a cursor to the table for class: " + itemClass.getSimpleName(),
                        AmplifyException.TODO_RECOVERY_SUGGESTION
                    ));
                }

                if (cursor.moveToFirst()) {
                    do {
                        Map<String, Object> map = converter.buildMapForModel(cursor);
                        models.add(ModelConverter.fromMap(map, itemClass));
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
    @SuppressWarnings("unchecked")
    @Override
    public void query(
            @NonNull String modelName,
            @NonNull QueryOptions options,
            @NonNull Consumer<Iterator<? extends Model>> onSuccess,
            @NonNull Consumer<DataStoreException> onError) {
        Objects.requireNonNull(modelName);
        Objects.requireNonNull(options);
        Objects.requireNonNull(onSuccess);
        Objects.requireNonNull(onError);

        threadPool.submit(() -> {
            try (Cursor cursor = getQueryAllCursor(modelName, options)) {
                LOG.debug("Querying item for: " + modelName);

                final List<Model> models = new ArrayList<>();
                final ModelSchema modelSchema =
                        modelSchemaRegistry.getModelSchemaForModelClass(modelName);
                final SQLiteModelFieldTypeConverter converter =
                    new SQLiteModelFieldTypeConverter(modelSchema, modelSchemaRegistry, gson);

                if (cursor == null) {
                    onError.accept(new DataStoreException(
                            "Error in getting a cursor to the table for class: " + modelName,
                            AmplifyException.TODO_RECOVERY_SUGGESTION
                    ));
                    return;
                }

                if (cursor.moveToFirst()) {
                    do {
                        final Map<String, Object> serializedData = new HashMap<>();
                        for (Map.Entry<String, Object> entry : converter.buildMapForModel(cursor).entrySet()) {
                            ModelField field = modelSchema.getFields().get(entry.getKey());
                            if (field == null || entry.getValue() == null) {
                                // Skip it
                            } else if (field.isModel()) {
                                String id = (String) ((Map<String, Object>) entry.getValue()).get("id");
                                serializedData.put(entry.getKey(), SerializedModel.builder()
                                    .serializedData(Collections.singletonMap("id", id))
                                    .modelSchema(null)
                                    .build()
                                );
                            } else {
                                serializedData.put(entry.getKey(), entry.getValue());
                            }
                        }
                        SerializedModel model = SerializedModel.builder()
                            .serializedData(serializedData)
                            .modelSchema(modelSchema)
                            .build();
                        models.add(model);
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
            @NonNull QueryPredicate predicate,
            @NonNull Consumer<StorageItemChange<T>> onSuccess,
            @NonNull Consumer<DataStoreException> onError
    ) {
        Objects.requireNonNull(item);
        Objects.requireNonNull(initiator);
        Objects.requireNonNull(predicate);
        Objects.requireNonNull(onSuccess);
        Objects.requireNonNull(onError);

        threadPool.submit(() -> {
            try {
                final String modelName = getModelName(item);
                final ModelSchema modelSchema = modelSchemaRegistry.getModelSchemaForModelClass(modelName);

                // Check if data being deleted exists; "Succeed" deletion in that case.
                if (!modelExists(item, QueryPredicates.all())) {
                    LOG.verbose(modelName + " model with id = " + item.getId() + " does not exist.");
                    // Pass back item change instance without publishing it.
                    onSuccess.accept(StorageItemChange.<T>builder()
                        .item(item)
                        .patchItem(SerializedModel.create(item, modelSchema))
                        .modelSchema(modelSchema)
                        .type(StorageItemChange.Type.DELETE)
                        .predicate(predicate)
                        .initiator(initiator)
                        .build());
                    return;
                }

                // Check if existing data meets the condition
                if (!modelExists(item, predicate)) {
                    throw new DataStoreException(
                        "Deletion failed because condition did not match existing model instance.",
                        "The deletion will continue to fail until the model instance is updated."
                    );
                }

                // identify items affected by cascading delete before deleting them
                List<Model> cascadedModels = sqliteModelTree.descendantsOf(Collections.singleton(item));

                // execute local deletion
                writeData(item, StorageItemChange.Type.DELETE);

                // publish cascaded deletions
                for (Model cascadedModel : cascadedModels) {
                    ModelSchema schema = modelSchemaRegistry.getModelSchemaForModelInstance(cascadedModel);
                    itemChangeSubject.onNext(StorageItemChange.builder()
                        .item(cascadedModel)
                        .patchItem(SerializedModel.create(cascadedModel, schema))
                        .modelSchema(schema)
                        .type(StorageItemChange.Type.DELETE)
                        .predicate(QueryPredicates.all())
                        .initiator(initiator)
                        .build());
                }

                // publish successful deletion of top-level item
                StorageItemChange<T> change = StorageItemChange.<T>builder()
                        .item(item)
                        .patchItem(SerializedModel.create(item, modelSchema))
                        .modelSchema(modelSchema)
                        .type(StorageItemChange.Type.DELETE)
                        .predicate(predicate)
                        .initiator(initiator)
                        .build();
                itemChangeSubject.onNext(change);
                onSuccess.accept(change);
            } catch (DataStoreException dataStoreException) {
                onError.accept(dataStoreException);
            } catch (Exception someOtherTypeOfException) {
                DataStoreException dataStoreException = new DataStoreException(
                    "Error in deleting the model.", someOtherTypeOfException,
                    "See attached exception for details."
                );
                onError.accept(dataStoreException);
            }
        });
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T extends Model> void delete(
            @NonNull Class<T> itemClass,
            @NonNull StorageItemChange.Initiator initiator,
            @NonNull QueryPredicate predicate,
            @NonNull Action onSuccess,
            @NonNull Consumer<DataStoreException> onError
    ) {
        Objects.requireNonNull(itemClass);
        Objects.requireNonNull(initiator);
        Objects.requireNonNull(predicate);
        Objects.requireNonNull(onSuccess);
        Objects.requireNonNull(onError);

        threadPool.submit(() -> {
            try (Cursor cursor = getQueryAllCursor(itemClass.getSimpleName(), Where.matches(predicate))) {
                final ModelSchema modelSchema = modelSchemaRegistry.getModelSchemaForModelClass(itemClass);
                final SQLiteTable sqliteTable = SQLiteTable.fromSchema(modelSchema);
                final String primaryKeyName = sqliteTable.getPrimaryKey().getAliasedName();

                // identify items that meet the predicate
                List<T> items = new ArrayList<>();
                if (cursor != null && cursor.moveToFirst()) {
                    int index = cursor.getColumnIndexOrThrow(primaryKeyName);
                    do {
                        String id = cursor.getString(index);
                        String dummyJson = gson.toJson(Collections.singletonMap("id", id));
                        T dummyItem = gson.fromJson(dummyJson, itemClass);
                        items.add(dummyItem);
                    } while (cursor.moveToNext());
                }

                // identify every model to delete as a result of this operation
                List<Model> modelsToDelete = new ArrayList<>(items);
                List<Model> cascadedModels = sqliteModelTree.descendantsOf(items);
                modelsToDelete.addAll(cascadedModels);

                // execute local deletions
                SqlCommand sqlCommand = sqlCommandFactory.deleteFor(modelSchema, predicate);
                executeStatement(sqlCommand.getCompiledSqlStatement(), sqlCommand.getBindings());

                // publish every deletion
                for (Model model : modelsToDelete) {
                    ModelSchema schema = modelSchemaRegistry.getModelSchemaForModelInstance(model);
                    itemChangeSubject.onNext(StorageItemChange.builder()
                            .item(model)
                            .patchItem(SerializedModel.create(model, schema))
                            .modelSchema(schema)
                            .type(StorageItemChange.Type.DELETE)
                            .predicate(QueryPredicates.all())
                            .initiator(initiator)
                            .build());
                }
                onSuccess.call();
            } catch (DataStoreException dataStoreException) {
                onError.accept(dataStoreException);
            } catch (Exception someOtherTypeOfException) {
                DataStoreException dataStoreException = new DataStoreException(
                        "Error in deleting models.", someOtherTypeOfException,
                        "See attached exception for details."
                );
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
            if (toBeDisposed != null) {
                toBeDisposed.clear();
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

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized void clear(@NonNull Action onComplete,
                                   @NonNull Consumer<DataStoreException> onError) {
        try {
            LOG.debug("Shutting down thread pool for the storage adapter.");
            threadPool.shutdown();
            if (!threadPool.awaitTermination(THREAD_POOL_TERMINATE_TIMEOUT, TimeUnit.MILLISECONDS)) {
                threadPool.shutdownNow();
            }
            LOG.debug("Storage adapter thread pool shutdown.");
        } catch (InterruptedException exception) {
            LOG.warn("Storage adapter thread pool was interrupted during shutdown.", exception);
        }
        sqliteStorageHelper.close();
        databaseConnectionHandle.close();
        LOG.debug("Clearing DataStore.");
        if (!context.deleteDatabase(DATABASE_NAME)) {
            DataStoreException dataStoreException = new DataStoreException(
                "Error while trying to clear data from the local DataStore storage.",
                "See attached exception for details.");
            onError.accept(dataStoreException);
        }
        LOG.debug("DataStore cleared. Re-initializing storage adapter.");

        //Re-initialize the adapter.
        initialize(context,
            schemaList -> onComplete.call(),
            exception -> onError.accept(new DataStoreException(
                "Error occurred while trying to re-initialize the storage adapter",
                String.valueOf(exception.getMessage())
            ))
        );
    }

    private CreateSqlCommands getCreateCommands(@NonNull Set<String> modelNames) {
        final Set<SqlCommand> createTableCommands = new HashSet<>();
        final Set<SqlCommand> createIndexCommands = new HashSet<>();
        for (String modelName : modelNames) {
            final ModelSchema modelSchema =
                modelSchemaRegistry.getModelSchemaForModelClass(modelName);
            createTableCommands.add(sqlCommandFactory.createTableFor(modelSchema));
            createIndexCommands.addAll(sqlCommandFactory.createIndexesFor(modelSchema));
        }
        return new CreateSqlCommands(createTableCommands, createIndexCommands);
    }

    // extract model field values to save in database
    private List<Object> extractFieldValues(@NonNull Model model) throws DataStoreException {
        final String modelName = getModelName(model);
        final ModelSchema schema = modelSchemaRegistry.getModelSchemaForModelClass(modelName);
        final SQLiteTable table = SQLiteTable.fromSchema(schema);
        final SQLiteModelFieldTypeConverter converter =
                new SQLiteModelFieldTypeConverter(schema, modelSchemaRegistry, gson);
        final Map<String, ModelField> modelFields = schema.getFields();
        final List<Object> bindings = new ArrayList<>();
        for (SQLiteColumn column : table.getSortedColumns()) {
            final ModelField modelField = Objects.requireNonNull(modelFields.get(column.getFieldName()));
            final Object fieldValue = converter.convertValueFromTarget(model, modelField);
            bindings.add(fieldValue);
        }
        return bindings;
    }

    private <T extends Model> void writeData(
            T item,
            StorageItemChange.Type writeType
    ) throws DataStoreException {
        final String modelName = getModelName(item);
        final ModelSchema modelSchema =
                modelSchemaRegistry.getModelSchemaForModelClass(modelName);
        final SQLiteTable sqliteTable = SQLiteTable.fromSchema(modelSchema);
        final String primaryKeyName = sqliteTable.getPrimaryKeyColumnName();
        final QueryPredicate matchId = QueryField.field(primaryKeyName).eq(item.getId());

        // Generate SQL command for given action
        final SqlCommand sqlCommand;
        final List<Object> bindings;
        switch (writeType) {
            case CREATE:
                LOG.verbose("Creating item in " + sqliteTable.getName() +
                        " identified by ID: " + item.getId());
                sqlCommand = sqlCommandFactory.insertFor(modelSchema);
                bindings = extractFieldValues(item); // VALUES clause
                break;
            case UPDATE:
                LOG.verbose("Updating item in " + sqliteTable.getName() +
                        " identified by ID: " + item.getId());
                sqlCommand = sqlCommandFactory.updateFor(modelSchema, matchId);
                bindings = extractFieldValues(item); // SET clause
                bindings.addAll(sqlCommand.getBindings()); // WHERE clause
                break;
            case DELETE:
                LOG.verbose("Deleting item in " + sqliteTable.getName() +
                        " identified by ID: " + item.getId());
                sqlCommand = sqlCommandFactory.deleteFor(modelSchema, matchId);
                bindings = sqlCommand.getBindings(); // WHERE clause
                break;
            default:
                throw new DataStoreException(
                    "Unexpected change was requested: " + writeType.name(),
                    "Valid storage changes are CREATE, UPDATE, and DELETE."
                );
        }

        executeStatement(sqlCommand.getCompiledSqlStatement(), bindings);
    }

    private synchronized void executeStatement(
            SQLiteStatement sqliteStatement,
            List<Object> bindings
    ) throws DataStoreException {
        if (sqliteStatement == null) {
            throw new DataStoreException(
                "Compiled SQLite statement cannot be null.",
                AmplifyException.REPORT_BUG_TO_AWS_SUGGESTION
            );
        }

        try {
            bindValuesToStatement(sqliteStatement, bindings);
            sqliteStatement.execute();
        } catch (SQLException sqlException) {
            throw new DataStoreException(
                "Invalid SQL statement: " + sqliteStatement,
                sqlException,
                AmplifyException.REPORT_BUG_TO_AWS_SUGGESTION
            );
        }
    }

    private synchronized void bindValuesToStatement(
            SQLiteStatement statement,
            List<Object> values
    ) throws DataStoreException {
        // remove any bindings if there is any
        statement.clearBindings();

        // 1-based index for columns
        int columnIndex = 1;

        // apply stored bindings after columns were bound
        for (Object value : Objects.requireNonNull(values)) {
            bindValueToStatement(statement, columnIndex++, value);
        }
    }

    private synchronized void bindValueToStatement(
            SQLiteStatement statement,
            int columnIndex,
            Object value
    ) throws DataStoreException {
        LOG.verbose("SQLiteStorageAdapter.bindValueToStatement(..., value = " + value);
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
        } else if (value instanceof Double) {
            statement.bindDouble(columnIndex, (Double) value);
        } else {
            throw new DataStoreException(
                    "Failed to bind " + value + " to SQL statement. " +
                            value.getClass().getSimpleName() + " is an unsupported type.",
                    AmplifyException.REPORT_BUG_TO_AWS_SUGGESTION
            );
        }
    }

    private boolean modelExists(Model model, QueryPredicate predicate) throws DataStoreException {
        final String modelName = getModelName(model);
        final ModelSchema schema = modelSchemaRegistry.getModelSchemaForModelClass(modelName);
        final SQLiteTable table = SQLiteTable.fromSchema(schema);
        final String tableName = table.getName();
        final String primaryKeyName = table.getPrimaryKeyColumnName();

        final QueryPredicate matchId = QueryField.field(primaryKeyName).eq(model.getId());
        final QueryPredicate condition = matchId.and(predicate);
        try (Cursor cursor = getQueryAllCursor(tableName, Where.matches(condition))) {
            return cursor.moveToFirst();
        }
    }

    /**
     * Helper method to synchronously query for a single model instance.  Used before any save initiated by
     * DATASTORE_API in order to determine which fields have changed.
     * @param model a Model that we want to query for the same type and id in SQLite.
     * @return the Model instance from SQLite, if it exists, otherwise null.
     */
    private Model query(Model model) {
        Iterator<? extends Model> result = Single.<Iterator<? extends Model>>create(emitter -> {
            if (model instanceof SerializedModel) {
                query(getModelName(model), Where.id(model.getId()), emitter::onSuccess, emitter::onError);
            } else {
                query(model.getClass(), Where.id(model.getId()), emitter::onSuccess, emitter::onError);
            }
        }).blockingGet();
        return result.hasNext() ? result.next() : null;
    }

    /*
     * Detect if the version of the models stored in SQLite is different
     * from the version passed in through {@link ModelProvider#version()}.
     * Drop all tables if the version has changed.
     */
    private Completable updateModels() {
        return PersistentModelVersion.fromLocalStorage(this).flatMap(iterator -> {
            if (iterator.hasNext()) {
                LOG.verbose("Successfully read model version from local storage. " +
                    "Checking if the model version need to be updated...");
                PersistentModelVersion persistentModelVersion = iterator.next();
                String oldVersion = persistentModelVersion.getVersion();
                String newVersion = modelsProvider.version();
                if (!ObjectsCompat.equals(oldVersion, newVersion)) {
                    LOG.debug("Updating version as it has changed from " + oldVersion + " to " + newVersion);
                    Objects.requireNonNull(sqliteStorageHelper);
                    Objects.requireNonNull(databaseConnectionHandle);
                    sqliteStorageHelper.update(databaseConnectionHandle, oldVersion, newVersion);
                }
            }
            PersistentModelVersion persistentModelVersion = new PersistentModelVersion(modelsProvider.version());
            return PersistentModelVersion.saveToLocalStorage(this, persistentModelVersion);
        }).ignoreElement();
    }

    private String getModelName(@NonNull Model model) {
        if (model.getClass() == SerializedModel.class) {
            return ((SerializedModel) model).getModelName();
        } else {
            return model.getClass().getSimpleName();
        }
    }

    private Cursor getQueryAllCursor(
            @NonNull String tableName,
            @NonNull QueryOptions options
    ) throws DataStoreException {
        final ModelSchema schema = modelSchemaRegistry.getModelSchemaForModelClass(tableName);
        final SqlCommand sqlCommand = sqlCommandFactory.queryFor(schema, options);
        final String rawQuery = sqlCommand.sqlStatement();
        final String[] bindings = sqlCommand.getBindingsAsArray();
        return this.databaseConnectionHandle.rawQuery(rawQuery, bindings);
    }
}
