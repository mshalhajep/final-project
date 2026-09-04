package com.example.network

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.os.Build
import android.util.Log
import com.example.model.Peer
import com.example.model.UserPresenceStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.net.InetAddress

class NetworkServiceDiscoveryEngine(private val context: Context) {

    companion object {
        private const val TAG = "NSDEngine"
        private const val SERVICE_TYPE = "_localconnect._tcp."
        private const val SERVICE_NAME_PREFIX = "LocalConnect_"
    }

    private val nsdManager = context.getSystemService(Context.NSD_SERVICE) as? NsdManager
    private val scope = CoroutineScope(Dispatchers.IO)

    private var registrationListener: NsdManager.RegistrationListener? = null
    private var discoveryListener: NsdManager.DiscoveryListener? = null

    private val _discoveredMdnsPeers = MutableStateFlow<List<Peer>>(emptyList())
    val discoveredMdnsPeers = _discoveredMdnsPeers.asStateFlow()

    private val activeResolvedServices = mutableMapOf<String, NsdServiceInfo>()
    private var isRegistered = false
    private var isDiscovering = false

    // NsdManager resolves ONE service at a time platform-wide; concurrent resolve
    // calls fail with FAILURE_ALREADY_ACTIVE. Serialize through a FIFO queue.
    private val resolveQueue = java.util.concurrent.ConcurrentLinkedQueue<NsdServiceInfo>()
    private val isResolveInFlight = java.util.concurrent.atomic.AtomicBoolean(false)

    private var localServiceName = ""

    fun startDiscoveryAndRegistration(peerId: String, username: String, port: Int = NetworkUtils.DISCOVERY_PORT) {
        if (nsdManager == null) {
            Log.w(TAG, "NsdManager not available on this device/context.")
            return
        }

        localServiceName = "$SERVICE_NAME_PREFIX${Build.MODEL}_$peerId"

        // 1. Register local service
        registerService(port, peerId, username)

        // 2. Start discovering other services
        startDiscovery()
    }

