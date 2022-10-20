/*
 * Copyright 2022 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package com.amplifyframework.auth.cognito.asf

import android.content.Context

/**
 * Base class for the data collectors. Data collectors provide logic to collect context data.
 */
internal interface DataCollector {
    /**
     * Actual logic to collect a category of data from device.
     * @param context android application context.
     * @return return key-value pair of user-context data.
     */
    fun collect(context: Context): Map<String, String?>
}
