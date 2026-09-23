package com.amplifyframework.datastore.extensions

import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.assertFalse
import org.junit.Test

class NetworkCapabilitiesUtilTest {

    @Test
    fun testGetNetworkCapabilitiesSecurityException() {
        val mockConnectivityManager = mockk<ConnectivityManager> {
            every { activeNetwork } throws SecurityException()
        }

        val networkCapabilities = mockConnectivityManager.networkCapabilitiesOrNull()
        assertNull(networkCapabilities)
    }

    @Test
    fun testGetNetworkCapabilities() {
        val expectedNetworkCapabilities = mockk<NetworkCapabilities>()
        val mockConnectivityManager = mockk<ConnectivityManager> {
            every { getNetworkCapabilities(any()) } returns expectedNetworkCapabilities
        }

        val networkCapabilities = mockConnectivityManager.networkCapabilitiesOrNull()

        assertEquals(expectedNetworkCapabilities, networkCapabilities)
    }

    @Test
    fun testIsInternetReachable() {
        val networkCapabilitiesWithCellular = mockk<NetworkCapabilities> {
            every { hasTransport(NetworkCapabilities.TRANSPORT_WIFI) } returns false
            every { hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) } returns true
            every { hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) } returns false
        }

        val networkCapabilitiesWithWifi = mockk<NetworkCapabilities> {
            every { hasTransport(NetworkCapabilities.TRANSPORT_WIFI) } returns true
            every { hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) } returns false
            every { hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) } returns false
        }

        val networkCapabilitiesWithEthernet = mockk<NetworkCapabilities> {
            every { hasTransport(NetworkCapabilities.TRANSPORT_WIFI) } returns false
            every { hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) } returns false
            every { hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) } returns true
        }

        assertTrue(networkCapabilitiesWithCellular.isInternetReachable())
        assertTrue(networkCapabilitiesWithWifi.isInternetReachable())
        assertTrue(networkCapabilitiesWithEthernet.isInternetReachable())
        assertFalse(null.isInternetReachable())
    }
}