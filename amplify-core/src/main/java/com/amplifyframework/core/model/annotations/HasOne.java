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
 * This annotation is used to indicate that a field has one to one relationship
 * with another class that extends {@link com.amplifyframework.core.model.Model}.
 * The model containing this annotation is the owner of annotated field.
 *
 * The {@link RetentionPolicy#RUNTIME} annotation is added to
 * retain {@link ModelField} at runtime for the reflection capabilities to work
 * in order to check if this annotation is present for a field of a Model.
 *
 * {@link ElementType#FIELD} annotation is added to indicate
 * {@link ModelField} annotation can be used only on Fields (Data members of a class).
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface HasOne {
    /**
     * Returns the name of connection.
     * @return the name of connection.
     */
    String name() default "";
}
