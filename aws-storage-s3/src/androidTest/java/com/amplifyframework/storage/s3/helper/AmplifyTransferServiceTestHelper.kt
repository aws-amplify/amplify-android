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

package com.amplifyframework.storage.s3.helper

import android.content.Context
import com.amplifyframework.storage.s3.service.AmplifyTransferService

// Without this helper class, calling AmplifyTransferService from androidTest Java classes results in Kotlin
// complaint about usage in different module
object AmplifyTransferServiceTestHelper {

    @JvmStatic
    fun stopForegroundAndUnbind(context: Context) {
        AmplifyTransferService.stopForegroundAndUnbind(context)
    }

    @JvmStatic
    fun isNotificationShowing(): Boolean = AmplifyTransferService.isNotificationShowing()
}
