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

import com.amplifyframework.core.model.Model;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * {@link ModelConfig} annotates any {@link Model} with applicable configuration information.
 *
 * The {@link RetentionPolicy#RUNTIME} annotation is added to
 * retain {@link ModelConfig} at runtime for the reflection capabilities to work
 * in order to check if this annotation is present for a field of a Model.
 *
 * {@link ElementType#TYPE} annotation is added to indicate
 * {@link ModelConfig} annotation can be used only on types (class, interface, enum).
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface ModelConfig {
    /**
     * Specifies the plural version of the model name.
     * @return the plural version of the name.
     * @Deprecated instead use listPluralName or syncPluralName
     */
    String pluralName() default "";

    /**
     * Specifies the plural version for list query of the model name.
     * @return the plural version of the name for list query.
     */
    String listPluralName() default "";

    /**
     * Specifies the plural version for sync query of the model name.
     * @return the plural version of the name for sync query.
     */
    String syncPluralName() default "";

    /**
     * Specifies an array of authorization rules that should apply to this {@link Model}.
     * @return list of {@link AuthRule} annotations.
     */
    AuthRule[] authRules() default {};

    /**
     * Model Type SYSTEM or USER.
     * @return Type of Model.
     */
    Model.Type type() default Model.Type.USER;

    /**
     * Model Version.
     * @return Version of Model.
     */
    int version() default 0;

    /**
     * Specifies if a Model supports fields with lazy types.
     * @return true if model support fields with lazy types.
     */
    boolean hasLazySupport() default false;
}
