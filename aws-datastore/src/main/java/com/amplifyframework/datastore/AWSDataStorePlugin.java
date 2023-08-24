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

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.annotation.WorkerThread;

import com.amplifyframework.AmplifyException;
import com.amplifyframework.api.ApiCategory;
import com.amplifyframework.api.aws.AuthModeStrategyType;
import com.amplifyframework.api.graphql.GraphQLBehavior;
import com.amplifyframework.core.Action;
import com.amplifyframework.core.Amplify;
import com.amplifyframework.core.Consumer;
import com.amplifyframework.core.InitializationStatus;
import com.amplifyframework.core.async.Cancelable;
import com.amplifyframework.core.category.CategoryType;
import com.amplifyframework.core.model.Model;
import com.amplifyframework.core.model.ModelIdentifier;
import com.amplifyframework.core.model.ModelProvider;
import com.amplifyframework.core.model.SchemaRegistry;
import com.amplifyframework.core.model.SerializedModel;
import com.amplifyframework.core.model.query.ObserveQueryOptions;
import com.amplifyframework.core.model.query.QueryOptions;
import com.amplifyframework.core.model.query.Where;
import com.amplifyframework.core.model.query.predicate.QueryPredicate;
import com.amplifyframework.core.model.query.predicate.QueryPredicates;
import com.amplifyframework.datastore.appsync.AppSyncClient;
import com.amplifyframework.datastore.events.NetworkStatusEvent;
import com.amplifyframework.datastore.model.ModelProviderLocator;
import com.amplifyframework.datastore.storage.ItemChangeMapper;
import com.amplifyframework.datastore.storage.LocalStorageAdapter;
import com.amplifyframework.datastore.storage.StorageItemChange;
import com.amplifyframework.datastore.storage.sqlite.SQLiteStorageAdapter;
import com.amplifyframework.datastore.syncengine.Orchestrator;
import com.amplifyframework.datastore.syncengine.ReachabilityMonitor;
import com.amplifyframework.hub.HubChannel;
import com.amplifyframework.hub.HubEvent;
import com.amplifyframework.logging.Logger;

import org.json.JSONObject;

import java.io.Serializable;
import java.util.Iterator;
import java.util.Objects;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.schedulers.Schedulers;

/**
 * An AWS implementation of the {@link DataStorePlugin}.
 */
public final class AWSDataStorePlugin extends DataStorePlugin<Void> {
    private static final Logger LOG = Amplify.Logging.logger(CategoryType.DATASTORE, "amplify:aws-datastore");
    private static final long LIFECYCLE_TIMEOUT_MS = TimeUnit.SECONDS.toMillis(5);

    // Reference to an implementation of the Local Storage Adapter that
    // manages the persistence of data on-device.
    private final LocalStorageAdapter sqliteStorageAdapter;

    // A component which synchronizes data state between the
    // local storage adapter, and a remote API
    private final Orchestrator orchestrator;

    // Keeps track of whether of not the category is initialized yet
    private final CountDownLatch categoryInitializationsPending;

    // User-provided configuration for the plugin.
    private final DataStoreConfiguration userProvidedConfiguration;

    // Configuration for the plugin that contains settings from the JSON file plus any
    // overrides provided via the userProvidedConfiguration
    private DataStoreConfiguration pluginConfiguration;

    private final AuthModeStrategyType authModeStrategy;

    private final boolean isSyncRetryEnabled;

    private final ReachabilityMonitor reachabilityMonitor;

    private AWSDataStorePlugin(
            @NonNull ModelProvider modelProvider,
            @NonNull SchemaRegistry schemaRegistry,
            @NonNull ApiCategory api,
            @Nullable DataStoreConfiguration userProvidedConfiguration) {
        this.sqliteStorageAdapter = SQLiteStorageAdapter.forModels(schemaRegistry, modelProvider);
        this.categoryInitializationsPending = new CountDownLatch(1);
        this.authModeStrategy = AuthModeStrategyType.DEFAULT;
        this.userProvidedConfiguration = userProvidedConfiguration;
        this.isSyncRetryEnabled = userProvidedConfiguration != null && userProvidedConfiguration.getDoSyncRetry();
        this.reachabilityMonitor = ReachabilityMonitor.Companion.create();
        // Used to interrogate plugins, to understand if sync should be automatically turned on
        this.orchestrator = new Orchestrator(
            modelProvider,
            schemaRegistry,
            sqliteStorageAdapter,
            AppSyncClient.via(api),
            () -> pluginConfiguration,
            () -> api.getPlugins().isEmpty() ? Orchestrator.State.LOCAL_ONLY : Orchestrator.State.SYNC_VIA_API,
            reachabilityMonitor,
            isSyncRetryEnabled
        );

    }

