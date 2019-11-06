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

package com.amplifyframework.datastore.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * {@link Index} annotates any {@link com.amplifyframework.datastore.model.Model}
 * with the index that will be configured in the persistence layer. For example,
 * this will dictate the indexes that will be created when a {@link com.amplifyframework.datastore.model.Model}
 * is stored in the Android {@link android.database.sqlite.SQLiteDatabase}.
 *
 * The {@link RetentionPolicy#RUNTIME} annotation is added to
 * retain {@link Index} at runtime for the reflection capabilities to work
 * in order to check if this annotation is present for a field of a Model.
 *
 * {@link ElementType#TYPE} annotation is added to indicate
 * {@link Index} annotation can be used only on types (class, interface, enum).
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Index {
    /**
     * Specify the array of fields of a {@link com.amplifyframework.datastore.model.Model}
     * for which indexes need to be created in the persistent store.
     * @return array of fields
     */
    String[] fields();

    /**
     * Return the name of the index.
     * @return the name of the index.
     */
    String name();
}
