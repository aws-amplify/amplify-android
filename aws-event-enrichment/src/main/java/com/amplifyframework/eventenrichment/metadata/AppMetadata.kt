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
package com.amplifyframework.eventenrichment.metadata

/**
 * Application-level metadata stamped on every event.
 *
 * @param appId Application identifier used in the event envelope.
 * @param packageName Application package name.
 * @param versionName Application version name.
 * @param versionCode Application version code.
 * @param title Application display title.
 */
data class AppMetadata(
    val appId: String,
    val packageName: String? = null,
    val versionName: String? = null,
    val versionCode: String? = null,
    val title: String? = null
)
