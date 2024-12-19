/*
 * Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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
package com.amplifyframework.preference

import android.content.Context
import androidx.annotation.WorkerThread
import com.amplifyframework.AmplifyException
import com.amplifyframework.core.category.CategoryType
import com.amplifyframework.core.plugin.Plugin

abstract class PreferencePlugin<E> : PreferenceCategoryBehavior, Plugin<E> {
    override fun getCategoryType(): CategoryType {
        return CategoryType.PREFERENCE
    }

    @WorkerThread
    @Throws(AmplifyException::class)
    override fun initialize(context: Context) {
    }
}
