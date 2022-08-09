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
package com.amplifyframework.analytics.pinpoint.internal.core.util

import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * A utility class for date marshalling
 */
object DateUtil {
    // date formats are not thread safe, so we must synchronize it since
    // this is shared
    @get:Synchronized
    private const val DATE_FORMAT_STRING = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"
    private val dateFormat: DateFormat = SimpleDateFormat(DATE_FORMAT_STRING, Locale.US)

    /**
     * get date in iso format "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"
     *
     * @param millis the time in milli seconds
     * @return the formatted date.
     */
    @JvmStatic
    @Synchronized
    fun isoDateFromMillis(millis: Long): String {
        return dateFormat.format(Date(millis))
    }

    /**
     * Formats the specified date as an ISO 8601 string.
     *
     * @param date The date to format.
     * @return The ISO 8601 string representing the specified date.
     */
    @JvmStatic
    fun formatISO8601Date(date: Date): String {
        return dateFormat.format(date)
    }

    init {
        dateFormat.timeZone = TimeZone.getTimeZone("UTC")
    }
}
