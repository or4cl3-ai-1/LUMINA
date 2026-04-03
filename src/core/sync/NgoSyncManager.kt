package ai.or4cl3.lumina.core.sync

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * NgoSyncManager — Opportunistic NGO Sync Layer
 *
 * LUMINA is offline-first. This manager opportunistically syncs
 * when a trusted NGO WiFi network is detected.
 *
 * Architecture:
 *   1. ConnectivityManager watches for WiFi availability
 *   2. On WiFi detected, NgoDiscovery probes for a LUMINA Sync Server
 *      on the local network (mDNS: _lumina._tcp.local)
 *   3. On server found, SyncSession opens and exchanges:
 *      - PULL: curriculum content updates, new activity packs
 *      - PUSH: anonymised aggregate progress (consent-gated)
 *      - PUSH: SENTINEL alerts for NGO safeguarding worker (consent-gated)
 *   4. Sync is idempotent and resumable — safe to interrupt
 *
 * Privacy guarantees:
 *   - Nothing syncs without caregiver consent flags set
 *   - All sync traffic is TLS 1.3
 *   - Personal data is never transmitted (nicknames, exact ages, etc.)
 *   - SENTINEL payloads are content-minimal (timestamp + protocol level + session ID only)
 *
 * Phase 1: Skeleton — connectivity monitoring + discovery stub
 * Phase 2: Full mDNS discovery + REST sync protocol
 * Phase 3: Bluetooth LE mesh for camp environments without WiFi router
 */
@Singleton
class NgoSyncManager @Inject constructor(
    private val context: Context
) {
    // ─── Sync state ───────────────────────────────────────────────────────────

    private val _syncState = MutableStateFlow<SyncState>(SyncState.Idle)
    val syncState: StateFlow<SyncState> = _syncState.asStateFlow()

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    // ─── Connectivity monitoring ──────────────────────────────────────────────

    private val connectivityManager by lazy {
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    }

    fun startMonitoring() {
        val request = NetworkRequest.Builder()
            .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
            .build()

        connectivityManager.registerNetworkCallback(request, object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                scope.launch { onWifiConnected() }
            }
            override fun onLost(network: Network) {
                _syncState.value = SyncState.Idle
            }
        })
    }

    fun stopMonitoring() {
        scope.cancel()
        _syncState.value = SyncState.Idle
    }

    // ─── Discovery ────────────────────────────────────────────────────────────

    private suspend fun onWifiConnected() {
        _syncState.value = SyncState.Scanning
        val server = discoverSyncServer()
        if (server != null) {
            _syncState.value = SyncState.ServerFound(server)
            performSync(server)
        } else {
            _syncState.value = SyncState.Idle
        }
    }

    /**
     * Discover a LUMINA Sync Server on the local network.
     * Phase 1: Returns null (stub)
     * Phase 2: mDNS service discovery via NsdManager (_lumina._tcp.local)
     */
    private suspend fun discoverSyncServer(): SyncServer? {
        // TODO Phase 2: NsdManager mDNS discovery
        return null
    }

    // ─── Sync session ─────────────────────────────────────────────────────────

    private suspend fun performSync(server: SyncServer) {
        _syncState.value = SyncState.Syncing(server)
        try {
            // 1. Pull curriculum updates
            pullCurriculumUpdates(server)
            // 2. Push aggregate progress (consent required)
            pushAggregateProgress(server)
            // 3. Push SENTINEL alerts (consent required)
            pushSentinelAlerts(server)

            _syncState.value = SyncState.Complete(server, System.currentTimeMillis())
        } catch (e: Exception) {
            _syncState.value = SyncState.Error(e.message ?: "Sync failed")
        }
    }

    private suspend fun pullCurriculumUpdates(server: SyncServer) {
        // TODO Phase 2: GET /api/v1/curriculum/updates?since={lastSyncTimestamp}
        // Download new content packs and activity modules
    }

    private suspend fun pushAggregateProgress(server: SyncServer) {
        // TODO Phase 2: POST /api/v1/progress/aggregate
        // Payload: anonymised, consent-gated session statistics only
        // NO personal data, NO interaction content, NO names
    }

    private suspend fun pushSentinelAlerts(server: SyncServer) {
        // TODO Phase 2: POST /api/v1/safeguarding/alerts
        // Payload: {sessionId, timestamp, protocolLevel} only
        // Caregiver sync consent required
        // SENTINEL log content is NEVER transmitted — only metadata
    }
}

// ─── Models ───────────────────────────────────────────────────────────────────

data class SyncServer(
    val host: String,
    val port: Int,
    val ngoName: String?,
    val tlsCertFingerprint: String // Verified before any data exchange
)

sealed class SyncState {
    object Idle                                         : SyncState()
    object Scanning                                     : SyncState()
    data class ServerFound(val server: SyncServer)      : SyncState()
    data class Syncing(val server: SyncServer)          : SyncState()
    data class Complete(val server: SyncServer, val at: Long) : SyncState()
    data class Error(val message: String)               : SyncState()
}
