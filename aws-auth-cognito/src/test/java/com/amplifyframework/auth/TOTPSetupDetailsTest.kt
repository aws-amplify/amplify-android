package com.amplifyframework.auth

import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class TOTPSetupDetailsTest {

    @Test
    fun getSetupURI() {
        val ss = "SS123"
        val username = "User123"
        val appName = "MyApp"
        val expectedSetupURI = "otpauth://totp/MyApp:User123?secret=SS123&issuer=MyApp"

        val actual = TOTPSetupDetails(ss, username).getSetupURI(appName)

        assertEquals(expectedSetupURI, actual.toString())
    }

    @Test
    fun getSetupURIWithAccountNameOverride() {
        val ss = "SS123"
        val username = "User123"
        val accountNameOverride = "AccountOverride"
        val appName = "MyApp"
        val expectedSetupURI = "otpauth://totp/MyApp:AccountOverride?secret=SS123&issuer=MyApp"

        val actual = TOTPSetupDetails(ss, username).getSetupURI(appName, accountNameOverride)

        assertEquals(expectedSetupURI, actual.toString())
    }
}
