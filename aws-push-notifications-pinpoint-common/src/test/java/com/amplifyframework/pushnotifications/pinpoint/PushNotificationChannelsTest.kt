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

import android.graphics.Color
import android.media.AudioAttributes
import android.net.Uri
import android.provider.Settings
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationChannelGroupCompat
import androidx.core.app.NotificationManagerCompat
import io.mockk.MockKAssertScope
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for the [PushNotificationChannels] class
 */
class PushNotificationChannelsTest {

    private val manager = mockk<NotificationManagerCompat>(relaxed = true)
    private val pushNotificationChannels = PushNotificationChannels(manager)

    @Test
    fun `creates a group`() {
        pushNotificationChannels.create {
            group("groupId", "groupName") {
            }
        }

        assertCreatedGroup { group ->
            assertEquals("groupId", group.id)
            assertEquals("groupName", group.name)
            assertNull(group.description)
        }
    }

    @Test
    fun `sets group description`() {
        pushNotificationChannels.create {
            group("groupId", "groupName") {
                description = "groupDescription"
            }
        }

        assertCreatedGroup { group ->
            assertEquals("groupDescription", group.description)
        }
    }

    @Test
    fun `creates a channel`() {
        pushNotificationChannels.create {
            channel("channelId", "channelName", NotificationImportance.Max)
        }

        assertCreatedChannel { channel ->
            assertEquals("channelId", channel.id)
            assertEquals("channelName", channel.name)
            assertEquals(NotificationImportance.Max.intValue, channel.importance)
            assertNull(channel.group)
        }
    }

    @Test
    fun `creates a channel with defaults`() {
        pushNotificationChannels.create {
            channel("channelId", "channelName")
        }

        assertCreatedChannel { channel ->
            assertEquals("channelId", channel.id)
            assertEquals("channelName", channel.name)
            assertEquals(NotificationImportance.Default.intValue, channel.importance)
            assertNull(channel.group)
            assertTrue(channel.canShowBadge())
            assertNull(channel.description)
            assertEquals(Settings.System.DEFAULT_NOTIFICATION_URI, channel.sound)
            assertNull(channel.audioAttributes)
            assertEquals(0, channel.lightColor)
            assertFalse(channel.shouldShowLights())
            assertNull(channel.vibrationPattern)
            assertFalse(channel.shouldVibrate())
        }
    }

    @Test
    fun `sets group id in a channel`() {
        pushNotificationChannels.create {
            group("groupId", "groupName") {
                channel("channelId", "channelName")
            }
        }

        assertCreatedChannel { channel ->
            assertEquals("channelId", channel.id)
            assertEquals("groupId", channel.group)
        }
    }

    @Test
    fun `sets channel description`() {
        pushNotificationChannels.create {
            channel("channelId", "channelName") {
                description = "channelDescription"
            }
        }

        assertCreatedChannel { channel ->
            assertEquals("channelDescription", channel.description)
        }
    }

    @Test
    fun `showBadge can be set to false`() {
        pushNotificationChannels.create {
            channel("channelId", "channelName") { showBadge = false }
        }

        assertCreatedChannel { channel ->
            assertFalse(channel.canShowBadge())
        }
    }

    @Test
    fun `sets sound and audio`() {
        val uri = mockk<Uri>()
        pushNotificationChannels.create {
            channel("channelId", "channelName") { sound = uri }
        }

        assertCreatedChannel { channel ->
            assertSame(uri, channel.sound)
        }
    }

    @Test
    fun `sets sound with audio attributes`() {
        val uri = mockk<Uri>()
        val attributes = mockk<AudioAttributes>()
        pushNotificationChannels.create {
            channel("channelId", "channelName") {
                sound = uri
                audioAttributes = attributes
            }
        }

        assertCreatedChannel { channel ->
            assertSame(uri, channel.sound)
            assertSame(attributes, channel.audioAttributes)
        }
    }

    @Test
    fun `sets light color`() {
        pushNotificationChannels.create {
            channel("channelId", "channelName") { lightColor = Color.CYAN }
        }

        assertCreatedChannel { channel ->
            assertEquals(Color.CYAN, channel.lightColor)
        }
    }

    @Test
    fun `enables lights`() {
        pushNotificationChannels.create {
            channel("channelId", "channelName") { lightsEnabled = true }
        }

        assertCreatedChannel { channel ->
            assertTrue(channel.shouldShowLights())
        }
    }

    @Test
    fun `sets vibration pattern`() {
        val pattern = LongArray(3)
        pushNotificationChannels.create {
            channel("channelId", "channelName") { vibrationPattern = pattern }
        }

        assertCreatedChannel { channel ->
            assertSame(pattern, channel.vibrationPattern)
        }
    }

    @Test
    fun `enables vibration`() {
        pushNotificationChannels.create {
            channel("channelId", "channelName") { vibrationEnabled = true }
        }

        assertCreatedChannel { channel ->
            assertTrue(channel.shouldVibrate())
        }
    }

    @Test
    fun `deletes a channel`() {
        pushNotificationChannels.deleteChannel("myChannelId")
        verify {
            manager.deleteNotificationChannel("myChannelId")
        }
    }

    @Test
    fun `deletes a group`() {
        pushNotificationChannels.deleteGroup("myChannelGroup")
        verify {
            manager.deleteNotificationChannelGroup("myChannelGroup")
        }
    }

    @Test
    fun `returns true if a channel exists`() {
        every { manager.getNotificationChannelCompat("myChannelId") } returns mockk()
        assertTrue(pushNotificationChannels.channelExists("myChannelId"))
    }

    @Test
    fun `returns false if a channel does not exist`() {
        every { manager.getNotificationChannelCompat("myChannelId") } returns null
        assertFalse(pushNotificationChannels.channelExists("myChannelId"))
    }

    @Test
    fun `returns true if a group exists`() {
        every { manager.getNotificationChannelGroupCompat("myGroupId") } returns mockk()
        assertTrue(pushNotificationChannels.channelExists("myGroupId"))
    }

    @Test
    fun `returns false if a group does not exist`() {
        every { manager.getNotificationChannelGroupCompat("myGroupId") } returns null
        assertFalse(pushNotificationChannels.groupExists("myGroupId"))
    }

    //region helpers
    private fun assertCreatedGroup(assertionBlock: MockKAssertScope.(NotificationChannelGroupCompat) -> Unit) = verify {
        manager.createNotificationChannelGroup(withArg(assertionBlock))
    }

    private fun assertCreatedChannel(assertionBlock: MockKAssertScope.(NotificationChannelCompat) -> Unit) = verify {
        manager.createNotificationChannel(withArg(assertionBlock))
    }
    //endregion
}
