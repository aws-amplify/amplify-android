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

import android.annotation.SuppressLint
import android.app.ActivityManager
import android.app.ActivityManager.RunningAppProcessInfo
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import androidx.annotation.ChecksSdkIntAtLeast
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.amplifyframework.pushnotifications.pinpoint.common.R
import java.net.URL
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PushNotificationsUtils(
    private val context: Context,
    private val channelId: String = PushNotificationsConstants.DEFAULT_NOTIFICATION_CHANNEL_ID
) {
    init {
        retrieveNotificationChannel()
    }

    @ChecksSdkIntAtLeast(api = Build.VERSION_CODES.O)
    private fun isNotificationChannelSupported() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O

    private fun retrieveNotificationChannel(): NotificationChannel? {
        var channel: NotificationChannel? = null
        val notificationManager = ContextCompat.getSystemService(context, NotificationManager::class.java)
        if (isNotificationChannelSupported()) {
            channel = notificationManager?.getNotificationChannel(channelId)
        }
        return channel ?: createDefaultNotificationChannel(channelId)
    }

    // create before notification trigger for API 32 or lower
    @SuppressLint("NewApi")
    private fun createDefaultNotificationChannel(channelId: String): NotificationChannel? {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (isNotificationChannelSupported()) {
            val notificationManager = ContextCompat.getSystemService(context, NotificationManager::class.java)
            val defaultChannel = NotificationChannel(
                channelId,
                "Default channel",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            // Register the channel with the system
            notificationManager?.createNotificationChannel(defaultChannel)
            return defaultChannel
        }
        return null
    }

    private suspend fun downloadImage(url: String): Bitmap? = withContext(Dispatchers.IO) {
        BitmapFactory.decodeStream(URL(url).openConnection().getInputStream())
    }

    fun isAppInForeground(): Boolean {
        // Gets a list of running processes.
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val processes = am.runningAppProcesses

        // On some versions of android the first item in the list is what runs in the foreground, but this is not true
        // on all versions. Check the process importance to see if the app is in the foreground.
        val packageName = context.applicationContext.packageName
        for (appProcess in processes) {
            val processName = appProcess.processName
            if (RunningAppProcessInfo.IMPORTANCE_FOREGROUND == appProcess.importance && packageName == processName) {
                return true
            }
        }
        return false
    }

    fun areNotificationsEnabled(): Boolean {
        // check for app level opt out
        return NotificationManagerCompat.from(context).areNotificationsEnabled()
    }

    @Suppress("DEPRECATION")
    @SuppressLint("NewApi")
    fun showNotification(
        notificationId: Int,
        payload: PinpointNotificationPayload,
        targetClass: Class<*>?
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            val largeImageIcon = payload.imageUrl?.let { downloadImage(it) }
            val notificationIntent = Intent(context, payload.targetClass ?: targetClass)
            notificationIntent.putExtra("amplifyNotificationPayload", payload)
            notificationIntent.putExtra("notificationId", notificationId)
            val pendingIntent = PendingIntent.getActivity(
                context,
                notificationId,
                notificationIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            val notificationChannel = retrieveNotificationChannel()
            val builder = if (isNotificationChannelSupported() && notificationChannel != null) {
                NotificationCompat.Builder(context, payload.channelId ?: notificationChannel.id)
            } else {
                NotificationCompat.Builder(context)
            }

            builder.apply {
                setContentTitle(payload.title)
                setContentText(payload.body)
                setSmallIcon(R.drawable.ic_launcher_foreground)
                setContentIntent(pendingIntent)
                setPriority(NotificationCompat.PRIORITY_DEFAULT)
                setLargeIcon(largeImageIcon)
                setAutoCancel(true)
            }

            with(NotificationManagerCompat.from(context)) {
                notify(notificationId, builder.build())
            }
        }
    }
}
