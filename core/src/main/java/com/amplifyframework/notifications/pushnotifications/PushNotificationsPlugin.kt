package com.amplifyframework.notifications.pushnotifications

import android.content.Context
import com.amplifyframework.AmplifyException
import com.amplifyframework.core.category.CategoryType
import com.amplifyframework.core.category.SubCategoryType
import com.amplifyframework.notifications.NotificationsPlugin

abstract class PushNotificationsPlugin<E> : PushNotificationsCategoryBehavior, NotificationsPlugin<E>() {
    override fun getCategoryType() = CategoryType.NOTIFICATIONS
    override fun getSubCategoryType() = SubCategoryType.PUSH_NOTIFICATIONS

    @Throws(AmplifyException::class)
    override fun initialize(context: Context) { }
}
