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

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import com.amplifyframework.core.Amplify
import com.amplifyframework.pushnotifications.pinpoint.utils.PushNotificationsConstants

class AWSPinpointPushNotificationsActivity : Activity() {

    @Override
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Amplify.Notifications.Push.recordNotificationOpened(emptyMap(), { }, { })
        val resultIntent = intent.extras
        @Suppress("UNCHECKED_CAST")
        val action = resultIntent?.get("action") as HashMap<String, String>
        val intent = processIntent(action)
        try {
            startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            Log.e("TAG", "Couldn't launch intent.", e)
        }
        finish()
    }

    private fun processIntent(action: HashMap<String, String>): Intent {
        // Action is open url
        val notificationIntent: Intent = if (action.containsKey(PushNotificationsConstants.PINPOINT_URL)) {
            val url = action[PushNotificationsConstants.PINPOINT_URL]
            Intent(Intent.ACTION_VIEW, Uri.parse(url))
        }
        // Action is open deeplink
        else if (action.containsKey(PushNotificationsConstants.PINPOINT_DEEPLINK)) {
            val deepLink = action[PushNotificationsConstants.PINPOINT_DEEPLINK]
            Intent(Intent.ACTION_VIEW, Uri.parse(deepLink))
        }
        // Default action is open app
        else {
            val packageName = applicationContext.packageName
            applicationContext.packageManager.getLaunchIntentForPackage(packageName)!!
        }
        return notificationIntent
    }
}
