/*
 * Copyright 2026 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package com.amplifyframework.annotations

/**
 * An API marked with this annotation is experimental. It may be changed or removed in a future release without
 * following the standard deprecation cycle.
 *
 * Any usage of this API must be explicitly opted in with [OptIn] (e.g., `@OptIn(ExperimentalAmplifyApi::class)`) or
 * by annotating the consuming declaration with [ExperimentalAmplifyApi].
 */
@MustBeDocumented
@Retention(AnnotationRetention.BINARY)
@Target(
    AnnotationTarget.CLASS,
    AnnotationTarget.TYPEALIAS,
    AnnotationTarget.FUNCTION,
    AnnotationTarget.PROPERTY,
    AnnotationTarget.FIELD,
    AnnotationTarget.CONSTRUCTOR
)
@RequiresOptIn(
    level = RequiresOptIn.Level.WARNING,
    message = "This API is experimental. It may be changed or removed without notice."
)
annotation class ExperimentalAmplifyApi
