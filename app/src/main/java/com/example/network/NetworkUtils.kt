package com.example.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import android.os.Build
import java.net.Inet4Address
import java.net.InetAddress
import java.net.NetworkInterface
import java.util.Collections

object NetworkUtils {

    const val DISCOVERY_PORT = 8888
    const val AUDIO_PORT = 8889
    const val VIDEO_PORT = 8890
    const val FILE_PORT = 8891
    const val MULTICAST_GROUP_IP = "239.255.42.99"

    /**
     * Formats bytes to human-readable format (KB, MB, GB).
     */
    fun formatFileSize(bytes: Long): String {
        if (bytes <= 0) return "0 B"
        val kb = bytes / 1024.0
        val mb = kb / 1024.0
        val gb = mb / 1024.0
        return when {
            gb >= 1.0 -> String.format(java.util.Locale.US, "%.1f GB", gb)
            mb >= 1.0 -> String.format(java.util.Locale.US, "%.1f MB", mb)
            kb >= 1.0 -> String.format(java.util.Locale.US, "%.1f KB", kb)
            else -> "$bytes B"
        }
    }

    /**
     * Finds the best active local IPv4 address (Wi-Fi, Hotspot AP, or Ethernet).
     */
    fun getLocalIpAddress(context: Context): String {
        try {
            val interfaces = Collections.list(NetworkInterface.getNetworkInterfaces())
            // Priority: wlan0, ap0, rndis0, eth0
            for (intf in interfaces) {
                if (intf.isLoopback || !intf.isUp) continue
                val addrs = Collections.list(intf.inetAddresses)
                for (addr in addrs) {
                    if (!addr.isLoopbackAddress && addr is Inet4Address) {
                        val host = addr.hostAddress ?: ""
                        if (host.isNotEmpty() && !host.startsWith("127.")) {
                            return host
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return "127.0.0.1"
    }

    /**
     * Determines the best broadcast address for LAN peer discovery.
     *
     * Priority 1 — Subnet-directed broadcast collected from LIVE network interfaces
     * (wlan0, hotspot ap0, rndis0/usb tethering, eth0). This is the only reliable
     * path when the phone acts as a Hotspot router: dhcpInfo is null/0 in AP mode
     * and the kernel usually drops global 255.255.255.255 datagrams.
     * Priority 2 — DHCP-derived broadcast (classic DHCP-client Wi-Fi).
     * Priority 3 — Global broadcast fallback.
     */
    // For API 31+, prefer NetworkInterface-based approach
    fun getBroadcastAddress(context: Context): InetAddress {
        // Try NetworkInterface first (works on all versions)
        try {
            val interfaces = NetworkInterface.getNetworkInterfaces()
            while (interfaces.hasMoreElements()) {
                val ni = interfaces.nextElement()
                if (ni.isLoopback || !ni.isUp) continue
                for (ia in ni.interfaceAddresses) {
                    ia.broadcast?.let { return it }
                }
            }
        } catch (_: Exception) {}

        // Fallback to deprecated method for older devices
        try {
            val wifi = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
            val dhcp = wifi?.dhcpInfo
            if (dhcp != null && dhcp.ipAddress != 0) {
                val broadcast = (dhcp.ipAddress and dhcp.netmask) or dhcp.netmask.inv()
                val quads = ByteArray(4)
                for (k in 0..3) {
                    quads[k] = ((broadcast shr (k * 8)) and 0xFF).toByte()
                }
                return InetAddress.getByAddress(quads)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return InetAddress.getByName("255.255.255.255")
    }

    /**
     * Acquires a Wi-Fi Multicast Lock to enable receiving UDP multicast packets.
     */
    fun acquireMulticastLock(context: Context): WifiManager.MulticastLock? {
        return try {
            val wifi = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
            val lock = wifi?.createMulticastLock("LocalConnectMulticastLock")
            lock?.setReferenceCounted(true)
            lock?.acquire()
            lock
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Releases Wi-Fi Multicast Lock safely.
     */
    fun releaseMulticastLock(lock: WifiManager.MulticastLock?) {
        try {
            if (lock?.isHeld == true) {
                lock.release()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Checks if Wi-Fi or Hotspot is connected.
     */
    fun isLocalNetworkActive(context: Context): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return false
        val network = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(network) ?: return false
        return caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) ||
                getLocalIpAddress(context) != "127.0.0.1"
    }
}
