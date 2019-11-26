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

package com.amplifyframework.core.model.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * {@link Indexes} allows an instance of {@link com.amplifyframework.core.model.Model}
 * to be annotated with multiple {@link Index}.
 *
 * The {@link RetentionPolicy#RUNTIME} annotation is added to
 * retain {@link Indexes} at runtime for the reflection capabilities to work
 * in order to check if this annotation is present for a field of a Model.
 *
 * {@link ElementType#TYPE} annotation is added to indicate
 * {@link Indexes} annotation can be used only on types (class, interface, enum).
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Indexes {
    /**
     * Specify the array of indexes of a {@link com.amplifyframework.core.model.Model}.
     * @return array of indexes
     */
    Index[] value();
}
