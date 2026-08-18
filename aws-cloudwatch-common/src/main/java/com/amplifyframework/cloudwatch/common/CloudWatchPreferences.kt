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
package com.amplifyframework.cloudwatch.common

import com.amplifyframework.annotations.InternalAmplifyApi

/**
 * Shared-preferences identifiers used by the CloudWatch logging clients — for example the file that
 * persists the per-install device id used when naming log streams. Defined once here so every
 * client reads and writes the same store.
 */
@InternalAmplifyApi
object CloudWatchPreferences {
    const val SHARED_PREFERENCE_FILENAME = "com.amplify.logging.a3fa4188-0ac5-11ee-be56-0242ac120002"

    /** Preferences key under which the per-install device id is stored. */
    const val DEVICE_ID_KEY = "unique_device_id"
}
