package com.zihad.netblock

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.VpnService
import android.os.ParcelFileDescriptor
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.io.FileInputStream

/**
 * Local VPN that captures traffic ONLY from the blocked apps and drops it.
 * Every other app keeps using the normal connection.
 */
class BlockVpnService : VpnService() {

    private var tun: ParcelFileDescriptor? = null
    private var drainThread: Thread? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopBlocking()
            return START_NOT_STICKY
        }
        startBlocking()
        return START_STICKY
    }

    private fun startBlocking() {
        val blocked = Prefs(this).blocked
        // With no allowed apps a VPN would capture EVERY app, so never start empty.
        if (blocked.isEmpty()) {
            stopBlocking()
            return
        }

        val builder = Builder()
            .setSession("Net Block")
            .addAddress("10.8.0.2", 32)
            .addAddress("fd00::2", 128)
            .addRoute("0.0.0.0", 0)
            .addRoute("::", 0)

        var added = 0
        for (pkg in blocked) {
            try {
                builder.addAllowedApplication(pkg)
                added++
            } catch (_: PackageManager.NameNotFoundException) {
                // App was uninstalled; skip it.
            }
        }
        if (added == 0) {
            stopBlocking()
            return
        }

        closeTunnel()
        tun = builder.establish()
        val fd = tun
        if (fd == null) {
            state.value = false
            return
        }
        state.value = true

        // Read and discard packets so the blocked apps simply get no connection.
        drainThread = Thread {
            try {
                val input = FileInputStream(fd.fileDescriptor)
                val buffer = ByteArray(32767)
                while (input.read(buffer) >= 0) { /* drop */ }
            } catch (_: Exception) {
                // Tunnel closed.
            }
        }.also { it.isDaemon = true; it.start() }
    }

    private fun closeTunnel() {
        try { tun?.close() } catch (_: Exception) {}
        tun = null
        drainThread = null
    }

    private fun stopBlocking() {
        closeTunnel()
        state.value = false
        stopSelf()
    }

    override fun onRevoke() {
        stopBlocking()
    }

    override fun onDestroy() {
        closeTunnel()
        state.value = false
        super.onDestroy()
    }

    companion object {
        private const val ACTION_STOP = "com.zihad.netblock.STOP"
        private val state = MutableStateFlow(false)
        val isRunning: StateFlow<Boolean> get() = state

        fun start(context: Context) {
            context.startService(Intent(context, BlockVpnService::class.java))
        }

        fun stop(context: Context) {
            context.startService(
                Intent(context, BlockVpnService::class.java).setAction(ACTION_STOP)
            )
        }
    }
}
