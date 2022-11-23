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

package com.amplifyframework.pushnotifications.pinpoint.utils

import android.app.Activity
import android.os.Bundle

class PinpointPushNotificationActivity : Activity() {
    companion object {
        private val TAG = this::class.java.name
    }

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val intent = intent
        val extras = intent.extras
//        if (notificationClient != null) {
//            val eventSourceType: EventSourceType = EventSourceType.getEventSourceType(extras)
//            notificationClient.handleNotificationOpen(
//                eventSourceType.getAttributeParser().parseAttributes(extras),
//                extras
//            )
//        } else {
        val launchIntent = packageManager.getLaunchIntentForPackage(intent.getPackage()!!)
        launchIntent!!.putExtras(extras!!)
        startActivity(launchIntent)
//        }
        finish()
    }
}
