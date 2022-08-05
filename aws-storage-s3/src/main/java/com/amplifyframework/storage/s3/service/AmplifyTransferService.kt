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

package com.amplifyframework.storage.s3.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import com.amazonaws.mobileconnectors.s3.transferutility.TransferNetworkLossHandler
import com.amazonaws.mobileconnectors.s3.transferutility.TransferStatusUpdaterAccessor
import com.amplifyframework.core.Amplify
import com.amplifyframework.storage.s3.R
import java.util.concurrent.atomic.AtomicInteger

internal class AmplifyTransferService : Service() {

    private val log = Amplify.Logging.forNamespace("amplify:aws-s3")

    /**
     * registers a BroadcastReceiver to receive network status change events. It
     * will update transfer records in database directly.
     */
    private var transferNetworkLossHandler: TransferNetworkLossHandler? = null

    /**
     * A flag indicates whether or not the receiver has is started the first time.
     */
    private var isReceiverNotRegistered = true

    private var shutdownCheckHandler: Handler? = null
    private var shutdownCheckRunnable: Runnable? = null

    override fun onBind(intent: Intent?): IBinder? {
        throw UnsupportedOperationException("Can't bind to TransferService")
    }

    override fun onCreate() {
        super.onCreate()

        transferNetworkLossHandler = TransferNetworkLossHandler.getInstance(applicationContext)

        synchronized(this) {
            if (isReceiverNotRegistered) {
                try {
                    log.info("Registering the network receiver")
                    this.registerReceiver(
                        transferNetworkLossHandler,
                        IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION)
                    )
                    isReceiverNotRegistered = false
                } catch (iae: IllegalArgumentException) {
                    log.warn(
                        "Ignoring the exception trying to register the receiver for connectivity change."
                    )
                } catch (ise: IllegalStateException) {
                    log.warn("Ignoring the leak in registering the receiver.")
                }
            }
        }

        startForegroundNotificationIfRequired()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForegroundNotificationIfRequired()

        synchronized(this) {
            if (isReceiverNotRegistered) {
                try {
                    log.info("Registering the network receiver")
                    this.registerReceiver(
                        transferNetworkLossHandler,
                        IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION)
                    )
                    isReceiverNotRegistered = false
                } catch (iae: java.lang.IllegalArgumentException) {
                    log.warn(
                        "Ignoring the exception trying to register the receiver for connectivity change."
                    )
                } catch (ise: java.lang.IllegalStateException) {
                    log.warn("Ignoring the leak in registering the receiver.")
                }
            }
        }

        shutdownCheckHandler?.removeCallbacksAndMessages(null)
        shutdownCheckRunnable = Runnable {
            // If there are no startForegroundService calls waiting for startService, and all transfers are completed
            // or paused, then we are safe to stopForeground and kill the service
            synchronized(pendingStartForegroundCount) {
                log.verbose("AmplifyTransferService shutdown check running")
                if (pendingStartForegroundCount.get() == 0 &&
                    !TransferStatusUpdaterAccessor.hasActiveTransfer(applicationContext)
                ) {
                    try {
                        log.verbose("Shutting down AmplifyTransferService")
                        stopForeground(true)
                        stopSelf()
                    } catch (e: Exception) {
                        log.error("Error in moving the service out of the foreground state: $e")
                    }
                } else {
                    val pendingStartForegroundCount = pendingStartForegroundCount.get()
                    if (pendingStartForegroundCount > 0) {
                        log.verbose("Pending calls to startForeground, rescheduling shutdown check")
                    } else {
                        log.verbose("Transfers info progress, rescheduling shutdown check")
                    }
                    shutdownCheckRunnable?.let {
                        shutdownCheckHandler?.postDelayed(it, SHUTDOWN_CHECK_INTERVAL_MILLIS)
                    }
                }
            }
        }

        shutdownCheckHandler = Handler(Looper.getMainLooper()).apply {
            shutdownCheckRunnable?.let {
                postDelayed(it, SHUTDOWN_CHECK_INTERVAL_MILLIS)
            }
        }
        return START_STICKY
    }

    override fun onDestroy() {
        try {
            log.info("De-registering the network receiver.")
            synchronized(this) {
                if (!isReceiverNotRegistered) {
                    unregisterReceiver(transferNetworkLossHandler)
                    isReceiverNotRegistered = true
                    transferNetworkLossHandler = null
                }
            }
        } catch (iae: IllegalArgumentException) {
            /*
             * Ignore on purpose, just in case the service stops before
             * onStartCommand where the receiver is registered.
             */
            log.warn("Exception trying to de-register the network receiver")
        }
        super.onDestroy()
    }

    private fun startForegroundNotificationIfRequired() {
        try {
            synchronized(pendingStartForegroundCount) {
                log.verbose("Starting AmplifyTransferService in Foreground")
                startForeground(NOTIFICATION_ID, createDefaultNotification(applicationContext))
                if (pendingStartForegroundCount.get() > 0) {
                    pendingStartForegroundCount.decrementAndGet()
                }
            }
        } catch (ex: Exception) {
            log.error("Error in moving the service to foreground state: $ex")
        }
    }

    private fun createDefaultNotification(context: Context): Notification? {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createChannel(context)
        }
        val appIcon: Int = R.drawable.amplify_storage_transfer_notification_icon
        return NotificationCompat.Builder(
            context,
            context.getString(R.string.amplify_storage_notification_channel_id)
        )
            .setSmallIcon(appIcon)
            .setContentTitle(context.getString(R.string.amplify_storage_notification_title))
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private fun createChannel(context: Context) {
        val notificationManager = context.getSystemService(NOTIFICATION_SERVICE)
            as NotificationManager
        notificationManager.createNotificationChannel(
            NotificationChannel(
                context.getString(R.string.amplify_storage_notification_channel_id),
                context.getString(R.string.amplify_storage_notification_channel_name),
                NotificationManager.IMPORTANCE_LOW
            )
        )
    }

    internal companion object {
        const val NOTIFICATION_ID = 9382
        const val SHUTDOWN_CHECK_INTERVAL_MILLIS = 8_000L

        val pendingStartForegroundCount = AtomicInteger(0)

        /**
         * If Android SDK 26 (Oreo) or above, we must start as ForegroundService to start service from the background.
         * This will no longer work in Android SDK 31
         */
        fun start(context: Context) {
            synchronized(pendingStartForegroundCount) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    val serviceIntent = Intent(context, AmplifyTransferService::class.java)
                    pendingStartForegroundCount.incrementAndGet()
                    context.startForegroundService(serviceIntent)
                } else {
                    context.startService(Intent(context, AmplifyTransferService::class.java))
                }
            }
        }
    }
}
