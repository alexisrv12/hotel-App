package com.example.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import java.net.Inet4Address
import java.net.NetworkInterface
import java.util.Collections

object NetworkUtils {

    /**
     * Retrieves the device's local IPv4 address on the Wi-Fi or LAN network.
     * Returns fallback 127.0.0.1 if no active network interface is found.
     */
    fun getLocalIpAddress(context: Context? = null): String {
        try {
            // First check network interfaces for active non-loopback IPv4 (wlan0, eth0, etc.)
            val interfaces = Collections.list(NetworkInterface.getNetworkInterfaces())
            for (intf in interfaces) {
                if (!intf.isUp || intf.isLoopback) continue
                val addresses = Collections.list(intf.inetAddresses)
                for (addr in addresses) {
                    if (!addr.isLoopbackAddress && addr is Inet4Address) {
                        val hostAddress = addr.hostAddress ?: continue
                        if (hostAddress.startsWith("192.168.") ||
                            hostAddress.startsWith("10.") ||
                            hostAddress.startsWith("172.")
                        ) {
                            return hostAddress
                        }
                    }
                }
            }

            // Secondary check if specific subnet wasn't matched first
            for (intf in interfaces) {
                if (!intf.isUp || intf.isLoopback) continue
                val addresses = Collections.list(intf.inetAddresses)
                for (addr in addresses) {
                    if (!addr.isLoopbackAddress && addr is Inet4Address) {
                        val host = addr.hostAddress
                        if (!host.isNullOrEmpty() && host != "127.0.0.1") {
                            return host
                        }
                    }
                }
            }

            // Third fallback: WifiManager
            if (context != null) {
                val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
                val wifiInfo = wifiManager?.connectionInfo
                val ipInt = wifiInfo?.ipAddress ?: 0
                if (ipInt != 0) {
                    return String.format(
                        java.util.Locale.US,
                        "%d.%d.%d.%d",
                        ipInt and 0xff,
                        ipInt shr 8 and 0xff,
                        ipInt shr 16 and 0xff,
                        ipInt shr 24 and 0xff
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return "192.168.1.100" // Sensible display fallback
    }

    /**
     * Checks if device is currently connected to a Wi-Fi or Ethernet network.
     */
    fun isConnectedToLocalNetwork(context: Context): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return false
        val activeNetwork = cm.activeNetwork ?: return false
        val capabilities = cm.getNetworkCapabilities(activeNetwork) ?: return false
        return capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
    }
}
