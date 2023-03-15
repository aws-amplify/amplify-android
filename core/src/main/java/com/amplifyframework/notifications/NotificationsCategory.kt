/*
 * Copyright 2023 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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
import com.amplifyframework.analytics.UserProfile
import com.amplifyframework.core.Action
import com.amplifyframework.core.Consumer
import com.amplifyframework.core.category.Category
import com.amplifyframework.core.category.CategoryConfiguration
import com.amplifyframework.core.category.CategoryType
import com.amplifyframework.core.category.EmptyCategoryConfiguration
import com.amplifyframework.core.category.SubCategoryType.PUSH_NOTIFICATIONS
import com.amplifyframework.notifications.pushnotifications.PushNotificationsCategory
import com.amplifyframework.notifications.pushnotifications.PushNotificationsException
import com.amplifyframework.notifications.pushnotifications.PushNotificationsPlugin

open class NotificationsCategory : Category<NotificationsPlugin<*>>(), NotificationsCategoryBehavior {
    @Suppress("PropertyName")
    @JvmField
    var Push: PushNotificationsCategory = PushNotificationsCategory()

    override fun getCategoryType(): CategoryType = CategoryType.NOTIFICATIONS

    /**
     * Plugins are added to both category and subcategory. Plugins are configured only once during
     * subcategory configure call. Subcategory config is required to extract plugin from subcategory
     * JSON object.
     *
     * Note: Plugins are initialized only once during category initialization. Subcategory initialization
     * in not required as the plugins are already initialized as category level.
     */
    override fun configure(configuration: CategoryConfiguration, context: Context) {
        plugins.forEach { plugin ->
            when (plugin.getSubCategoryType()) {
                PUSH_NOTIFICATIONS -> {
                    Push.addPlugin(plugin as PushNotificationsPlugin<*>)

                    val notificationsConfiguration = configuration as? NotificationsCategoryConfiguration
                    Push.configure(
                        notificationsConfiguration ?: EmptyCategoryConfiguration.forCategoryType(categoryType),
                        context
                    )
                }
                else -> Unit
            }
        }
    }

    /**
     * Defer to subcategories for top level APIs.
     * Ex: subCategories.forEach { it.identifyUser(userId, onSuccess, onError) }
     */
    override fun identifyUser(
        userId: String,
        onSuccess: Action,
        onError: Consumer<PushNotificationsException>
    ) {
        Push.identifyUser(userId, onSuccess, onError)
    }

    /**
     * Defer to subcategories for top level APIs.
     * Ex: subCategories.forEach { it.identifyUser(userId, profile, onSuccess, onError) }
     */
    override fun identifyUser(
        userId: String,
        profile: UserProfile,
        onSuccess: Action,
        onError: Consumer<PushNotificationsException>
    ) {
        Push.identifyUser(userId, profile, onSuccess, onError)
    }
}
