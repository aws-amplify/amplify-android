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