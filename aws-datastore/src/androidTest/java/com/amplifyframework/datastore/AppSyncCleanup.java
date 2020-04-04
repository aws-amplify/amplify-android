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

package com.amplifyframework.datastore;

import android.content.Context;
import androidx.annotation.NonNull;

import com.amplifyframework.AmplifyException;
import com.amplifyframework.api.ApiCategory;
import com.amplifyframework.api.aws.AWSApiPlugin;
import com.amplifyframework.core.Amplify;
import com.amplifyframework.core.AmplifyConfiguration;
import com.amplifyframework.core.Consumer;
import com.amplifyframework.core.category.CategoryConfiguration;
import com.amplifyframework.core.category.CategoryType;
import com.amplifyframework.core.model.Model;
import com.amplifyframework.core.model.ModelProvider;
import com.amplifyframework.datastore.appsync.AppSync;
import com.amplifyframework.datastore.appsync.AppSyncClient;
import com.amplifyframework.datastore.appsync.ModelMetadata;
import com.amplifyframework.datastore.appsync.ModelWithMetadata;
import com.amplifyframework.datastore.appsync.SynchronousAppSync;
import com.amplifyframework.datastore.test.R;
import com.amplifyframework.logging.Logger;

import java.util.Objects;

/**
 * A utility to delete resources at an AppSync endpoint.
 * Note: this works by executing {@link AppSync#delete(Class, String, Integer, Consumer, Consumer)}
 * calls. Better might be to clear records out of Dynamo, entirely.
 * This utility doesn't actually leave the databases in a cleared out state, it just toggles
 * the metadata for a bunch of records, so that {@link ModelMetadata#isDeleted()} is true.
 */
final class AppSyncCleanup {
    private static final Logger LOG = Amplify.Logging.forNamespace("amplify:aws-datastore:test");
    private final SynchronousAppSync appSync;

    private AppSyncCleanup(SynchronousAppSync appSync) {
        this.appSync = appSync;
    }

    static AppSyncCleanup instance(@NonNull Context context) throws AmplifyException {
        Objects.requireNonNull(context);

        // Create an independent API instance. We don't want to do this
        // through the Amplify facade, since that's a one-way door as its a
        // process singleton.
        ApiCategory apiDelegate = new ApiCategory();
        apiDelegate.addPlugin(new AWSApiPlugin());
        CategoryConfiguration apiCategoryConfiguration =
            AmplifyConfiguration.fromConfigFile(context, R.raw.amplifyconfiguration)
                .forCategoryType(CategoryType.API);
        apiDelegate.configure(apiCategoryConfiguration, context);
        apiDelegate.initialize(context);

        // Now, wrap that configured API up behind an AppSync client.
        // Wrap once more into a *synchronous* AppSync client.
        AppSync appSyncDelegate = AppSyncClient.delegatingTo(apiDelegate);
        SynchronousAppSync appSync = SynchronousAppSync.delegatingTo(appSyncDelegate);

        return new AppSyncCleanup(appSync);
    }

    /**
     * Delete all models of the provided types, on the AppSync backend.
     * @param modelProvider All instances of the provided model classes will be deleted
     * @throws AmplifyException On failure to query the models to delete, or on failure to delete any model
     */
    void deleteAll(ModelProvider modelProvider) throws AmplifyException {
        LOG.info("Cleaning up backend resources for DataStore tests.");
        // Loop over all the models. Understand the backend state of each.
        // Iterate over all, and delete everything.
        for (Class<? extends Model> modelClass : modelProvider.models()) {
            for (ModelWithMetadata<? extends Model> modelWithMetadata : appSync.sync(modelClass, null).getData()) {
                delete(modelWithMetadata);
            }
        }
    }

    /**
     * Deletes a model, in a synchronous way.
     * @param modelWithMetadata A model coupled with its AppSync metadata
     * @param <T> Type of model
     * @throws DataStoreException On failure to delete model
     */
    private <T extends Model> void delete(ModelWithMetadata<T> modelWithMetadata) throws DataStoreException {
        Model model = modelWithMetadata.getModel();
        ModelMetadata metadata = modelWithMetadata.getSyncMetadata();
        Integer version = metadata.getVersion() == null ? 0 : metadata.getVersion();
        appSync.delete(model.getClass(), model.getId(), version);
        LOG.info("Deleted model with name=" + model.getClass().getSimpleName() + ", id=" + model.getId());
    }
}
