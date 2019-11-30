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

package com.amplifyframework.datastore.network;

import androidx.annotation.NonNull;

import com.amplifyframework.api.ApiCategoryBehavior;
import com.amplifyframework.datastore.DataStoreException;

/**
 * A way to find the name of the configured APIs we are supposed to
 * be talking to. We use this, instead of just a direct reference to the API name,
 * since we may like to create an {@link SyncEngine} before we know this value.
 * This functional interface serves as a provider delegate.
 */
@FunctionalInterface
public interface ConfiguredApiProvider {
    /**
     * Gets the name of the API that has been configured for use with the DataStore.
     * This value can be passed into the {@link ApiCategoryBehavior} methods which
     * require an API name.
     * @return Name of API for {@link ApiCategoryBehavior} calls
     * @throws DataStoreException If there is no such API configured for use
     */
    @NonNull
    String getDataStoreApiName() throws DataStoreException, DataStoreException;
}
