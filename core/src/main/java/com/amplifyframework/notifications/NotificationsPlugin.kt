package com.amplifyframework.notifications

import android.content.Context
import com.amplifyframework.AmplifyException
import com.amplifyframework.core.category.CategoryType
import com.amplifyframework.core.category.SubCategoryType
import com.amplifyframework.core.plugin.Plugin

abstract class NotificationsPlugin<E> : NotificationsCategoryBehavior, Plugin<E> {
    override fun getCategoryType() = CategoryType.NOTIFICATIONS
    abstract fun getSubCategoryType(): SubCategoryType

    @Throws(AmplifyException::class)
    override fun initialize(context: Context) { }
}