    private fun registerService(port: Int, peerId: String, username: String) {
        if (isRegistered || nsdManager == null) return

        val serviceInfo = NsdServiceInfo().apply {
            serviceName = localServiceName
            serviceType = SERVICE_TYPE
            setPort(port)
            setAttribute("peer_id", peerId)
            // Use hashed username so it's not cleartext
            setAttribute("username", username.hashCode().toString())
        }

        registrationListener = object : NsdManager.RegistrationListener {
            override fun onServiceRegistered(NsdServiceInfo: NsdServiceInfo) {
                isRegistered = true
                Log.d(TAG, "Service registered successfully: ${NsdServiceInfo.serviceName}")
            }

            override fun onRegistrationFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
                isRegistered = false
                Log.e(TAG, "Service registration failed: Error code $errorCode")
            }

            override fun onServiceUnregistered(serviceInfo: NsdServiceInfo) {
                isRegistered = false
                Log.d(TAG, "Service unregistered: ${serviceInfo.serviceName}")
            }

            override fun onUnregistrationFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
                Log.e(TAG, "Service unregistration failed: Error code $errorCode")
            }
        }

        try {
            nsdManager.registerService(serviceInfo, NsdManager.PROTOCOL_DNS_SD, registrationListener)
        } catch (e: Exception) {
            Log.e(TAG, "Exception registering NSD service", e)
        }
    }

    private fun startDiscovery() {
        if (isDiscovering || nsdManager == null) return

        discoveryListener = object : NsdManager.DiscoveryListener {
            override fun onDiscoveryStarted(serviceType: String) {
                isDiscovering = true
                Log.d(TAG, "NSD discovery started for $serviceType")
            }

            override fun onServiceFound(serviceInfo: NsdServiceInfo) {
                Log.d(TAG, "NSD Service found: ${serviceInfo.serviceName}")
                if (serviceInfo.serviceType == SERVICE_TYPE) {
                    // Ignore our own service
                    if (serviceInfo.serviceName != localServiceName) {
                        resolveService(serviceInfo)
                    }
                }
            }

            override fun onServiceLost(serviceInfo: NsdServiceInfo) {
                Log.d(TAG, "NSD Service lost: ${serviceInfo.serviceName}")
                activeResolvedServices.remove(serviceInfo.serviceName)
                updatePeersList()
            }

            override fun onDiscoveryStopped(serviceType: String) {
                isDiscovering = false
                Log.d(TAG, "NSD discovery stopped")
            }

            override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {
                isDiscovering = false
                Log.e(TAG, "Start discovery failed: Error code $errorCode")
                try {
                    nsdManager.stopServiceDiscovery(this)
                } catch (e: Exception) {}
            }

            override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) {
                Log.e(TAG, "Stop discovery failed: Error code $errorCode")
            }
        }

        try {
            nsdManager.discoverServices(SERVICE_TYPE, NsdManager.PROTOCOL_DNS_SD, discoveryListener)
        } catch (e: Exception) {
            Log.e(TAG, "Exception starting NSD discovery", e)
        }
    }

    private fun resolveService(serviceInfo: NsdServiceInfo) {
        if (nsdManager == null) return
        if (activeResolvedServices.containsKey(serviceInfo.serviceName)) return

        resolveQueue.add(serviceInfo)
        drainResolveQueue()
    }

    /** Resolves queued services strictly one-by-one to avoid FAILURE_ALREADY_ACTIVE. */
    private fun drainResolveQueue() {
        if (nsdManager == null) return
        if (!isResolveInFlight.compareAndSet(false, true)) return
        val next = resolveQueue.poll()
        if (next == null) {
            isResolveInFlight.set(false)
            return
        }

        val resolveListener = object : NsdManager.ResolveListener {
            override fun onResolveFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
                Log.e(TAG, "Resolve failed for ${serviceInfo.serviceName}: Error code $errorCode")
                isResolveInFlight.set(false)
                drainResolveQueue()
            }

            override fun onServiceResolved(resolvedService: NsdServiceInfo) {
                Log.d(TAG, "Service resolved: ${resolvedService.serviceName} at ${resolvedService.host?.hostAddress}:${resolvedService.port}")
                activeResolvedServices[resolvedService.serviceName] = resolvedService
                updatePeersList()
                isResolveInFlight.set(false)
                drainResolveQueue()
            }
        }

        try {
            nsdManager.resolveService(next, resolveListener)
        } catch (e: Exception) {
            Log.e(TAG, "Exception resolving NSD service", e)
            isResolveInFlight.set(false)
            drainResolveQueue()
        }
    }

    private fun updatePeersList() {
        scope.launch {
            val peersList = mutableListOf<Peer>()
            for ((_, service) in activeResolvedServices) {
                val host = service.host ?: continue
                val ip = host.hostAddress ?: continue
                val port = service.port

                val attributes = service.attributes
                val peerId = attributes?.get("peer_id")?.let { String(it) } ?: service.serviceName.hashCode().toString()
                val username = attributes?.get("username")?.let { String(it) } ?: "مستخدم شبكة"

                val peer = Peer(
                    id = peerId,
                    name = username,
                    ip = ip,
                    port = port,
                    avatarColor = 0xFF0EA5E9,
                    userStatus = UserPresenceStatus.ONLINE,
                    statusMessage = "مكتشف عبر Network Service Discovery (mDNS)",
                    currentRoom = "general",
                    lastSeen = System.currentTimeMillis(),
                    deviceModel = service.serviceName
                )
                peersList.add(peer)
            }
            _discoveredMdnsPeers.value = peersList
        }
    }

    fun stop() {
        if (nsdManager != null) {
            if (isRegistered && registrationListener != null) {
                try {
                    nsdManager.unregisterService(registrationListener)
                } catch (e: Exception) {}
                isRegistered = false
            }
            if (isDiscovering && discoveryListener != null) {
                try {
                    nsdManager.stopServiceDiscovery(discoveryListener)
                } catch (e: Exception) {}
                isDiscovering = false
            }
        }
        activeResolvedServices.clear()
        _discoveredMdnsPeers.value = emptyList()
    }
}
