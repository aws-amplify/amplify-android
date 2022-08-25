package com.amplifyframework.pushnotifications.pinpoint.utils

import android.annotation.SuppressLint
import android.app.ActivityManager
import android.app.ActivityManager.RunningAppProcessInfo
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.annotation.ChecksSdkIntAtLeast
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.amplifyframework.notifications.pushnotifications.PushNotificationsDetails
import kotlin.random.Random


class AWSPinpointPushNotificationUtils(private val context: Context) {

    private val channel: NotificationChannel? by lazy {
        createNotificationChannel()
    }

    @ChecksSdkIntAtLeast(api = Build.VERSION_CODES.O)
    private val isNotificationChannelSupported = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O

    @SuppressLint("NewApi")
    private fun createNotificationChannel(): NotificationChannel? {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (isNotificationChannelSupported) {
            val name = "Default Channel"
            val descriptionText = "Default notification channel for all notifications"
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val defaultChannel = NotificationChannel("DEFAULT_CHANNEL", name, importance).apply {
                description = descriptionText
            }
            // Register the channel with the system
            val notificationManager: NotificationManager? =
                ContextCompat.getSystemService(context, NotificationManager::class.java)
            notificationManager?.createNotificationChannel(defaultChannel)
            return defaultChannel
        }
        return null
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

    @SuppressLint("NewApi")
    fun showNotification(details: PushNotificationsDetails) {
        val notificationId = Random.nextInt()
        val title = details.data[AWSPinpointPushNotificationsConstants.AWS_PINPOINT_NOTIFICATION_TITLE]
        val body = details.data[AWSPinpointPushNotificationsConstants.AWS_PINPOINT_NOTIFICATION_BODY]

        val notificationIntent = Intent(context, PinpointPushNotificationActivity::class.java).apply {
            flags = (Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }
        val intent = PendingIntent.getActivity(
            context,
            0,
            notificationIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = if (isNotificationChannelSupported && channel != null) {
            NotificationCompat.Builder(context, channel!!.id)
        } else {
            NotificationCompat.Builder(context)
        }

        builder.apply {
            setContentTitle(title)
            setContentText(body)
            setSmallIcon(R.drawable.ic_launcher_foreground)
            setContentIntent(intent)
            setPriority(NotificationCompat.PRIORITY_DEFAULT)
        }

        with(NotificationManagerCompat.from(context)) {
            // notificationId is a unique int for each notification that you must define
            notify(notificationId, builder.build())
        }
    }
}
