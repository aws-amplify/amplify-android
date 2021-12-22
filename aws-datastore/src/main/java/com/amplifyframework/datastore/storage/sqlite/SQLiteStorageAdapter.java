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
import androidx.annotation.NonNull;
import androidx.annotation.VisibleForTesting;
import androidx.core.util.ObjectsCompat;

import com.amplifyframework.AmplifyException;
import com.amplifyframework.core.Action;
import com.amplifyframework.core.Amplify;
import com.amplifyframework.core.Consumer;
import com.amplifyframework.core.async.Cancelable;
import com.amplifyframework.core.model.CustomTypeField;
import com.amplifyframework.core.model.CustomTypeSchema;
import com.amplifyframework.core.model.Model;
import com.amplifyframework.core.model.ModelAssociation;
import com.amplifyframework.core.model.ModelField;
import com.amplifyframework.core.model.ModelProvider;
import com.amplifyframework.core.model.ModelSchema;
import com.amplifyframework.core.model.SchemaRegistry;
import com.amplifyframework.core.model.SerializedCustomType;
import com.amplifyframework.core.model.SerializedModel;
import com.amplifyframework.core.model.query.ObserveQueryOptions;
import com.amplifyframework.core.model.query.QueryOptions;
import com.amplifyframework.core.model.query.Where;
import com.amplifyframework.core.model.query.predicate.QueryField;
import com.amplifyframework.core.model.query.predicate.QueryPredicate;
import com.amplifyframework.core.model.query.predicate.QueryPredicates;
import com.amplifyframework.datastore.DataStoreConfiguration;
import com.amplifyframework.datastore.DataStoreException;
import com.amplifyframework.datastore.DataStoreQuerySnapshot;
import com.amplifyframework.datastore.model.CompoundModelProvider;
import com.amplifyframework.datastore.model.SystemModelsProviderFactory;
import com.amplifyframework.datastore.storage.LocalStorageAdapter;
import com.amplifyframework.datastore.storage.StorageItemChange;
import com.amplifyframework.datastore.storage.sqlite.adapter.SQLiteTable;
import com.amplifyframework.datastore.storage.sqlite.migrations.ModelMigrations;
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

    // Thread pool size is determined as number of processors multiplied by this value.  We want to allow more threads
    // than available processors to parallelize primarily IO bound work, but still provide a limit to avoid out of
    // memory errors.
    private static final int THREAD_POOL_SIZE_MULTIPLIER = 20;

    @VisibleForTesting @SuppressWarnings("checkstyle:all") // Keep logger first
    static final String DEFAULT_DATABASE_NAME = "AmplifyDatastore.db";

    private final String databaseName;

    // Provider of the Models that will be warehouse-able by the DataStore
    // and models that are used internally for DataStore to track metadata
    private final ModelProvider modelsProvider;

    // SchemaRegistry instance that gives the ModelSchema, CustomTypeSchema (Flutter) and Model objects
    // based on Model class name lookup mechanism.
    private final SchemaRegistry schemaRegistry;

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

    // Responsible for executing all commands on the SQLiteDatabase.
    private SQLCommandProcessor sqlCommandProcessor;

    // Factory that produces SQL commands.
    private SQLCommandFactory sqlCommandFactory;

    // The helper object to iterate through associated models of a given model.
    private SQLiteModelTree sqliteModelTree;

    // Stores the reference to disposable objects for cleanup
    private final CompositeDisposable toBeDisposed;

    // Need to keep a reference to the app context so we can
    // re-initialize the adapter after deleting the file in the clear() method
    private Context context;

    private SqlQueryProcessor sqlQueryProcessor;

    private DataStoreConfiguration dataStoreConfiguration;
    private SyncStatus syncStatus;

    /**
     * Construct the SQLiteStorageAdapter object.
     * @param schemaRegistry A registry of schema for all models and custom types used by the system
     * @param userModelsProvider Provides the models that will be usable by the DataStore
     * @param systemModelsProvider Provides the models that are used by the DataStore system internally
     */
    private SQLiteStorageAdapter(
            SchemaRegistry schemaRegistry,
            ModelProvider userModelsProvider,
            ModelProvider systemModelsProvider) {
        this(schemaRegistry, userModelsProvider, systemModelsProvider, DEFAULT_DATABASE_NAME);
    }

    private SQLiteStorageAdapter(
        SchemaRegistry schemaRegistry,
        ModelProvider userModelsProvider,
        ModelProvider systemModelsProvider,
        String databaseName) {
        this.schemaRegistry = schemaRegistry;
        this.modelsProvider = CompoundModelProvider.of(systemModelsProvider, userModelsProvider);
        this.gson = GsonFactory.instance();
        this.itemChangeSubject = PublishSubject.<StorageItemChange<? extends Model>>create().toSerialized();
        this.toBeDisposed = new CompositeDisposable();
        this.databaseName = databaseName;
    }

    /**
     * Gets a SQLiteStorageAdapter that can be initialized to use the provided models.
     * @param schemaRegistry Registry of schema for all models and custom types in the system
     * @param userModelsProvider A provider of models that will be represented in SQL
     * @return A SQLiteStorageAdapter that will host the provided models in SQL tables
     */
    @NonNull
    public static SQLiteStorageAdapter forModels(
            @NonNull SchemaRegistry schemaRegistry,
            @NonNull ModelProvider userModelsProvider) {
        return new SQLiteStorageAdapter(
            schemaRegistry,
            Objects.requireNonNull(userModelsProvider),
            SystemModelsProviderFactory.create()
        );
    }

    /**
     * Gets a SQLiteStorageAdapter that can be initialized to use the provided models.
     * @param schemaRegistry Registry of schema for all models and custom types in the system
     * @param userModelsProvider A provider of models that will be represented in SQL
     * @param databaseName Name of the SQLite database.
     * @return A SQLiteStorageAdapter that will host the provided models in SQL tables
     */
    @NonNull
    static SQLiteStorageAdapter forModels(
        @NonNull SchemaRegistry schemaRegistry,
        @NonNull ModelProvider userModelsProvider,
        @NonNull String databaseName) {
        return new SQLiteStorageAdapter(
            schemaRegistry,
            Objects.requireNonNull(userModelsProvider),
            SystemModelsProviderFactory.create(),
            databaseName
        );
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized void initialize(
            @NonNull Context context,
            @NonNull Consumer<List<ModelSchema>> onSuccess,
            @NonNull Consumer<DataStoreException> onError,
            @NonNull DataStoreConfiguration dataStoreConfiguration) {
        Objects.requireNonNull(context);
        Objects.requireNonNull(onSuccess);
        Objects.requireNonNull(onError);
        // Create a thread pool large enough to take advantage of parallelization, but small enough to avoid
        // OutOfMemoryError and CursorWindowAllocationException issues.
        this.threadPool = Executors.newFixedThreadPool(
                Runtime.getRuntime().availableProcessors() * THREAD_POOL_SIZE_MULTIPLIER);
        this.context = context;
        this.dataStoreConfiguration = dataStoreConfiguration;
        threadPool.submit(() -> {
            try {
                /*
                 * Start with a fresh registry.
                 */
                schemaRegistry.clear();
                /*
                 * Create {@link ModelSchema} objects for the corresponding {@link Model}.
                 * Any exception raised during this when inspecting the Model classes
                 * through reflection will be notified via the `onError` callback.
                 */
                schemaRegistry.register(modelsProvider.modelSchemas(), modelsProvider.customTypeSchemas());

                /*
                 * Create the CREATE TABLE and CREATE INDEX commands for each of the
                 * Models. Instantiate {@link SQLiteStorageHelper} to execute those
                 * create commands.
                 */
                this.sqlCommandFactory = new SQLiteCommandFactory(schemaRegistry, gson);
                CreateSqlCommands createSqlCommands = getCreateCommands(modelsProvider.modelNames());
                sqliteStorageHelper = SQLiteStorageHelper.getInstance(
                        context,
                        databaseName,
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

                /*
                 * Create helper instance that can traverse through model relations.
                 */
                this.sqliteModelTree = new SQLiteModelTree(
                    schemaRegistry,
                    databaseConnectionHandle
                );

                /*
                 * Create a command processor which runs the actual SQL transactions.
                 */
                this.sqlCommandProcessor = new SQLCommandProcessor(databaseConnectionHandle);

                sqlQueryProcessor = new SqlQueryProcessor(sqlCommandProcessor,
                        sqlCommandFactory,
                        schemaRegistry);
                syncStatus = new SyncStatus(sqlQueryProcessor, dataStoreConfiguration);

                /*
                 * Detect if the version of the models stored in SQLite is different
                 * from the version passed in through {@link ModelProvider#version()}.
                 * Delete the database if there is a version change.
                 */
                toBeDisposed.add(updateModels().subscribe(
                    () -> onSuccess.accept(
                        Immutable.of(new ArrayList<>(schemaRegistry.getModelSchemaMap().values()))
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
                final ModelSchema modelSchema = schemaRegistry.getModelSchemaForModelClass(item.getModelName());

                final StorageItemChange.Type writeType;
                SerializedModel patchItem = null;

                if (sqlQueryProcessor.modelExists(item, QueryPredicates.all())) {
                    // if data exists already, then UPDATE the row
                    writeType = StorageItemChange.Type.UPDATE;

                    // Check if existing data meets the condition, only if a condition other than all() was provided.
                    if (!QueryPredicates.all().equals(predicate) && !sqlQueryProcessor.modelExists(item, predicate)) {
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
                String modelToString = item.getModelName() + "[id=" + item.getId() + "]";
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
            List<T> models = sqlQueryProcessor.queryOfflineData(itemClass, options, onError);
            onSuccess.accept(models.iterator());
        });
    }

    /**
     * {@inheritDoc}
     */
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
            final ModelSchema modelSchema = schemaRegistry.getModelSchemaForModelClass(modelName);
            try (Cursor cursor = sqlCommandProcessor.rawQuery(sqlCommandFactory.queryFor(modelSchema, options))) {
                LOG.debug("Querying item for: " + modelName);

                final List<Model> models = new ArrayList<>();
                final SQLiteModelFieldTypeConverter converter =
                    new SQLiteModelFieldTypeConverter(modelSchema, schemaRegistry, gson);

                if (cursor == null) {
                    onError.accept(new DataStoreException(
                            "Error in getting a cursor to the table for class: " + modelName,
                            AmplifyException.TODO_RECOVERY_SUGGESTION
                    ));
                    return;
                }

                if (cursor.moveToFirst()) {
                    do {
                        final Map<String, Object> data = converter.buildMapForModel(cursor);
                        final SerializedModel model = createSerializedModel(modelSchema, data);
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
                final String modelName = item.getModelName();
                final ModelSchema modelSchema = schemaRegistry.getModelSchemaForModelClass(modelName);

                // Check if data being deleted exists; "Succeed" deletion in that case.
                if (!sqlQueryProcessor.modelExists(item, QueryPredicates.all())) {
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

                // Check if existing data meets the condition, only if a condition other than all() was provided.
                if (!QueryPredicates.all().equals(predicate) && !sqlQueryProcessor.modelExists(item, predicate)) {
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
                    ModelSchema schema = schemaRegistry.getModelSchemaForModelClass(cascadedModel.getModelName());
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
            final ModelSchema modelSchema = schemaRegistry.getModelSchemaForModelClass(itemClass);
            QueryOptions options = Where.matches(predicate);
            try (Cursor cursor = sqlCommandProcessor.rawQuery(sqlCommandFactory.queryFor(modelSchema, options))) {
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
                sqlCommandProcessor.execute(sqlCommandFactory.deleteFor(modelSchema, predicate));

                // publish every deletion
                for (Model model : modelsToDelete) {
                    ModelSchema schema = schemaRegistry.getModelSchemaForModelClass(model.getModelName());
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
    public <T extends Model> void observeQuery(
            @NonNull Class<T> itemClass,
            @NonNull ObserveQueryOptions options,
            @NonNull Consumer<Cancelable> onObservationStarted,
            @NonNull Consumer<DataStoreQuerySnapshot<T>> onQuerySnapshot,
            @NonNull Consumer<DataStoreException> onObservationError,
            @NonNull Action onObservationComplete) {
        Objects.requireNonNull(onObservationStarted);
        Objects.requireNonNull(onObservationError);
        Objects.requireNonNull(onObservationComplete);
        new ObserveQueryExecutor<>(itemChangeSubject, sqlQueryProcessor,
                threadPool,
                syncStatus,
                new ModelSorter<T>(),
                dataStoreConfiguration)
                .observeQuery(itemClass,
                        options,
                        onObservationStarted,
                        onQuerySnapshot,
                        onObservationError,
                        onObservationComplete);
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
        if (!context.deleteDatabase(databaseName)) {
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
            )),
                dataStoreConfiguration
        );
    }

    private CreateSqlCommands getCreateCommands(@NonNull Set<String> modelNames) {
        final Set<SqlCommand> createTableCommands = new HashSet<>();
        final Set<SqlCommand> createIndexCommands = new HashSet<>();
        for (String modelName : modelNames) {
            final ModelSchema modelSchema =
                schemaRegistry.getModelSchemaForModelClass(modelName);
            createTableCommands.add(sqlCommandFactory.createTableFor(modelSchema));
            createIndexCommands.addAll(sqlCommandFactory.createIndexesFor(modelSchema));
        }
        return new CreateSqlCommands(createTableCommands, createIndexCommands);
    }

    private <T extends Model> void writeData(
            T item,
            StorageItemChange.Type writeType
    ) throws DataStoreException {
        final String modelName = item.getModelName();
        final ModelSchema modelSchema = schemaRegistry.getModelSchemaForModelClass(modelName);
        final SQLiteTable sqliteTable = SQLiteTable.fromSchema(modelSchema);

        // Generate SQL command for given action
        switch (writeType) {
            case CREATE:
                LOG.verbose("Creating item in " + sqliteTable.getName() + " identified by ID: " + item.getId());
                sqlCommandProcessor.execute(sqlCommandFactory.insertFor(modelSchema, item));
                break;
            case UPDATE:
                LOG.verbose("Updating item in " + sqliteTable.getName() + " identified by ID: " + item.getId());
                sqlCommandProcessor.execute(sqlCommandFactory.updateFor(modelSchema, item));
                break;
            case DELETE:
                LOG.verbose("Deleting item in " + sqliteTable.getName() + " identified by ID: " + item.getId());
                final String primaryKeyName = sqliteTable.getPrimaryKey().getName();
                final QueryPredicate matchId = QueryField.field(modelName, primaryKeyName).eq(item.getId());
                sqlCommandProcessor.execute(sqlCommandFactory.deleteFor(modelSchema, matchId));
                break;
            default:
                throw new DataStoreException(
                    "Unexpected change was requested: " + writeType.name(),
                    "Valid storage changes are CREATE, UPDATE, and DELETE."
                );
        }
    }

    private boolean modelExists(Model model, QueryPredicate predicate) throws DataStoreException {
        final String modelName = model.getModelName();
        final ModelSchema schema = schemaRegistry.getModelSchemaForModelClass(modelName);
        final SQLiteTable table = SQLiteTable.fromSchema(schema);
        final String tableName = table.getName();
        final String primaryKeyName = table.getPrimaryKey().getName();
        final QueryPredicate matchId = QueryField.field(tableName, primaryKeyName).eq(model.getId());
        final QueryPredicate condition = predicate.and(matchId);
        return sqlCommandProcessor.executeExists(sqlCommandFactory.existsFor(schema, condition));
    }

    /**
     * Helper method to synchronously query for a single model instance.  Used before any save initiated by
     * DATASTORE_API in order to determine which fields have changed.
     * @param model a Model that we want to query for the same type and id in SQLite.
     * @return the Model instance from SQLite, if it exists, otherwise null.
     */
    private Model query(Model model) {
        final String modelName = model.getModelName();
        final ModelSchema schema = schemaRegistry.getModelSchemaForModelClass(modelName);
        final SQLiteTable table = SQLiteTable.fromSchema(schema);
        final String primaryKeyName = table.getPrimaryKey().getName();
        final QueryPredicate matchId = QueryField.field(modelName, primaryKeyName).eq(model.getId());

        Iterator<? extends Model> result = Single.<Iterator<? extends Model>>create(emitter -> {
            if (model instanceof SerializedModel) {
                query(model.getModelName(), Where.matches(matchId), emitter::onSuccess, emitter::onError);
            } else {
                query(model.getClass(), Where.matches(matchId), emitter::onSuccess, emitter::onError);
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
                } else {
                    LOG.debug("Database up to date. Checking ModelMetadata.");
                    new ModelMigrations(databaseConnectionHandle, modelsProvider).apply();
                }
            }
            PersistentModelVersion persistentModelVersion = new PersistentModelVersion(modelsProvider.version());
            return PersistentModelVersion.saveToLocalStorage(this, persistentModelVersion);
        }).ignoreElement();
    }

    /**
     * recursively creates nested SerializedModels from raw data.
     */
    private SerializedModel createSerializedModel(ModelSchema modelSchema, Map<String, Object> data) {
        final Map<String, Object> serializedData = new HashMap<>();
        for (Map.Entry<String, Object> entry : data.entrySet()) {
            ModelField field = modelSchema.getFields().get(entry.getKey());
            if (field != null && entry.getValue() != null) {
                if (field.isModel()) {
                    ModelAssociation association = modelSchema.getAssociations().get(entry.getKey());
                    if (association != null) {
                        String associatedType = association.getAssociatedType();
                        final ModelSchema nestedModelSchema = schemaRegistry.getModelSchemaForModelClass(
                                associatedType
                        );
                        @SuppressWarnings("unchecked")
                        SerializedModel model = createSerializedModel(
                                nestedModelSchema, (Map<String, Object>) entry.getValue()
                        );
                        serializedData.put(entry.getKey(), model);
                    }
                } else if (field.isCustomType()) {
                    if (field.isArray()) {
                        @SuppressWarnings("unchecked")
                        List<Map<String, Object>> listItems = (List<Map<String, Object>>) entry.getValue();
                        List<SerializedCustomType> listOfCustomType =
                                getValueOfListCustomTypeField(field.getTargetType(), listItems);
                        serializedData.put(entry.getKey(), listOfCustomType);
                    } else {
                        final CustomTypeSchema nestedCustomTypeSchema =
                                schemaRegistry.getCustomTypeSchemaForCustomTypeClass(field.getTargetType());
                        @SuppressWarnings("unchecked")
                        SerializedCustomType customType = createSerializedCustomType(
                                nestedCustomTypeSchema, (Map<String, Object>) entry.getValue()
                        );
                        serializedData.put(entry.getKey(), customType);
                    }
                } else {
                    serializedData.put(entry.getKey(), entry.getValue());
                }
            }
        }
        return SerializedModel.builder()
                .serializedData(serializedData)
                .modelSchema(modelSchema)
                .build();
    }

    private SerializedCustomType createSerializedCustomType(
            CustomTypeSchema customTypeSchema, Map<String, Object> data) {
        final Map<String, Object> serializedData = new HashMap<>();
        for (Map.Entry<String, Object> entry : data.entrySet()) {
            CustomTypeField field = customTypeSchema.getFields().get(entry.getKey());

            if (field == null) {
                continue;
            }

            if (field.isCustomType() && entry.getValue() != null) {
                if (field.isArray()) {
                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> listItems = (List<Map<String, Object>>) entry.getValue();
                    List<SerializedCustomType> listOfCustomType =
                            getValueOfListCustomTypeField(field.getTargetType(), listItems);
                    serializedData.put(entry.getKey(), listOfCustomType);
                } else {
                    final CustomTypeSchema nestedCustomTypeSchema =
                            schemaRegistry.getCustomTypeSchemaForCustomTypeClass(field.getTargetType());
                    @SuppressWarnings("unchecked")
                    Map<String, Object> nestedData = (Map<String, Object>) entry.getValue();
                    serializedData.put(entry.getKey(),
                            createSerializedCustomType(nestedCustomTypeSchema, nestedData)
                    );
                }
            } else {
                serializedData.put(entry.getKey(), entry.getValue());
            }
        }

        return SerializedCustomType.builder()
                .serializedData(serializedData)
                .customTypeSchema(customTypeSchema)
                .build();
    }

    private List<SerializedCustomType> getValueOfListCustomTypeField(
            String fieldTargetType, List<Map<String, Object>> listItems) {
        // if the filed is optional and has null value instead of an array
        if (listItems == null) {
            return null;
        }

        final CustomTypeSchema nestedCustomTypeSchema =
                schemaRegistry.getCustomTypeSchemaForCustomTypeClass(fieldTargetType);
        List<SerializedCustomType> listOfCustomType = new ArrayList<>();

        for (Map<String, Object> listItem : listItems) {
            SerializedCustomType customType = createSerializedCustomType(
                    nestedCustomTypeSchema, listItem
            );
            listOfCustomType.add(customType);
        }

        return listOfCustomType;
    }
}
