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

import android.util.Log
import androidx.test.core.app.ApplicationProvider
import com.amplifyframework.AmplifyException
import com.amplifyframework.auth.cognito.AWSCognitoAuthPlugin
import com.amplifyframework.core.Amplify
import com.amplifyframework.notifications.pushnotifications.NotificationContentProvider
import com.amplifyframework.notifications.pushnotifications.NotificationPayload
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import org.junit.Assert
import org.junit.Assert.fail
import org.junit.BeforeClass
import org.junit.Test

class NotificationsCanaryTest {
//    companion object {
//        private const val TIMEOUT_S = 20L
//
//        @BeforeClass
//        @JvmStatic
//        fun setup() {
//            try {
//                Amplify.addPlugin(AWSCognitoAuthPlugin())
//                Amplify.addPlugin(AWSPinpointPushNotificationsPlugin())
//                Amplify.configure(ApplicationProvider.getApplicationContext())
//                Log.i("NotificationsCanaryTest", "Initialized Amplify")
//            } catch (error: AmplifyException) {
//                Log.e("NotificationsCanaryTest", "Could not initialize Amplify", error)
//            }
//        }
//    }
//
//    @Test
//    fun identifyUser() {
//        val latch = CountDownLatch(1)
//        var userId = ""
//        Amplify.Auth.getCurrentUser(
//            { authUser -> userId = authUser.userId },
//            { Log.e("NotificationsCanaryTest", "Error getting current user", it) }
//        )
//
//        try {
//            Amplify.Notifications.Push.identifyUser(
//                userId,
//                {
//                    Log.i("NotificationsCanaryTest", "Identified user successfully")
//                    latch.countDown()
//                },
//                {
//                    Log.e("NotificationsCanaryTest", "Error identifying user", it)
//                    fail()
//                }
//            )
//        } catch (e: Exception) {
//            fail(e.toString())
//        }
//        Assert.assertTrue(latch.await(TIMEOUT_S, TimeUnit.SECONDS))
//    }
//
//    @Test
//    fun registerDevice() {
//        val latch = CountDownLatch(1)
//        try {
//            Amplify.Notifications.Push.registerDevice(
//                "token",
//                {
//                    Log.i("NotificationsCanaryTest", "Registered device with token")
//                    latch.countDown()
//                },
//                {
//                    Log.e("NotificationsCanaryTest", "Failed to register device", it)
//                    fail()
//                }
//            )
//        } catch (error: Exception) {
//            Log.e("NotificationsCanaryTest", error.toString())
//        }
//        Assert.assertTrue(latch.await(TIMEOUT_S, TimeUnit.SECONDS))
//    }
//
//    @Test
//    fun recordNotificationReceived() {
//        val latch = CountDownLatch(1)
//        val payload = NotificationPayload(NotificationContentProvider.FCM(mapOf()))
//        try {
//            Amplify.Notifications.Push.recordNotificationReceived(
//                payload,
//                {
//                    Log.i("NotificationsCanaryTest", "Successfully registered notification received")
//                    latch.countDown()
//                },
//                {
//                    Log.e("NotificationsCanaryTest", "Failed to register notification received", it)
//                    fail()
//                }
//            )
//        } catch (error: Exception) {
//            Log.e("NotificationsCanaryTest", error.toString())
//        }
//        Assert.assertTrue(latch.await(TIMEOUT_S, TimeUnit.SECONDS))
//    }
//
//    @Test
//    fun recordNotificationOpened() {
//        val latch = CountDownLatch(1)
//        val payload = NotificationPayload(NotificationContentProvider.FCM(mapOf()))
//        try {
//            Amplify.Notifications.Push.recordNotificationOpened(
//                payload, {
//                    Log.i("NotificationsCanaryTest", "Successfully registered notification opened")
//                    latch.countDown()
//                },
//                {
//                    Log.e("NotificationsCanaryTest", "Failed to register notification opened", it)
//                    fail()
//                }
//            )
//        } catch (error: Exception) {
//            Log.e("NotificationsCanaryTest", error.toString())
//        }
//        Assert.assertTrue(latch.await(TIMEOUT_S, TimeUnit.SECONDS))
//    }
//
//    @Test
//    fun handleNotificationReceived() {
//        val latch = CountDownLatch(1)
//        val payload = NotificationPayload(NotificationContentProvider.FCM(mapOf()))
//        try {
//            Amplify.Notifications.Push.handleNotificationReceived(
//                payload,
//                {
//                    Log.i("NotificationsCanaryTest", "Successfully handled notification")
//                    latch.countDown()
//                },
//                {
//                    Log.e("NotificationsCanaryTest", "Failed to handle notification", it)
//                    fail()
//                }
//            )
//        } catch (error: Exception) {
//            Log.e("NotificationsCanaryTest", error.toString())
//        }
//        Assert.assertTrue(latch.await(TIMEOUT_S, TimeUnit.SECONDS))
//    }
}
