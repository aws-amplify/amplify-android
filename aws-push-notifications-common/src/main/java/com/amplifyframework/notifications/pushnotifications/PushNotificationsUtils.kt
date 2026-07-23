/*
 * Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package com.amplifyframework.notifications.pushnotifications

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
import android.util.Log
import androidx.annotation.ChecksSdkIntAtLeast
import androidx.annotation.DrawableRes
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import java.net.URL
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Backend-agnostic helpers for displaying push notifications and querying notification state.
 *
 * @param context an Android [Context]
 * @param smallIconRes drawable resource in the consuming application used as the notification small
 *   icon. On API 26+ the small icon must be a monochrome silhouette; supplying an adaptive launcher
 *   icon here renders as a solid square in the status bar.
 * @param scope the [CoroutineScope] used to run the image download and post the notification. Callers
 *   should pass a lifecycle-bound scope (e.g. a service's scope) so in-flight work is cancelled when
 *   the owner is destroyed.
 * @param channelId the default notification channel id to use when a payload does not specify one
 */
class PushNotificationsUtils(
    private val context: Context,
    @DrawableRes private val smallIconRes: Int,
    private val scope: CoroutineScope,
    private val channelId: String = PushNotificationsConstants.DEFAULT_NOTIFICATION_CHANNEL_ID
) {
    companion object {
        private const val TAG = "PushNotificationsUtils"
    }

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
        val resolved = channel ?: createDefaultNotificationChannel(channelId)
        if (resolved == null) {
            // Not fatal below API 26 where channels do not exist; log so a genuine creation failure is
            // visible during integration testing rather than surfacing later as a missing notification.
            Log.d(TAG, "No notification channel available for id '$channelId'.")
        }
        return resolved
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
        runCatching {
            BitmapFactory.decodeStream(URL(url).openConnection().getInputStream())
        }.getOrNull()
    }

    /**
     * Returns true if the host application is currently in the foreground.
     */
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

    /**
     * Returns true if the user has notifications enabled for this application.
     */
    fun areNotificationsEnabled(): Boolean {
        // check for app level opt out
        return NotificationManagerCompat.from(context).areNotificationsEnabled()
    }

    /**
     * Display a system notification for the given [payload].
     *
     * If notifications are disabled for the app (including a missing POST_NOTIFICATIONS grant on
     * API 33+), this returns early with a log instead of silently dropping the notification.
     *
     * @param notificationId a stable id for the notification
     * @param payload the parsed notification content
     * @param targetClass the Activity to launch when the notification is tapped; falls back to the
     *   payload's own target class when provided
     */
    @Suppress("DEPRECATION")
    @SuppressLint("NewApi")
    fun showNotification(notificationId: Int, payload: PushNotificationPayload, targetClass: Class<*>?) {
        if (!areNotificationsEnabled()) {
            // On API 33+ notify() without POST_NOTIFICATIONS is dropped with no exception and no log,
            // which is hard to debug. Bail out explicitly so the caller can surface it.
            Log.w(TAG, "Notifications are disabled for this app; skipping notification $notificationId.")
            return
        }
        scope.launch {
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
                setSmallIcon(smallIconRes)
                setContentIntent(pendingIntent)
                setPriority(NotificationCompat.PRIORITY_DEFAULT)
                setLargeIcon(largeImageIcon)
                setAutoCancel(true)
                setStyle(NotificationCompat.BigTextStyle().bigText(payload.body))
            }

            with(NotificationManagerCompat.from(context)) {
                notify(notificationId, builder.build())
            }
        }
    }
}
