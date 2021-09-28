/*
 * Copyright 2021 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

import com.amplifyframework.core.model.Model;
import com.amplifyframework.util.Immutable;

import java.util.List;

/***
 * Class which holds the snapshot of datastore queries.
 * @param <T> type of Model.
 */
public class DataStoreQuerySnapshot<T extends Model> {
    private final List<T> items;
    private final boolean isSynced;

    /***
     * Construtor for DataStoreQuerySnapshot.
     * @param items List of items.
     * @param isSynced sync status of the local datastore.
     */
    public DataStoreQuerySnapshot(List<T> items, boolean isSynced) {
        this.items = items;
        this.isSynced = isSynced;
    }

    /***
     * Get items.
     * @return List of items.
     */
    public List<T> getItems() {
        return Immutable.of(items);
    }

    /***
     * Get is synced.
     * @return synced status of local datastore.
     */
    public boolean getIsSynced() {
        return isSynced;
    }
}
