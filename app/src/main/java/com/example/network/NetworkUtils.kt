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
    // Cellular/Mobile-data interface prefixes that must NEVER be used for local P2P
    private val CELLULAR_INTERFACE_PREFIXES = listOf(
        "rmnet", "ccmni", "pdp", "wwan", "radio", "dummy", "sit", "ip_vti", "seth", "cellular", "mobile"
    )

    /**
     * Finds the best active local IPv4 address (Wi-Fi, Hotspot AP, or Ethernet).
     * STRICTLY excludes cellular / mobile data interfaces (rmnet, ccmni, etc.)
     * so that local communication NEVER routes over the cellular network or consumes data!
     */
    fun getLocalIpAddress(context: Context): String {
        try {
            val interfaces = Collections.list(NetworkInterface.getNetworkInterfaces())

            var hotspotIp: String? = null
            var wifiIp: String? = null
            var otherLocalIp: String? = null

            for (intf in interfaces) {
                if (intf.isLoopback || !intf.isUp) continue
                val name = intf.name.lowercase()

                // STRICT: Skip any cellular / mobile data interface!
                if (CELLULAR_INTERFACE_PREFIXES.any { name.startsWith(it) }) {
                    continue
                }

                val isApInterface = name.startsWith("ap") || name.startsWith("softap") || name == "wlan1"
                val isWifiInterface = name.startsWith("wlan") || name.startsWith("wifi") || name.startsWith("swlan")
                val isEthernet = name.startsWith("eth") || name.startsWith("rndis")

                val addrs = Collections.list(intf.inetAddresses)
                for (addr in addrs) {
                    if (!addr.isLoopbackAddress && addr is Inet4Address) {
                        val host = addr.hostAddress ?: ""
                        if (host.isNotEmpty() && !host.startsWith("127.")) {
                            // Check if this IP matches standard Android hotspot IP ranges
                            val isHotspotRange = host == "192.168.43.1" || host == "192.168.44.1" ||
                                    host == "192.168.49.1" || host == "192.168.50.1" || host.startsWith("192.168.43.")

                            if (isApInterface || isHotspotRange) {
                                if (hotspotIp == null) hotspotIp = host
                            } else if (isWifiInterface) {
                                if (wifiIp == null) wifiIp = host
                            } else if (isEthernet) {
                                if (otherLocalIp == null) otherLocalIp = host
                            } else if (!isCellularIp(host)) {
                                if (otherLocalIp == null) otherLocalIp = host
                            }
                        }
                    }
                }
            }

            // Return by priority: Hotspot AP > Wi-Fi station > Ethernet/Other
            hotspotIp?.let { return it }
            wifiIp?.let { return it }
            otherLocalIp?.let { return it }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return "127.0.0.1"
    }

    private fun isCellularIp(ip: String): Boolean {
        // CGNAT (Carrier-Grade NAT) addresses: 100.64.0.0 - 100.127.255.255
        if (ip.startsWith("100.")) {
            val secondOctet = ip.substringAfter('.').substringBefore('.').toIntOrNull() ?: 0
            if (secondOctet in 64..127) return true
        }
        // Public IPs that are clearly not private RFC1918 (192.168, 10, 172.16-31)
        if (!ip.startsWith("192.168.") && !ip.startsWith("10.") && !ip.startsWith("172.")) {
            return true
        }
        return false
    }

    /**
     * Determines the best broadcast address for LAN peer discovery.
     * Only uses Wi-Fi and Hotspot network interfaces, NEVER cellular!
     */
    fun getBroadcastAddress(context: Context): InetAddress {
        try {
            val interfaces = NetworkInterface.getNetworkInterfaces()
            while (interfaces.hasMoreElements()) {
                val ni = interfaces.nextElement()
                if (ni.isLoopback || !ni.isUp) continue
                val name = ni.name.lowercase()
                // Never broadcast across cellular data interfaces
                if (CELLULAR_INTERFACE_PREFIXES.any { name.startsWith(it) }) continue

                for (ia in ni.interfaceAddresses) {
                    ia.broadcast?.let { return it }
                }
            }
        } catch (_: Exception) {}

        // Fallback to deprecated method for older devices (Wi-Fi only)
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
     * Checks if Mobile Data (Cellular internet) is active on this device.
     */
    fun isMobileDataActive(context: Context): Boolean {
        return try {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return false
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val network = cm.activeNetwork ?: return false
                val caps = cm.getNetworkCapabilities(network) ?: return false
                caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)
            } else {
                @Suppress("DEPRECATION")
                val netInfo = cm.activeNetworkInfo
                netInfo?.type == ConnectivityManager.TYPE_MOBILE
            }
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Checks if the device is actively hosting a Wi-Fi Hotspot (AP).
     */
    fun isHotspotApActive(): Boolean {
        return try {
            val interfaces = Collections.list(NetworkInterface.getNetworkInterfaces())
            for (intf in interfaces) {
                if (!intf.isUp || intf.isLoopback) continue
                val name = intf.name.lowercase()
                if (name.startsWith("ap") || name.startsWith("softap") || name == "wlan1") {
                    return true
                }
                for (addr in intf.inetAddresses) {
                    if (addr is Inet4Address && (addr.hostAddress == "192.168.43.1" || addr.hostAddress == "192.168.44.1" || addr.hostAddress == "192.168.49.1")) {
                        return true
                    }
                }
            }
            false
        } catch (_: Exception) {
            false
        }
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
                isHotspotApActive() ||
                getLocalIpAddress(context) != "127.0.0.1"
    }
}
