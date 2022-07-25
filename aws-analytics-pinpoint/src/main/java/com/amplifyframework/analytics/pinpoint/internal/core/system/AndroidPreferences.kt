/*
 *  Copyright 2016-2022 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License").
 *  You may not use this file except in compliance with the License.
 *  A copy of the License is located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 *  or in the "license" file accompanying this file. This file is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 *  express or implied. See the License for the specific language governing
 *  permissions and limitations under the License.
 *
 */

package com.amplifyframework.analytics.pinpoint.internal.core.system

import android.content.Context
import android.content.SharedPreferences


open class AndroidPreferences(context: Context?, preferencesKey: String) {
    private val preferences: SharedPreferences = context?.getSharedPreferences(
        preferencesKey,
        Context.MODE_PRIVATE
    )!!

    open fun getBoolean(key: String, optValue: Boolean): Boolean {
        return preferences.getBoolean(key, optValue)
    }

    open fun getInt(key: String, optValue: Int): Int {
        return preferences.getInt(key, optValue)
    }

    open fun getFloat(key: String, optValue: Float): Float {
        return preferences.getFloat(key, optValue)
    }

    open fun getLong(key: String, optValue: Long): Long {
        return preferences.getLong(key, optValue)
    }

    open fun getString(key: String, optValue: String?): String? {
        return preferences.getString(key, optValue)
    }

    open fun putBoolean(key: String, value: Boolean) {
        val editor = preferences.edit()
        editor.putBoolean(key, value)
        editor.commit()
    }

    open fun putInt(key: String, value: Int) {
        val editor = preferences.edit()
        editor.putInt(key, value)
        editor.commit()
    }

    open fun putFloat(key: String, value: Float) {
        val editor = preferences.edit()
        editor.putFloat(key, value)
        editor.commit()
    }

    open fun putLong(key: String, value: Long) {
        val editor = preferences.edit()
        editor.putLong(key, value)
        editor.commit()
    }

    open fun putString(key: String, value: String) {
        val editor = preferences.edit()
        editor.putString(key, value)
        editor.commit()
    }
}
