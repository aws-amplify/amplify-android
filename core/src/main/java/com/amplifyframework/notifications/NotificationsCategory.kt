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
    var PushNotifications: PushNotificationsCategory = PushNotificationsCategory()

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
                    PushNotifications.addPlugin(plugin as PushNotificationsPlugin<*>)
                    val configuration = AmplifyConfiguration.fromConfigFile(context)
                    val categoryConfiguration: CategoryConfiguration = configuration.forCategoryType(categoryType)
                    PushNotifications.configure(categoryConfiguration, context)
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
