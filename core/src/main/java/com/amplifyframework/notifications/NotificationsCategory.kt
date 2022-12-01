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

package com.amplifyframework.notifications

import android.content.Context
import com.amplifyframework.core.AmplifyConfiguration
import com.amplifyframework.core.category.Category
import com.amplifyframework.core.category.CategoryConfiguration
import com.amplifyframework.core.category.CategoryInitializationResult
import com.amplifyframework.core.category.CategoryType
import com.amplifyframework.core.category.SubCategoryType
import com.amplifyframework.notifications.pushnotifications.PushNotificationsCategory
import com.amplifyframework.notifications.pushnotifications.PushNotificationsPlugin

open class NotificationsCategory : Category<NotificationsPlugin<*>>(), NotificationsCategoryBehavior {
    @Suppress("PropertyName")
    @JvmField
    var Push: PushNotificationsCategory = PushNotificationsCategory()

    override fun getCategoryType(): CategoryType = CategoryType.NOTIFICATIONS

    /**
     * Category plugins are already configured and initialized.
     * Add plugins to appropriate subcategories and configure.
     * TODO: fix - plugin is configured twice, once for category and once for subcategory.
     */
    override fun initialize(context: Context): CategoryInitializationResult {
        val result = super.initialize(context)
        plugins.forEach { plugin ->
            when (plugin.getSubCategoryType()) {
                SubCategoryType.PUSH_NOTIFICATIONS -> {
                    Push.addPlugin(plugin as PushNotificationsPlugin<*>)
                    val configuration = AmplifyConfiguration.fromConfigFile(context)
                    val categoryConfiguration: CategoryConfiguration = configuration.forCategoryType(categoryType)
                    Push.configure(categoryConfiguration, context)
                }
                else -> Unit
            }
        }
        return result
    }

    override fun identifyUser(userId: String) {
        plugins.forEach { plugin ->
            plugin.identifyUser(userId)
        }
    }
}
