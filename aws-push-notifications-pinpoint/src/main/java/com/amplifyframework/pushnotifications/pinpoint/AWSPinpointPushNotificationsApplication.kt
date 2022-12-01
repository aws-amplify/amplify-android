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

package com.amplifyframework.pushnotifications.pinpoint

import android.app.Application
import android.util.Log
import com.amplifyframework.core.Amplify
import com.google.firebase.messaging.FirebaseMessaging

class AWSPinpointPushNotificationsApplication : Application() {
    private val TAG: String = AWSPinpointPushNotificationsApplication::class.java.simpleName

    override fun onCreate() {
        super.onCreate()
        registerDevice()
    }

    private fun registerDevice() {
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (!task.isSuccessful) {
                Log.w(TAG, "Fetching FCM registration token failed", task.exception)
            }
            val token = task.result
            Amplify.Notifications.Push.registerDevice(token)
            Log.d(TAG, "Registering push notifications token: $token")
        }
    }
}