    private AWSDataStorePlugin(@NonNull Builder builder) throws DataStoreException {
        SchemaRegistry schemaRegistry = builder.schemaRegistry == null ?
            SchemaRegistry.instance() :
            builder.schemaRegistry;
        ModelProvider modelProvider = builder.modelProvider == null ?
            ModelProviderLocator.locate() :
            builder.modelProvider;
        this.authModeStrategy = builder.authModeStrategy == null ?
            AuthModeStrategyType.DEFAULT :
            builder.authModeStrategy;
        this.isSyncRetryEnabled = builder.isSyncRetryEnabled;
        ApiCategory api = builder.apiCategory == null ? Amplify.API : builder.apiCategory;
        this.userProvidedConfiguration = builder.dataStoreConfiguration;
        this.sqliteStorageAdapter = builder.storageAdapter == null ?
            SQLiteStorageAdapter.forModels(schemaRegistry, modelProvider) :
            builder.storageAdapter;
        this.categoryInitializationsPending = new CountDownLatch(1);
        this.reachabilityMonitor = builder.reachabilityMonitor == null ?
            ReachabilityMonitor.Companion.create() :
            builder.reachabilityMonitor;

        // Used to interrogate plugins, to understand if sync should be automatically turned on
        this.orchestrator = new Orchestrator(
            modelProvider,
            schemaRegistry,
            sqliteStorageAdapter,
            AppSyncClient.via(api, this.authModeStrategy),
            () -> pluginConfiguration,
            () -> api.getPlugins().isEmpty() ? Orchestrator.State.LOCAL_ONLY : Orchestrator.State.SYNC_VIA_API,
            reachabilityMonitor,
            isSyncRetryEnabled
        );
    }

    /**
     * Constructs an {@link AWSDataStorePlugin} which can warehouse the model types provided by
     * your application's code-generated model provider. This model provider is expected to have the
     * fully qualified class name of com.amplifyframework.datastore.generated.model.AmplifyModelProvider.
     *
     * This constructor will attempt to find that provide by means of reflection. If you have changed
     * the path to your {@link ModelProvider} instance, and do not use the default path generated by
     * the Amplify Code Generator, then this will break. Likewise, if you need to provide a custom
     * {@link ModelProvider}, this will not work. In both cases, you should prefer one of the other
     * overloads such as {@link #AWSDataStorePlugin(ModelProvider)}.
     *
     * If remote synchronization is enabled, it will be performed via {@link Amplify#API}.
     *
     * @throws DataStoreException If it is not possible to access the code-generated model provider
     */
    public AWSDataStorePlugin() throws DataStoreException {
        this(AWSDataStorePlugin.builder());
    }

    /**
     * Constructs an {@link AWSDataStorePlugin} using a user-provided configuration.
     * The plugin will be able to warehouse models as described in
     * {@link AWSDataStorePlugin#AWSDataStorePlugin()}.
     * @param userProvidedConfiguration
     *        Additionally, consider these user-provided configuration options.
     *        These values override anything found in `amplifyconfiguration.json`.
     *        This configuration may also include hooks for conflict resolution
     *        and or global error handling.
     * @throws DataStoreException
     *         If not possible to locate the code-generated model provider,
     *         com.amplifyframework.datastore.generated.model.AmplifyModelProvider.
     * @deprecated Use {@link Builder} instead.
     */
    @Deprecated
    public AWSDataStorePlugin(@NonNull DataStoreConfiguration userProvidedConfiguration) throws DataStoreException {
        this(
            ModelProviderLocator.locate(),
            SchemaRegistry.instance(),
            Amplify.API,
            Objects.requireNonNull(userProvidedConfiguration)
        );
    }

