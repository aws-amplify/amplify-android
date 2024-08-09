package com.amplifyframework.datastore.syncengine

import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.assertFalse
import org.junit.Test
import org.mockito.Mockito

class NetworkCapabilitiesUtilTest {

    @Test
    fun testGetNetworkCapabilitiesSecurityException() {
        val mockConnectivityManager = Mockito.mock(ConnectivityManager::class.java)
        Mockito.`when`(mockConnectivityManager.activeNetwork).thenThrow(SecurityException())

        val networkCapabilities = NetworkCapabilitiesUtil.getNetworkCapabilities(mockConnectivityManager)
        assertNull(networkCapabilities)
    }

    @Test
    fun testGetNetworkCapabilities() {
        val mockConnectivityManager = Mockito.mock(ConnectivityManager::class.java)
        val expectedNetworkCapabilities = Mockito.mock(NetworkCapabilities::class.java)
        Mockito.`when`(mockConnectivityManager.getNetworkCapabilities(mockConnectivityManager.activeNetwork))
            .thenReturn(expectedNetworkCapabilities)

        val networkCapabilities = NetworkCapabilitiesUtil.getNetworkCapabilities(mockConnectivityManager)

        assertEquals(expectedNetworkCapabilities, networkCapabilities)
    }

    @Test
    fun testIsInternetReachable() {
        val networkCapabilitiesWithCellular = Mockito.mock(NetworkCapabilities::class.java)
        Mockito.`when`(networkCapabilitiesWithCellular.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)).thenReturn(true)

        val networkCapabilitiesWithWifi = Mockito.mock(NetworkCapabilities::class.java)
        Mockito.`when`(networkCapabilitiesWithWifi.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)).thenReturn(true)

        val networkCapabilitiesWithEthernet = Mockito.mock(NetworkCapabilities::class.java)
        Mockito.`when`(networkCapabilitiesWithEthernet.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)).thenReturn(true)

        val networkCapabilitiesWithVpnAndBandwidth = Mockito.mock(NetworkCapabilities::class.java)
        Mockito.`when`(networkCapabilitiesWithVpnAndBandwidth.hasTransport(NetworkCapabilities.TRANSPORT_VPN)).thenReturn(true)
        Mockito.`when`(networkCapabilitiesWithVpnAndBandwidth.linkDownstreamBandwidthKbps).thenReturn(1000)

        val networkCapabilitiesWithVpnNoBandwidth = Mockito.mock(NetworkCapabilities::class.java)
        Mockito.`when`(networkCapabilitiesWithVpnNoBandwidth.hasTransport(NetworkCapabilities.TRANSPORT_VPN)).thenReturn(true)
        Mockito.`when`(networkCapabilitiesWithVpnNoBandwidth.linkDownstreamBandwidthKbps).thenReturn(0)

        assertTrue(NetworkCapabilitiesUtil.isInternetReachable(networkCapabilitiesWithCellular))
        assertTrue(NetworkCapabilitiesUtil.isInternetReachable(networkCapabilitiesWithWifi))
        assertTrue(NetworkCapabilitiesUtil.isInternetReachable(networkCapabilitiesWithEthernet))
        assertTrue(NetworkCapabilitiesUtil.isInternetReachable(networkCapabilitiesWithVpnAndBandwidth))
        assertFalse(NetworkCapabilitiesUtil.isInternetReachable(networkCapabilitiesWithVpnNoBandwidth))
        assertFalse(NetworkCapabilitiesUtil.isInternetReachable(null))
    }
}