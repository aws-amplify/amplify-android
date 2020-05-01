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

/**
 * Implementations of this interface should provide access to a <code>DataStoreConfiguration</code>
 * that represents the configuration used across all DataStore underlying components.
 *
 * Plugin-specific implementations are expected to document specific behaviors and/or side-effects.
 */
public interface DataStoreConfigurationProvider {

    /**
     * Returns the configuration object.
     * @return the configuration object.
     * @throws DataStoreException if errors happen during configuration building/parsing.
     */
    DataStoreConfiguration getConfiguration() throws DataStoreException;
}