    /**
     * Constructs an {@link AWSDataStorePlugin} which can warehouse the model types provided by
     * the supplied {@link ModelProvider}. If the API plugin is present and configured,
     * then remote synchronization will be performed through {@link Amplify#API}.
     * @param modelProvider Provider of models to be usable by plugin
     * @deprecated Use {@link Builder} instead.
     */
    @Deprecated
    public AWSDataStorePlugin(@NonNull ModelProvider modelProvider) {
        this(Objects.requireNonNull(modelProvider), Amplify.API, null);
    }

    /**
     * Constructs an {@link AWSDataStorePlugin} which can warehouse the model types provided by the
     * supplied {@link ModelProvider}. If remote synchronization is enabled, it will be performed
     * through the provided {@link GraphQLBehavior}.
     * @param modelProvider Provides the set of models to be warehouse-able by this system
     * @param api Interface to a remote system where models will be synchronized
     * @deprecated Use {@link Builder} instead.
     */
    @Deprecated
    @VisibleForTesting
    AWSDataStorePlugin(@NonNull ModelProvider modelProvider,
                       @NonNull ApiCategory api,
                       @Nullable DataStoreConfiguration dataStoreConfiguration) {
        this(
            Objects.requireNonNull(modelProvider),
            SchemaRegistry.instance(),
            Objects.requireNonNull(api),
            dataStoreConfiguration
        );
    }

