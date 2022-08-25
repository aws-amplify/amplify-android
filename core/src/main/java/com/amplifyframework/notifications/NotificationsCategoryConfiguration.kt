package com.amplifyframework.notifications

import com.amplifyframework.core.category.CategoryConfiguration
import com.amplifyframework.core.category.CategoryType

/**
 * Configuration for Notifications category that also
 * contains configuration for individual plugins.
 */
class NotificationsCategoryConfiguration : CategoryConfiguration() {
    // Any category level properties would be defined here and populateFromJson would be overridden
    // below to fill in these values from the JSON data.
    /**
     * Gets the category type associated with the current object.
     *
     * @return The category type to which the current object is affiliated
     */
    override fun getCategoryType(): CategoryType {
        return CategoryType.NOTIFICATIONS
    }
}
