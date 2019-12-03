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

/**
 * A Strategy to update the models of type {@link com.amplifyframework.core.model.Model}
 * when the version of the models change.
 *
 * @param <U> type of modelUpdater that can do the model update
 * @param <V> type of the version object
 */
interface ModelUpdateStrategy<U, V> {
    /**
     * Upgrades the models from oldVersion to newVersion.
     *
     * @param modelUpdater implementation that can perform the model update
     * @param oldVersion older version of models to be upgraded from
     * @param newVersion newer version of models to be upgraded to
     */
    void update(U modelUpdater, V oldVersion, V newVersion);
}
