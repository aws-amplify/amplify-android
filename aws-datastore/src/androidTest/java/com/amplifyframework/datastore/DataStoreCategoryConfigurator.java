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
import androidx.annotation.IntRange;
import androidx.annotation.NonNull;
import androidx.annotation.RawRes;

import com.amplifyframework.AmplifyException;
import com.amplifyframework.api.ApiCategory;
import com.amplifyframework.core.AmplifyConfiguration;
import com.amplifyframework.core.NoOpAction;
import com.amplifyframework.core.NoOpConsumer;
import com.amplifyframework.core.category.CategoryConfiguration;
import com.amplifyframework.core.category.CategoryType;
import com.amplifyframework.core.model.ModelProvider;
import com.amplifyframework.hub.HubChannel;
import com.amplifyframework.testutils.HubAccumulator;

import java.util.Objects;
import java.util.concurrent.TimeUnit;

final class DataStoreCategoryConfigurator {
    private Context context;
    @RawRes private Integer resourceId;
    private ModelProvider modelProvider;
    private ApiCategory api;
    private boolean clearRequested;
    private int timeoutAmount;
    private TimeUnit timeoutUnit;

    private DataStoreCategoryConfigurator() {}

    @NonNull
    static DataStoreCategoryConfigurator begin() {
        return new DataStoreCategoryConfigurator();
    }

    @SuppressWarnings("SameParameterValue")
    @NonNull
    DataStoreCategoryConfigurator clearDatabase(boolean willClear) {
        this.clearRequested = willClear;
        return DataStoreCategoryConfigurator.this;
    }

    @NonNull
    DataStoreCategoryConfigurator context(@NonNull Context context) {
        this.context = Objects.requireNonNull(context);
        return DataStoreCategoryConfigurator.this;
    }

    @NonNull
    DataStoreCategoryConfigurator resourceId(@RawRes int resourceId) {
        this.resourceId = resourceId;
        return DataStoreCategoryConfigurator.this;
    }

    @NonNull
    DataStoreCategoryConfigurator modelProvider(@NonNull ModelProvider modelProvider) {
        this.modelProvider = Objects.requireNonNull(modelProvider);
        return DataStoreCategoryConfigurator.this;
    }

    @NonNull
    DataStoreCategoryConfigurator api(@NonNull ApiCategory api) {
        this.api = Objects.requireNonNull(api);
        return DataStoreCategoryConfigurator.this;
    }

    @NonNull
    DataStoreCategoryConfigurator timeout(
            @IntRange(from = 0) int amount,
            @SuppressWarnings("SameParameterValue") @NonNull TimeUnit timeUnit) {
        this.timeoutAmount = amount;
        this.timeoutUnit = Objects.requireNonNull(timeUnit);
        return DataStoreCategoryConfigurator.this;
    }

    @NonNull
    DataStoreCategory finish() throws AmplifyException {
        // Make sure everything was supplied.
        Objects.requireNonNull(context);
        Objects.requireNonNull(resourceId);
        Objects.requireNonNull(modelProvider);
        Objects.requireNonNull(api);

        if (clearRequested) {
            context.deleteDatabase("AmplifyDatastore.db");
        }

        return buildCategory();
    }

    private DataStoreCategory buildCategory() throws AmplifyException {
        HubAccumulator initializationObserver =
            HubAccumulator.create(HubChannel.DATASTORE, DataStoreChannelEventName.READY, 1)
                .start();

        CategoryConfiguration dataStoreConfiguration =
            AmplifyConfiguration.fromConfigFile(context, resourceId)
                .forCategoryType(CategoryType.DATASTORE);

        AWSDataStorePlugin awsDataStorePlugin = AWSDataStorePlugin.builder()
                                                                  .modelProvider(modelProvider)
                                                                  .apiCategory(api)
                                                                  .build();
        DataStoreCategory dataStoreCategory = new DataStoreCategory();
        dataStoreCategory.addPlugin(awsDataStorePlugin);
        dataStoreCategory.configure(dataStoreConfiguration, context);
        dataStoreCategory.initialize(context);
        dataStoreCategory.start(NoOpAction.create(), NoOpConsumer.create());

        initializationObserver.await(timeoutAmount, timeoutUnit);

        return dataStoreCategory;
    }
}
