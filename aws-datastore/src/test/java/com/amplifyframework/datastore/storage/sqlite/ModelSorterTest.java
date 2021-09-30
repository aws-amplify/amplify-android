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

import com.amplifyframework.core.model.query.ObserveQueryOptions;
import com.amplifyframework.core.model.query.QuerySortBy;
import com.amplifyframework.testmodels.commentsblog.BlogOwner;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class ModelSorterTest {

    /***
     * Sort method returns sorted list.
     */
    @Test
    public void sortReturnsSortedList() {
        List<String> names = Arrays.asList("John", "Jacob", "Joe", "Bob", "Bobby", "Bobb", "Dan", "Dany", "Daniel");
        List<String> weas = Arrays.asList("pon", "lth", "ver", "kly", "ken", "sel", "ner", "rer", "ned");
        List<BlogOwner> owners = new ArrayList<>();

        for (int i = 0; i < names.size() / 2; i++) {
            BlogOwner owner = BlogOwner.builder()
                    .name(names.get(i))
                    .wea(weas.get(i))
                    .build();
            owners.add(owner);
        }
        List<QuerySortBy> sortBy = new ArrayList<>();
        sortBy.add(BlogOwner.NAME.descending());
        sortBy.add(BlogOwner.WEA.ascending());
        ModelSorter<BlogOwner> subject = new ModelSorter<>();
        subject.sort(new ObserveQueryOptions(null, sortBy), owners, BlogOwner.class);
        List<BlogOwner> sorted = new ArrayList<>(owners);
        Collections.sort(sorted, Comparator
                .comparing(BlogOwner::getName)
                .reversed()
                .thenComparing(BlogOwner::getWea)
        );
        assertEquals(sorted, owners);
    }

}
