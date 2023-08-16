/*
 * Copyright 2020 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package com.amplifyframework.datastore.syncengine;

import androidx.annotation.NonNull;

import com.amplifyframework.AmplifyException;
import com.amplifyframework.core.Consumer;
import com.amplifyframework.core.model.Model;
import com.amplifyframework.core.model.ModelSchema;
import com.amplifyframework.core.model.SchemaRegistry;
import com.amplifyframework.core.model.SerializedModel;
import com.amplifyframework.core.model.temporal.Temporal;
import com.amplifyframework.datastore.DataStoreConfiguration;
import com.amplifyframework.datastore.DataStoreConfigurationProvider;
import com.amplifyframework.datastore.DataStoreConflictHandler;
import com.amplifyframework.datastore.DataStoreException;
import com.amplifyframework.datastore.appsync.AppSync;
import com.amplifyframework.datastore.appsync.AppSyncConflictUnhandledError;
import com.amplifyframework.datastore.appsync.AppSyncConflictUnhandledErrorFactory;
import com.amplifyframework.datastore.appsync.AppSyncMocking;
import com.amplifyframework.datastore.appsync.ModelMetadata;
import com.amplifyframework.datastore.appsync.ModelWithMetadata;
import com.amplifyframework.datastore.appsync.ModelWithMetadataAdapter;
import com.amplifyframework.testmodels.commentsblog.BlogOwner;
import com.amplifyframework.util.GsonFactory;

import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Tests the {@link ConflictResolver}.
 */
public final class ConflictResolverTest {
    private static final long TIMEOUT_SECONDS = 5;

    private ModelSchema schema;
    private DataStoreConfigurationProvider configurationProvider;
    private AppSync appSync;
    private ConflictResolver resolver;

    /**
     * Arranges an {@link ConflictResolver} against which to run tests.
     * @throws AmplifyException on failure to arrange schema
     */
    @Before
    public void setup() throws AmplifyException {
        this.schema = ModelSchema.fromModelClass(BlogOwner.class);
        this.configurationProvider = mock(DataStoreConfigurationProvider.class);
        this.appSync = mock(AppSync.class);
        this.resolver = new ConflictResolver(configurationProvider, appSync);
    }

