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
import android.os.Bundle
import androidx.core.net.toUri
import com.amplifyframework.core.Amplify
import com.amplifyframework.core.category.CategoryType
import com.amplifyframework.notifications.pushnotifications.NotificationPayload

internal class AWSPinpointPushNotificationsActivity : Activity() {

    companion object {
        private val LOG = Amplify.Logging.logger(CategoryType.NOTIFICATIONS, "amplify:aws-push-notifications-pinpoint")
    }

    @Override
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            val payload = NotificationPayload.fromIntent(intent)
            val intent = payload?.let { getIntentAction(it) }
            if (payload != null) {
                Amplify.Notifications.Push.recordNotificationOpened(payload, {
                    LOG.info("Notification opened event recorded successfully.")
                }, {
                    LOG.error("Record notification opened event failed.", it)
                })
            }
            startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            LOG.error("Couldn't launch PushNotifications Activity.", e)
        }
        finish()
    }

    private fun getIntentAction(payload: NotificationPayload): Intent? {
        val action = PinpointNotificationPayload.fromNotificationPayload(payload)?.action
        return when {
            action?.get(PushNotificationsConstants.URL) != null -> {
                // Action is open url
                action[PushNotificationsConstants.URL]?.toUri()?.let {
                    Intent(Intent.ACTION_VIEW, it)
                } ?: getDefaultTapAction()
            }
            action?.get(PushNotificationsConstants.DEEPLINK) != null -> {
                // Action is open deeplink
                action[PushNotificationsConstants.DEEPLINK]?.toUri()?.let {
                    Intent(Intent.ACTION_VIEW, it)
                } ?: getDefaultTapAction()
            }
            // Default action is open app
            else -> getDefaultTapAction()
        }
    }

    private fun getDefaultTapAction(): Intent? {
        val packageName = applicationContext.packageName
        return applicationContext.packageManager.getLaunchIntentForPackage(packageName)
    }
}
