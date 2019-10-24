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

package com.amplifyframework.core.stream;

import androidx.annotation.NonNull;

import com.amplifyframework.core.category.CategoryType;

import java.util.UUID;

/**
 * An abstract representation of an Amplify pubsub observable.
 * @param <T> data type of the item or event being observed
 */
public abstract class AmplifyObservable<T> implements Observable<T> {

    // The unique ID of the observable that can be used to identify
    // previously tracked observable.
    private final UUID observableId;

    // Category type for hub.
    private final CategoryType categoryType;

    /**
     * Constructs a new AmplifyObservable.
     * @param categoryType The category in which this observable is publishing
     */
    public AmplifyObservable(@NonNull final CategoryType categoryType) {
        this.categoryType = categoryType;
        this.observableId = UUID.randomUUID();
    }

    /**
     * Gets the ID of the observable.
     * @return Observable unique ID
     */
    public final UUID getObservableId() {
        return observableId;
    }

    /**
     * Gets the category type.
     * @return Category type
     */
    public final CategoryType getCategoryType() {
        return categoryType;
    }
}