    /**
     * When the user elects to apply the remote data, the following is expected:
     * 1. No additional calls are made to app sync.
     * @throws DataStoreException On failure to obtain configuration from provider,
     *                            or on failure to arrange metadata into storage
     */
    @Test
    public void conflictIsResolvedByApplyingRemoteData() throws DataStoreException {
        // The user provides a conflict handler which will always apply the remote
        // copy of the data.
        when(configurationProvider.getConfiguration())
            .thenReturn(DataStoreConfiguration.builder()
                .conflictHandler(DataStoreConflictHandler.alwaysApplyRemote())
                .build()
            );

        // Arrange some local pending mutation
        BlogOwner localSusan = BlogOwner.builder()
            .name("Local Susan")
            .build();
        PendingMutation<BlogOwner> mutation = PendingMutation.update(localSusan, schema);

        // Arrange some server data, that is in conflict
        BlogOwner serverSusan = localSusan.copyOfBuilder()
            .name("Remote Susan")
            .build();
        Temporal.Timestamp now = Temporal.Timestamp.now();
        ModelMetadata modelMetadata = new ModelMetadata(serverSusan.getId(), false, 2, now);
        ModelWithMetadata<BlogOwner> serverData = new ModelWithMetadata<>(serverSusan, modelMetadata);

        // Arrange a conflict error that we could hypothetically get from AppSync
        AppSyncConflictUnhandledError<BlogOwner> unhandledConflictError =
            AppSyncConflictUnhandledErrorFactory.createUnhandledConflictError(serverData);

        // When we try to resolve the conflict, the final resolved conflict
        // is identically the server's version.
        resolver.resolve(mutation, unhandledConflictError)
            .test()
            .awaitDone(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .assertValue(serverData);

        // AppSync wasn't called, since the server was already "correct."
        verifyNoInteractions(appSync);
    }

    /**
     * When the user elects to retry the mutation using the local copy of the data,
     * the following is expected:
     * 1. The AppSync API is invoked, with the local mutation data
     * 2. We assume that the AppSync API will respond differently
     *    upon retry (TODO: why? Will the user be expected to manually
     *    intervene and modify the backend state somehow?)
     * @throws DataStoreException On failure to arrange metadata into storage
     */
    @Test
    public void conflictIsResolvedByRetryingLocalData() throws DataStoreException {
        // Arrange for the user-provided conflict handler to always request local retry.
        when(configurationProvider.getConfiguration())
            .thenReturn(DataStoreConfiguration.builder()
                .conflictHandler(DataStoreConflictHandler.alwaysRetryLocal())
                .build()
            );

        // Arrange a pending mutation that includes the local data
        BlogOwner localModel = BlogOwner.builder()
            .name("Local Blogger")
            .build();
        PendingMutation<BlogOwner> mutation = PendingMutation.update(localModel, schema);

        // Arrange server state for the model, in conflict to local data
        BlogOwner serverModel = localModel.copyOfBuilder()
            .name("Server Blogger")
            .build();
        Temporal.Timestamp now = Temporal.Timestamp.now();
        ModelMetadata metadata = new ModelMetadata(serverModel.getId(), false, 4, now);
        ModelWithMetadata<BlogOwner> serverData = new ModelWithMetadata<>(serverModel, metadata);

        // Arrange a hypothetical conflict error from AppSync
        AppSyncConflictUnhandledError<BlogOwner> unhandledConflictError =
            AppSyncConflictUnhandledErrorFactory.createUnhandledConflictError(serverData);

        // Assume that the AppSync call succeeds this time.
        ModelWithMetadata<BlogOwner> versionFromAppSyncResponse =
            new ModelWithMetadata<>(localModel, metadata);
        AppSyncMocking.update(appSync)
            .mockSuccessResponse(localModel, metadata.getVersion(), versionFromAppSyncResponse);

        // Act: when the resolver is invoked, we expect the resolved version
        // to include the server's metadata, but with the local data.
        resolver.resolve(mutation, unhandledConflictError)
            .test()
            .awaitDone(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .assertValue(versionFromAppSyncResponse);

        // The handler should have called up to AppSync to update the model
        verify(appSync)
            .update(eq(localModel), any(), eq(metadata.getVersion()), any(), any());
    }

    /**
     * When the user elects to retry the mutation using the local copy of the data, and the mutation is a delete, the
     * following is expected:
     * 1. The AppSync delete API is invoked
     * 2. We assume that the AppSync API will respond differently upon retry
     * @throws DataStoreException On failure to arrange metadata into storage
     */
    @Test
    public void conflictIsResolvedByRetryingLocalDeletion() throws DataStoreException {
        // Arrange for the user-provided conflict handler to always request local retry.
        when(configurationProvider.getConfiguration())
            .thenReturn(DataStoreConfiguration.builder()
                            .conflictHandler(DataStoreConflictHandler.alwaysRetryLocal())
                            .build()
            );

        // Arrange a pending mutation that includes the local data
        BlogOwner localModel = BlogOwner.builder()
                                   .name("Local Blogger")
                                   .build();
        PendingMutation<BlogOwner> mutation = PendingMutation.deletion(localModel, schema);

        // Arrange server state for the model, in conflict to local data
        BlogOwner serverModel = localModel.copyOfBuilder()
                                    .name("Server Blogger")
                                    .build();
        Temporal.Timestamp now = Temporal.Timestamp.now();
        ModelMetadata metadata = new ModelMetadata(serverModel.getId(), false, 4, now);
        ModelWithMetadata<BlogOwner> serverData = new ModelWithMetadata<>(serverModel, metadata);

        // Arrange a hypothetical conflict error from AppSync
        AppSyncConflictUnhandledError<BlogOwner> unhandledConflictError =
            AppSyncConflictUnhandledErrorFactory.createUnhandledConflictError(serverData);

        // Assume that the AppSync call succeeds this time.
        ModelWithMetadata<BlogOwner> versionFromAppSyncResponse =
            new ModelWithMetadata<>(localModel, metadata);
        AppSyncMocking.delete(appSync)
            .mockSuccessResponse(localModel, metadata.getVersion(), versionFromAppSyncResponse);

        // Act: when the resolver is invoked, we expect the resolved version
        // to include the server's metadata, but with the local data.
        resolver.resolve(mutation, unhandledConflictError)
            .test()
            .awaitDone(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .assertValue(versionFromAppSyncResponse);

        // The handler should have called up to AppSync to delete the model
        verify(appSync)
            .delete(eq(localModel), any(), eq(metadata.getVersion()), any(), any());
    }

    /**
     * When the user elects to retry the mutation using the local copy of the data, the following is expected: 1. The
     * AppSync API is invoked, with the local mutation data 2. We assume that the AppSync API will respond differently
     * upon retry
     * @throws AmplifyException On failure to arrange metadata into storage
     */
    @Test
    public void conflictIsResolvedByRetryingLocalDataWithSerializedModel() throws AmplifyException {
        // Arrange for the user-provided conflict handler to always request local retry.
        when(configurationProvider.getConfiguration())
                .thenReturn(DataStoreConfiguration.builder()
                    .conflictHandler(DataStoreConflictHandler.alwaysRetryLocal())
                    .build());

        // Arrange a pending mutation that includes the local data
        BlogOwner localModel = BlogOwner.builder()
                .name("Local Blogger")
                .build();
        SchemaRegistry.instance().register(new HashSet<>(Arrays.asList(BlogOwner.class)));
        Map<String, Object> ownerData = new HashMap<>();
        ownerData.put("id", localModel.getId());
        ownerData.put("name", localModel.getName());
        ownerData.put(ModelWithMetadataAdapter.TYPE_NAME, localModel.getModelName());
        SerializedModel serializedOwner = SerializedModel.builder()
                .modelSchema(ModelSchema.fromModelClass(BlogOwner.class))
                .serializedData(ownerData)
                .build();
        PendingMutation<SerializedModel> mutation = PendingMutation.update(serializedOwner, schema);

        // Arrange server state for the model, in conflict to local data
        BlogOwner serverModel = localModel.copyOfBuilder()
                .name("Server Blogger")
                .build();
        Temporal.Timestamp now = Temporal.Timestamp.now();
        ModelMetadata metadata = new ModelMetadata(serverModel.getId(), false, 4, now);
        ModelWithMetadata<SerializedModel> serverData = new ModelWithMetadata<>(serializedOwner, metadata);

        // Arrange a hypothetical conflict error from AppSync
        AppSyncConflictUnhandledError<SerializedModel> unhandledConflictError =
                AppSyncConflictUnhandledErrorFactory.createUnhandledConflictError(serverData);

        // Assume that the AppSync call succeeds this time.
        ModelWithMetadata<BlogOwner> versionFromAppSyncResponse =
                new ModelWithMetadata<>(localModel, metadata);
        AppSyncMocking.update(appSync)
                .mockSuccessResponse(localModel, metadata.getVersion(), versionFromAppSyncResponse);

        // Act: when the resolver is invoked, we expect the resolved version
        // to include the server's metadata, but with the local data.
        resolver.resolve(mutation, unhandledConflictError)
                .test();
        // The handler should have called AppSync to update the model
        verify(appSync)
                .update(eq(localModel), any(), eq(metadata.getVersion()), any(), any());
    }

    /**
     * When the user elects to retry the mutation using the local copy of the data,
     * the following is expected:
     * 1. The AppSync API is invoked, with the local mutation data
     * 2. We assume that the AppSync API will respond differently
     *    upon retry.
     * @throws AmplifyException On failure to arrange metadata into storage
     */
    @Test
    public void conflictIsResolvedByRetryingLocalDataWithFlutterSerializedModel() throws AmplifyException {
        // Arrange for the user-provided conflict handler to always request local retry.
        when(configurationProvider.getConfiguration())
                .thenReturn(DataStoreConfiguration.builder()
                        .conflictHandler(DataStoreConflictHandler.alwaysRetryLocal())
                        .build());

        // Arrange a pending mutation that includes the local data
        Map<String, Object> blogOwnerData = new HashMap<>();
        blogOwnerData.put("name", "A seasoned writer");
        blogOwnerData.put("id", "e50ffa8f-783b-4780-89b4-27043ffc35be");
        SerializedModel serializedOwner = SerializedModel.builder()
                .modelSchema(schemaFrom())
                .serializedData(blogOwnerData)
                .build();
        PendingMutation<SerializedModel> mutation = PendingMutation.update(serializedOwner, schema);
        SchemaRegistry.instance().register(new HashSet<>(Arrays.asList(BlogOwner.class)));
        // Arrange server state for the model, in conflict to local data
        Map<String, Object> serverBlogOwnerData = new HashMap<>();
        serverBlogOwnerData.put("name", "A seasoned writer");
        serverBlogOwnerData.put("id", "e50ffa8f-783b-4780-89b4-27043ffc35be");
        SerializedModel serverModel = SerializedModel.builder()
                .modelSchema(schemaFrom())
                .serializedData(serverBlogOwnerData)
                .build();

        Temporal.Timestamp now = Temporal.Timestamp.now();
        ModelMetadata metadata = new ModelMetadata(serverModel.getPrimaryKeyString(), false, 4, now);
        ModelWithMetadata<SerializedModel> serverData = new ModelWithMetadata<>(serializedOwner, metadata);

        // Arrange a hypothetical conflict error from AppSync
        AppSyncConflictUnhandledError<SerializedModel> unhandledConflictError =
                AppSyncConflictUnhandledErrorFactory.createUnhandledConflictError(serverData);

        // Assume that the AppSync call succeeds this time.
        ModelWithMetadata<SerializedModel> versionFromAppSyncResponse =
                new ModelWithMetadata<>(serializedOwner, metadata);
        AppSyncMocking.update(appSync)
                .mockSuccessResponse(serializedOwner, metadata.getVersion(), versionFromAppSyncResponse);

        // Act: when the resolver is invoked, we expect the resolved version
        // to include the server's metadata, but with the local data.
        resolver.resolve(mutation, unhandledConflictError)
                .test();
        // The handler should have called up to AppSync to update the model
        verify(appSync)
                .update(eq(serializedOwner), any(), eq(metadata.getVersion()), any(), any());
    }

    /**
     * When the user elects to retry with a custom model, and that model
     * is not null, it means to try an update mutation against AppSync.
     * We expect:
     * 1. The AppSync API is invoked with an update mutation request.
     * @throws DataStoreException upon failure to arrange metadata into storage
     */
    @Test
    public void conflictIsResolvedByRetryingWithCustomModel() throws DataStoreException {
        // Arrange local model
        BlogOwner localModel = BlogOwner.builder()
            .name("Local model")
            .build();
        PendingMutation<BlogOwner> mutation = PendingMutation.update(localModel, schema);

        // Arrange a server model
        BlogOwner remoteModel = localModel.copyOfBuilder()
            .name("Remote model")
            .build();
        Temporal.Timestamp now = Temporal.Timestamp.now();
        ModelMetadata remoteMetadata = new ModelMetadata(remoteModel.getPrimaryKeyString(), false, 4, now);
        ModelWithMetadata<BlogOwner> remoteData = new ModelWithMetadata<>(remoteModel, remoteMetadata);
        // Arrange an unhandled conflict error based on the server data
        AppSyncConflictUnhandledError<BlogOwner> unhandledConflictError =
            AppSyncConflictUnhandledErrorFactory.createUnhandledConflictError(remoteData);

        // Arrange a conflict handler that returns a custom model
        BlogOwner customModel = localModel.copyOfBuilder()
            .name("Custom model")
            .build();
        when(configurationProvider.getConfiguration())
            .thenReturn(DataStoreConfiguration.builder()
                .conflictHandler(RetryWithModelHandler.create(customModel))
                .build()
            );

        // When the AppSync update API is called, return a mock response
        ModelMetadata metadata = new ModelMetadata(customModel.getPrimaryKeyString(), false,
                remoteMetadata.getVersion(), now);
        ModelWithMetadata<BlogOwner> responseData = new ModelWithMetadata<>(customModel, metadata);
        AppSyncMocking.update(appSync)
            .mockSuccessResponse(customModel, remoteMetadata.getVersion(), responseData);

        // When the resolver is called, the AppSync update API should be called,
        // and the resolver should return its response data.
        resolver.resolve(mutation, unhandledConflictError)
            .test()
            .awaitDone(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .assertValue(responseData);

        // Verify that the update API was invoked
        verify(appSync)
            .update(eq(customModel), any(), eq(remoteMetadata.getVersion()), any(), any());
    }

    private static ModelSchema schemaFrom() {
        String serializedForm = "{\n" +
                "    \"associations\": {\n" +
                "        \"blog\": {\n" +
                "            \"associatedName\": \"owner\",\n" +
                "            \"associatedType\": \"Blog\",\n" +
                "            \"name\": \"HasOne\"\n" +
                "        }\n" +
                "    },\n" +
                "    \"authRules\": [],\n" +
                "    \"fields\": {\n" +
                "        \"blog\": {\n" +
                "            \"authRules\": [],\n" +
                "            \"isArray\": false,\n" +
                "            \"isEnum\": false,\n" +
                "            \"isModel\": true,\n" +
                "            \"isRequired\": false,\n" +
                "            \"javaClassForValue\": \"com.amplifyframework.core.model.SerializedModel\",\n" +
                "            \"name\": \"blog\",\n" +
                "            \"targetType\": \"Blog\"\n" +
                "        },\n" +
                "        \"id\": {\n" +
                "            \"authRules\": [],\n" +
                "            \"isArray\": false,\n" +
                "            \"isEnum\": false,\n" +
                "            \"isModel\": false,\n" +
                "            \"isRequired\": true,\n" +
                "            \"javaClassForValue\": \"java.lang.String\",\n" +
                "            \"name\": \"id\",\n" +
                "            \"targetType\": \"ID\"\n" +
                "        },\n" +
                "        \"name\": {\n" +
                "            \"authRules\": [],\n" +
                "            \"isArray\": false,\n" +
                "            \"isEnum\": false,\n" +
                "            \"isModel\": false,\n" +
                "            \"isRequired\": true,\n" +
                "            \"javaClassForValue\": \"java.lang.String\",\n" +
                "            \"name\": \"name\",\n" +
                "            \"targetType\": \"String\"\n" +
                "        },\n" +
                "        \"wea\": {\n" +
                "            \"authRules\": [],\n" +
                "            \"isArray\": false,\n" +
                "            \"isEnum\": false,\n" +
                "            \"isModel\": false,\n" +
                "            \"isRequired\": false,\n" +
                "            \"javaClassForValue\": \"java.lang.String\",\n" +
                "            \"name\": \"wea\",\n" +
                "            \"targetType\": \"String\"\n" +
                "        }\n" +
                "    },\n" +
                "    \"indexes\": {},\n" +
                "    \"modelClass\": \"com.amplifyframework.core.model.SerializedModel\",\n" +
                "    \"name\": \"BlogOwner\",\n" +
                "    \"pluralName\": \"BlogOwners\"\n" +
                "}\n";
        return GsonFactory.instance().fromJson(serializedForm, ModelSchema.class);
    }

    /**
     * This is convenient for our test, but isn't a good enough interface to live in the
     * customer-available {@link DataStoreConflictHandler}.
     */
    private static final class RetryWithModelHandler implements DataStoreConflictHandler {
        private final Model model;

        private RetryWithModelHandler(Model model) {
            this.model = model;
        }

        /**
         * Creates a new RetryWithModelHandler.
         * @param model A custom model to reply with
         * @return A RetryWithModelHandler
         */
        private static RetryWithModelHandler create(Model model) {
            return new RetryWithModelHandler(model);
        }

        @Override
        public void onConflictDetected(
                @NonNull ConflictData<? extends Model> conflictData,
                @NonNull Consumer<ConflictResolutionDecision<? extends Model>> onDecision) {
            onDecision.accept(ConflictResolutionDecision.retry(model));
        }
    }
}
