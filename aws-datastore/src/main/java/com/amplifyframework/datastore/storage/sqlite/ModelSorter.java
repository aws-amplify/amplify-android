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

package com.amplifyframework.datastore.storage.sqlite;

import com.amplifyframework.core.Consumer;
import com.amplifyframework.core.model.Model;
import com.amplifyframework.core.model.query.ObserveQueryOptions;
import com.amplifyframework.core.model.query.QuerySortBy;
import com.amplifyframework.core.model.query.QuerySortOrder;
import com.amplifyframework.datastore.DataStoreException;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Class for sorting objects of type model.
 * @param <T> type of Model.
 */
public class ModelSorter<T extends Model> {

    /**
     * Sorts type T which extends model.
     * @param options query options.
     * @param  list list of items to be sorted.
     * @param  itemClass the class of type to be sorted.
     * @param onObservationError invoked on observation error.
     * */
    public void sort(ObserveQueryOptions options,
                     List<T> list,
                     Class<T> itemClass,
                     Consumer<DataStoreException> onObservationError) {
        if (options != null && options.getSortBy() != null && options.getSortBy().size() > 0) {
            Comparator<T> comparator = getComparator(options.getSortBy(), itemClass, onObservationError);
            Collections.sort(list, comparator);
        }
    }

    private Comparator<T> getComparator(List<QuerySortBy> sortByList,
                                        Class<T> itemClass,
                                        Consumer<DataStoreException> onObservationError) {
        QuerySortBy sortBy = sortByList.get(0);

        Comparator<T> comparator = new ModelComparator<T>(sortBy, itemClass, onObservationError);
        QuerySortOrder sortOrder = sortBy.getSortOrder();
        if (sortOrder == QuerySortOrder.DESCENDING) {
            comparator = comparator.reversed();
        }

        for (int i = 1; i < sortByList.size(); i++) {
            QuerySortBy nextSortBy = sortByList.get(i);
            Comparator<T> nextComparator = comparator.thenComparing(new ModelComparator<T>(nextSortBy,
                    itemClass, onObservationError));
            if (nextSortBy.getSortOrder() == QuerySortOrder.DESCENDING) {
                nextComparator.reversed();
            }
        }
        return comparator;
    }
}
