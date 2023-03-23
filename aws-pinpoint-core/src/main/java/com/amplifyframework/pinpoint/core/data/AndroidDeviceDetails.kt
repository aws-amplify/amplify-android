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

package com.amplifyframework.pinpoint.core.data

import android.content.Context
import android.os.Build
import android.telephony.TelephonyManager
import com.amplifyframework.annotations.InternalAmplifyApi
import com.amplifyframework.pinpoint.core.util.LocaleSerializer
import java.util.Locale
import kotlinx.serialization.Serializable

@Serializable
@InternalAmplifyApi
data class AndroidDeviceDetails constructor(
    val carrier: String? = null,
    val platformVersion: String = Build.VERSION.RELEASE ?: "TEST VERSION",
    val platform: String = "ANDROID",
    val manufacturer: String = Build.MANUFACTURER ?: "TEST MANUFACTURER",
    val model: String = Build.MODEL ?: "TEST MODEL",
    @Serializable(with = LocaleSerializer::class)
    val locale: Locale = Locale.getDefault()
) {
    constructor(context: Context) : this(getCarrier(context))

    companion object {
        fun getCarrier(context: Context): String {
            val telephony = context.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager
            return telephony?.let {
                if (it.networkOperatorName.isNullOrBlank()) {
                    it.networkOperatorName
                } else {
                    "Unknown"
                }
            } ?: "Unknown"
        }
    }
}
