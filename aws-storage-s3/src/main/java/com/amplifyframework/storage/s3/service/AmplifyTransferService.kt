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
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.ServiceConnection
import android.net.ConnectivityManager
import android.os.Binder
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import androidx.annotation.RequiresApi
import androidx.annotation.VisibleForTesting
import androidx.core.app.NotificationCompat
import com.amazonaws.mobileconnectors.s3.transferutility.TransferNetworkLossHandler
import com.amazonaws.mobileconnectors.s3.transferutility.TransferStatusUpdaterAccessor
import com.amplifyframework.core.Amplify
import com.amplifyframework.storage.s3.R

internal class AmplifyTransferService : Service() {

    private val log = Amplify.Logging.forNamespace("amplify:aws-s3")

    private val binder = LocalBinder()

    /**
     * registers a BroadcastReceiver to receive network status change events. It
     * will update transfer records in database directly.
     */
    private var transferNetworkLossHandler: TransferNetworkLossHandler? = null

    /**
     * A flag indicates whether or not the receiver has is started the first time.
     */
    private var isReceiverNotRegistered = true

    /**
     * Handler to post Runnable check to unbind service if all transfers are complete
     */
    private var unbindCheckHandler: Handler? = null

    /**
     * Runnable that unbinds service if all transfers are complete
     */
    private var unbindCheckRunnable: Runnable? = null

    override fun onBind(intent: Intent?): IBinder {
        return binder
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
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    private fun startUnbindCheck() {
        unbindCheckHandler?.removeCallbacksAndMessages(null)
        unbindCheckRunnable = Runnable {
            log.verbose("AmplifyTransferService unbind check running")
            if (!TransferStatusUpdaterAccessor.hasActiveTransfer(applicationContext)) {
                try {
                    log.verbose("Removing AmplifyTransferService from foreground and unbinding")
                    stopForegroundAndUnbind(applicationContext)
                } catch (e: Exception) {
                    log.error("Error in moving the service out of the foreground state: $e")
                }
            } else {
                log.verbose("Transfers info progress, rescheduling unbind check")
                unbindCheckRunnable?.let {
                    unbindCheckHandler?.postDelayed(it, SHUTDOWN_CHECK_INTERVAL_MILLIS)
                }
            }
        }

        unbindCheckHandler = Handler(Looper.getMainLooper()).apply {
            unbindCheckRunnable?.let {
                postDelayed(it, SHUTDOWN_CHECK_INTERVAL_MILLIS)
            }
        }
    }

    private fun stopUnbindCheck() {
        log.info("Stopping AmplifyTransferService unbind check")
        unbindCheckHandler?.removeCallbacksAndMessages(null)
        unbindCheckRunnable = null
        unbindCheckHandler = null
    }

    override fun onDestroy() {
        try {
            stopUnbindCheck()
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

    inner class LocalBinder : Binder() {
        fun getService(): AmplifyTransferService {
            return this@AmplifyTransferService
        }
    }

    internal companion object {
        private const val NOTIFICATION_ID = 9382
        private const val SHUTDOWN_CHECK_INTERVAL_MILLIS = 8_000L

        private var boundService: AmplifyTransferService? = null
        private var boundServiceConnection: ServiceConnection? = null
        private var notification: Notification? = null

        fun bind(context: Context) {
            if (boundServiceConnection == null) {
                boundServiceConnection = object : ServiceConnection {
                    override fun onServiceConnected(name: ComponentName, service: IBinder?) {
                        val binder = service as AmplifyTransferService.LocalBinder
                        boundService = binder.getService()
                        startForeground(context)
                    }

                    override fun onServiceDisconnected(name: ComponentName?) {
                        stopForegroundAndUnbind(context)
                        boundService = null
                    }
                }
            }

            boundServiceConnection?.let {
                context.bindService(Intent(context, AmplifyTransferService::class.java), it, Context.BIND_AUTO_CREATE)
            }

            // A new call to to bind will restart counter, removing potential to unbind service too early
            boundService?.startUnbindCheck()
        }

        fun startForeground(context: Context) {
            if (!isNotificationShowing()) {
                val notification = createDefaultNotification(context)
                boundService?.startForeground(NOTIFICATION_ID, notification)
                this.notification = notification
            }
        }

        @JvmStatic
        fun stopForegroundAndUnbind(context: Context) {
            boundService?.stopForeground(true)
            boundServiceConnection?.let { context.unbindService(it) }
            boundServiceConnection = null
            notification = null
        }

        @VisibleForTesting
        fun isNotificationShowing(): Boolean {
            return notification != null
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
    }
}