    /**
     * {@inheritDoc}
     */
    @NonNull
    @Override
    public String getPluginKey() {
        return DataStoreConfiguration.PLUGIN_CONFIG_KEY;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void configure(
            @NonNull JSONObject pluginConfiguration,
            @NonNull Context context
    ) throws DataStoreException {
        try {
            // Applies user-provided configs on-top-of any values from the file.
            this.pluginConfiguration = DataStoreConfiguration
                .builder(pluginConfiguration, userProvidedConfiguration)
                .build();
        } catch (DataStoreException badConfigException) {
            throw new DataStoreException(
                "There was an issue configuring the plugin from the amplifyconfiguration.json",
                badConfigException,
                "Check the attached exception for more details and " +
                    "be sure you are only calling Amplify.configure once"
            );
        }

        HubChannel hubChannel = HubChannel.forCategoryType(getCategoryType());
        Amplify.Hub.subscribe(hubChannel,
            event -> InitializationStatus.SUCCEEDED.toString().equals(event.getName()),
            event -> categoryInitializationsPending.countDown()
        );

        reachabilityMonitor.configure(context);

        waitForInitialization().subscribe(this::observeNetworkStatus);
    }

    private void publishNetworkStatusEvent(boolean active) {
        Amplify.Hub.publish(HubChannel.DATASTORE,
                HubEvent.create(DataStoreChannelEventName.NETWORK_STATUS, new NetworkStatusEvent(active)));
    }

    private void observeNetworkStatus() {
        reachabilityMonitor.getObservable()
                .subscribe(this::publishNetworkStatusEvent);
    }

    @WorkerThread
    @Override
    public void initialize(@NonNull Context context) throws AmplifyException {
        try {
            initializeStorageAdapter(context)
                .blockingAwait(LIFECYCLE_TIMEOUT_MS, TimeUnit.MILLISECONDS);
        } catch (Throwable initError) {
            throw new AmplifyException(
                "Failed to initialize the local storage adapter for the DataStore plugin.",
                initError, AmplifyException.TODO_RECOVERY_SUGGESTION
            );
        }
    }

    /**
     * Initializes the storage adapter, and gets the result as a {@link Completable}.
     * @param context An Android Context
     * @return A Completable which will initialize the storage adapter when subscribed.
     */
    @WorkerThread
    private Completable initializeStorageAdapter(Context context) {
        return Completable.defer(() -> Completable.create(emitter ->
            sqliteStorageAdapter.initialize(context, schemaList -> emitter.onComplete(),
                    emitter::onError, pluginConfiguration)
        ));
    }

    private Completable waitForInitialization() {
        return Completable.fromAction(() -> categoryInitializationsPending.await())
            .timeout(LIFECYCLE_TIMEOUT_MS, TimeUnit.MILLISECONDS)
            .subscribeOn(Schedulers.io())
            .doOnComplete(() -> LOG.info("DataStore plugin initialized."))
            .doOnError(error -> LOG.error("DataStore initialization timed out.", error));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void start(@NonNull Action onComplete, @NonNull Consumer<DataStoreException> onError) {
        waitForInitialization()
            .andThen(orchestrator.start())
            .subscribeOn(Schedulers.io())
            .subscribe(
                onComplete::call,
                error -> onError.accept(new DataStoreException("Failed to start DataStore.", error, "Retry."))
            );
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void stop(@NonNull Action onComplete, @NonNull Consumer<DataStoreException> onError) {
        waitForInitialization()
            .andThen(orchestrator.stop())
            .subscribeOn(Schedulers.io())
            .subscribe(
                onComplete::call,
                error -> onError.accept(new DataStoreException("Failed to stop DataStore.", error, "Retry."))
            );
    }

    /**
     * Stops all synchronization processes and invokes the clear method of the underlying storage adapter. Any items
     * pending synchronization in the outbound queue will be lost. Synchronization processes will be restarted on the
     * next interaction with the DataStore.
     *
     * @param onComplete Invoked if the call is successful.
     * @param onError Invoked if not successful
     */
    @Override
    public void clear(@NonNull Action onComplete, @NonNull Consumer<DataStoreException> onError) {
        stop(() -> Completable.create(emitter -> sqliteStorageAdapter.clear(emitter::onComplete, emitter::onError))
                        .subscribeOn(Schedulers.io())
                        .subscribe(onComplete::call,
                            throwable -> onError.accept(new DataStoreException("Clear operation failed",
                                    throwable, AmplifyException.REPORT_BUG_TO_AWS_SUGGESTION))),
                onError);
    }

    /**
     * {@inheritDoc}
     */
    @Nullable
    @Override
    public Void getEscapeHatch() {
        return null;
    }

    @NonNull
    @Override
    public String getVersion() {
        return BuildConfig.VERSION_NAME;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T extends Model> void save(
            @NonNull T item,
            @NonNull Consumer<DataStoreItemChange<T>> onItemSaved,
            @NonNull Consumer<DataStoreException> onFailureToSave) {
        save(item, QueryPredicates.all(), onItemSaved, onFailureToSave);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T extends Model> void save(
            @NonNull T item,
            @NonNull QueryPredicate predicate,
            @NonNull Consumer<DataStoreItemChange<T>> onItemSaved,
            @NonNull Consumer<DataStoreException> onFailureToSave) {
        start(() -> sqliteStorageAdapter.save(
            item,
            StorageItemChange.Initiator.DATA_STORE_API,
            predicate,
            itemSave -> {
                try {
                    onItemSaved.accept(ItemChangeMapper.map(itemSave));
                } catch (DataStoreException dataStoreException) {
                    onFailureToSave.accept(dataStoreException);
                }
            },
            onFailureToSave
        ), onFailureToSave);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T extends Model> void delete(
            @NonNull T item,
            @NonNull Consumer<DataStoreItemChange<T>> onItemDeleted,
            @NonNull Consumer<DataStoreException> onFailureToDelete) {
        delete(item, QueryPredicates.all(), onItemDeleted, onFailureToDelete);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T extends Model> void delete(
            @NonNull T item,
            @NonNull QueryPredicate predicate,
            @NonNull Consumer<DataStoreItemChange<T>> onItemDeleted,
            @NonNull Consumer<DataStoreException> onFailureToDelete) {
        start(() -> sqliteStorageAdapter.delete(
            item,
            StorageItemChange.Initiator.DATA_STORE_API,
            predicate,
            itemDeletion -> {
                try {
                    onItemDeleted.accept(ItemChangeMapper.map(itemDeletion));
                } catch (DataStoreException dataStoreException) {
                    onFailureToDelete.accept(dataStoreException);
                }
            },
            onFailureToDelete
        ), onFailureToDelete);
    }

    @Override
    public <T extends Model> void delete(
            @NonNull Class<T> itemClass,
            @NonNull QueryPredicate predicate,
            @NonNull Action onItemsDeleted,
            @NonNull Consumer<DataStoreException> onFailureToDelete) {
        start(() -> sqliteStorageAdapter.delete(
                itemClass,
                StorageItemChange.Initiator.DATA_STORE_API,
                predicate,
                onItemsDeleted,
                onFailureToDelete
        ), onFailureToDelete);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T extends Model> void query(
            @NonNull Class<T> itemClass,
            @NonNull Consumer<Iterator<T>> onQueryResults,
            @NonNull Consumer<DataStoreException> onQueryFailure) {
        start(() ->
            sqliteStorageAdapter.query(itemClass, Where.matchesAll(), onQueryResults, onQueryFailure), onQueryFailure);
    }

    /**
     * Query the DataStore to find all items of the requested model (by name).
     * NOTE: This private method is used only by hybrid platforms (Flutter, React Native),
     * and should not be included into the {@link DataStoreCategory} spec.
     * @param modelName name of the Model to query
     * @param options Filtering, paging, and sorting options
     * @param onQueryResults Called when a query successfully returns 0 or more results
     * @param onQueryFailure Called when a failure interrupts successful completion of a query
     */
    public void query(
            @NonNull String modelName,
            @NonNull QueryOptions options,
            @NonNull Consumer<Iterator<? extends Model>> onQueryResults,
            @NonNull Consumer<DataStoreException> onQueryFailure) {
        start(() -> sqliteStorageAdapter.query(modelName, options, onQueryResults, onQueryFailure), onQueryFailure);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T extends Model> void query(
            @NonNull Class<T> itemClass,
            @NonNull QueryPredicate queryPredicate,
            @NonNull Consumer<Iterator<T>> onQueryResults,
            @NonNull Consumer<DataStoreException> onQueryFailure) {
        this.query(itemClass, Where.matches(queryPredicate), onQueryResults, onQueryFailure);
    }

    @Override
    public <T extends Model> void query(
            @NonNull Class<T> itemClass,
            @NonNull QueryOptions options,
            @NonNull Consumer<Iterator<T>> onQueryResults,
            @NonNull Consumer<DataStoreException> onQueryFailure) {
        start(() -> sqliteStorageAdapter.query(itemClass, options, onQueryResults, onQueryFailure), onQueryFailure);
    }

    @Override
    public void observe(
            @NonNull Consumer<Cancelable> onObservationStarted,
            @NonNull Consumer<DataStoreItemChange<? extends Model>> onDataStoreItemChange,
            @NonNull Consumer<DataStoreException> onObservationFailure,
            @NonNull Action onObservationCompleted) {
        start(() -> onObservationStarted.accept(sqliteStorageAdapter.observe(
            itemChange -> {
                try {
                    onDataStoreItemChange.accept(ItemChangeMapper.map(itemChange));
                } catch (DataStoreException dataStoreException) {
                    onObservationFailure.accept(dataStoreException);
                }
            },
            onObservationFailure,
            onObservationCompleted
        )), onObservationFailure);
    }

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
        start(() -> sqliteStorageAdapter.observeQuery(itemClass,
                                            options,
                                            onObservationStarted,
                                            onQuerySnapshot,
                                            onObservationError,
                                            onObservationComplete), onObservationError);
    }

    @Override
    public <T extends Model> void observe(
            @NonNull Class<T> itemClass,
            @NonNull Consumer<Cancelable> onObservationStarted,
            @NonNull Consumer<DataStoreItemChange<T>> onDataStoreItemChange,
            @NonNull Consumer<DataStoreException> onObservationFailure,
            @NonNull Action onObservationCompleted) {
        start(() -> onObservationStarted.accept(sqliteStorageAdapter.observe(
            itemChange -> {
                try {
                    if (itemChange.modelSchema().getName().equals(itemClass.getSimpleName())) {
                        @SuppressWarnings("unchecked") // This was just checked, right above.
                        StorageItemChange<T> typedChange = (StorageItemChange<T>) itemChange;
                        onDataStoreItemChange.accept(ItemChangeMapper.map(typedChange));
                    }
                } catch (DataStoreException dataStoreException) {
                    onObservationFailure.accept(dataStoreException);
                }
            },
            onObservationFailure,
            onObservationCompleted
        )), onObservationFailure);
    }

    /**
     * Observe changes to a certain type of item(s) in the DataStore.
     * @param modelName The name of the model to observe
     * @param onObservationStarted Called when observation begins
     * @param onDataStoreItemChange Called 0..n times, whenever there is a change to an
     *                              item of the requested class
     * @param onObservationFailure Called if observation of the DataStore terminates
     *                             with a non-recoverable failure
     * @param onObservationCompleted Called when observation completes gracefully
     */
    public void observe(
            @NonNull String modelName,
            @NonNull Consumer<Cancelable> onObservationStarted,
            @NonNull Consumer<DataStoreItemChange<? extends Model>> onDataStoreItemChange,
            @NonNull Consumer<DataStoreException> onObservationFailure,
            @NonNull Action onObservationCompleted) {
        start(() -> onObservationStarted.accept(sqliteStorageAdapter.observe(
            itemChange -> {
                try {
                    if (itemChange.modelSchema().getModelClass().equals(SerializedModel.class)) {
                        if (((SerializedModel) itemChange.item()).getModelName().equals(modelName)) {
                            @SuppressWarnings("unchecked") // This was just checked, right above.
                            StorageItemChange<SerializedModel> typedChange =
                                    (StorageItemChange<SerializedModel>) itemChange;
                            onDataStoreItemChange.accept(ItemChangeMapper.map(itemChange));
                        }
                    }
                } catch (DataStoreException dataStoreException) {
                    onObservationFailure.accept(dataStoreException);
                }
            },
            onObservationFailure,
            onObservationCompleted
        )), onObservationFailure);
    }

    @Override
    public <T extends Model> void observe(
            @NonNull Class<T> itemClass,
            @NonNull Serializable uniqueId,
            @NonNull Consumer<Cancelable> onObservationStarted,
            @NonNull Consumer<DataStoreItemChange<T>> onDataStoreItemChange,
            @NonNull Consumer<DataStoreException> onObservationFailure,
            @NonNull Action onObservationCompleted) {
        start(() -> onObservationStarted.accept(sqliteStorageAdapter.observe(
            itemChange -> {
                try {
                    if (itemChange.modelSchema().getName().equals(itemClass.getSimpleName()) &&
                            itemChange.item().getPrimaryKeyString().equals(ModelIdentifier.Helper
                                    .getUniqueKey(uniqueId))) {
                        @SuppressWarnings("unchecked") // itemClass() was just inspected above. This is safe.
                        StorageItemChange<T> typedChange = (StorageItemChange<T>) itemChange;
                        onDataStoreItemChange.accept(ItemChangeMapper.map(typedChange));
                    }
                } catch (DataStoreException dataStoreException) {
                    onObservationFailure.accept(dataStoreException);
                }
            },
            onObservationFailure,
            onObservationCompleted
        )), onObservationFailure);
    }

    @Override
    public <T extends Model> void observe(
            @NonNull Class<T> itemClass,
            @NonNull QueryPredicate selectionCriteria,
            @NonNull Consumer<Cancelable> onObservationStarted,
            @NonNull Consumer<DataStoreItemChange<T>> onDataStoreItemChange,
            @NonNull Consumer<DataStoreException> onObservationFailure,
            @NonNull Action onObservationCompleted) {
        start(() -> onObservationStarted.accept(sqliteStorageAdapter.observe(
            itemChange -> {
                try {
                    if (itemChange.modelSchema().getName().equals(itemClass.getSimpleName()) &&
                            selectionCriteria.evaluate(itemChange.item())) {
                        @SuppressWarnings("unchecked") // itemClass() was just inspected above. This is safe.
                        StorageItemChange<T> typedChange = (StorageItemChange<T>) itemChange;
                        onDataStoreItemChange.accept(ItemChangeMapper.map(typedChange));
                    }
                } catch (DataStoreException dataStoreException) {
                    onObservationFailure.accept(dataStoreException);
                }
            },
            onObservationFailure,
            onObservationCompleted
        )), onObservationFailure);
    }

    /**
     * Creates a builder that provides available options to be set when creating
     * a DataStore plugin.
     * @return A new instance of the DataStore plugin builder.
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * Builder object for the DataStore plugin.
     */
    public static final class Builder {
        private DataStoreConfiguration dataStoreConfiguration;
        private ModelProvider modelProvider;
        private SchemaRegistry schemaRegistry;
        private ApiCategory apiCategory;
        private AuthModeStrategyType authModeStrategy;
        private LocalStorageAdapter storageAdapter;
        private ReachabilityMonitor reachabilityMonitor;
        private boolean isSyncRetryEnabled;

        private Builder() {}

        /**
         * Sets the user-provided configuration options.
         * @param dataStoreConfiguration An instance of {@link DataStoreConfiguration} with the
         *                               desired options set.
         * @return Current builder instance, for fluent construction of plugin.
         */
        public Builder dataStoreConfiguration(DataStoreConfiguration dataStoreConfiguration) {
            this.dataStoreConfiguration = dataStoreConfiguration;
            return this;
        }

        /**
         * Sets the model provider field of the builder.
         * @param modelProvider An implementation of the {@link ModelProvider} interface.
         * @return Current builder instance, for fluent construction of plugin.
         */
        public Builder modelProvider(ModelProvider modelProvider) {
            this.modelProvider = modelProvider;
            return this;
        }

        /**
         * Sets the model schema registry of the builder.
         * @param schemaRegistry An instance of {@link SchemaRegistry}.
         * @return An implementation of the {@link ModelProvider} interface.
         */
        public Builder schemaRegistry(SchemaRegistry schemaRegistry) {
            this.schemaRegistry = schemaRegistry;
            return this;
        }

        /**
         * Package-private method to allow for injection of an API category for testing.
         * @param apiCategory An instance that implements ApiCategory.
         * @return Current builder instance, for fluent construction of plugin.
         */
        @VisibleForTesting
        Builder apiCategory(ApiCategory apiCategory) {
            this.apiCategory = apiCategory;
            return this;
        }

        /**
         * Package-private method to allow for injection of a storage adapter for testing purposes.
         * @param storageAdapter An instance that implements LocalStorageAdapter.
         * @return Current builder instance, for fluent construction of plugin.
         */
        @VisibleForTesting
        Builder storageAdapter(LocalStorageAdapter storageAdapter) {
            this.storageAdapter = storageAdapter;
            return this;
        }

        /**
         * Package-private method to allow for injection of a ReachabilityMonitor for testing purposes.
         * @param reachabilityMonitor An instance that implements LocalStorageAdapter.
         * @return Current builder instance, for fluent construction of plugin.
         */
        public Builder reachabilityMonitor(ReachabilityMonitor reachabilityMonitor) {
            this.reachabilityMonitor = reachabilityMonitor;
            return this;
        }

        /**
         * Sets the authorization mode strategy which will be used by DataStore sync engine
         * when interacting with the API plugin.
         * @param authModeStrategy One of the options from the {@link AuthModeStrategyType} enum.
         * @return An implementation of the {@link ModelProvider} interface.
         */
        public Builder authModeStrategy(AuthModeStrategyType authModeStrategy) {
            this.authModeStrategy = authModeStrategy;
            return this;
        }

        /**
         * Enables Retry on DataStore sync engine.
         * @deprecated This configuration will be deprecated in a future version.
         * @param isSyncRetryEnabled is sync retry enabled.
         * @return An implementation of the {@link ModelProvider} interface.
         */
        @Deprecated
        public Builder isSyncRetryEnabled(Boolean isSyncRetryEnabled) {
            LOG.warn("The isSyncRetryEnabled configuration will be deprecated in a future version."
                    + " Please discontinue use of this API.");
            this.isSyncRetryEnabled = isSyncRetryEnabled;
            return this;
        }

        /**
         * Builds the DataStore plugin.
         * @return An instance of the DataStore plugin ready for use.
         * @throws DataStoreException If unable to locate a model provider.
         */
        public AWSDataStorePlugin build() throws DataStoreException {
            return new AWSDataStorePlugin(Builder.this);
        }
    }
}
