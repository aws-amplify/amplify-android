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

package com.amplifyframework.pushnotifications.pinpoint

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import com.amplifyframework.core.Amplify
import com.amplifyframework.pushnotifications.pinpoint.utils.PushNotificationsConstants
import com.amplifyframework.pushnotifications.pinpoint.utils.toNotificationsPayload

class AWSPinpointPushNotificationsActivity : Activity() {

    private val TAG = "PushNotificationsActivity"

    @Override
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            val bundle = intent.extras?.get("payload") as Bundle
            val payload = bundle.toNotificationsPayload()
            val intent = processIntent(payload.action)
            Amplify.Notifications.Push.recordNotificationOpened(bundle, {
                Log.i(TAG, "Notification opened event recorded successfully.")
            }, {
                Log.i(TAG, "Record notification opened event failed.", it)
            })
            startActivity(intent)
        } catch (exception: ActivityNotFoundException) {
            Log.e(TAG, "Couldn't launch PushNotifications Activity.", exception)
        }
        finish()
    }

    private fun processIntent(action: HashMap<String, String>): Intent? {
        // Action is open url
        val notificationIntent: Intent? = if (action.containsKey(PushNotificationsConstants.URL)) {
            val url = action[PushNotificationsConstants.URL]
            Intent(Intent.ACTION_VIEW, Uri.parse(url))
        }
        // Action is open deeplink
        else if (action.containsKey(PushNotificationsConstants.DEEPLINK)) {
            val deepLink = action[PushNotificationsConstants.DEEPLINK]
            Intent(Intent.ACTION_VIEW, Uri.parse(deepLink))
        }
        // Default action is open app
        else {
            val packageName = applicationContext.packageName
            applicationContext.packageManager.getLaunchIntentForPackage(packageName)
        }
        return notificationIntent
    }
}
