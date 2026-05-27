package top.cyclops.mcp.server

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import top.cyclops.mcp.common.McpConfig
import javax.inject.Inject

@AndroidEntryPoint
class McpServerService : Service() {

    @Inject
    lateinit var mcpServer: McpServer

    @Inject
    lateinit var config: McpConfig

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        Log.i(TAG, "MCP Service created")
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.i(TAG, "Starting MCP Server: ${config.name} on ${config.host}:${config.port}")

        startForeground(NOTIFICATION_ID, createNotification())

        serviceScope.launch {
            try {
                mcpServer.start()
                val tools = mcpServer.tools
                Log.i(TAG, "MCP Server started successfully")
                Log.i(TAG, "MCP Server info: name=${config.name}, port=${config.port}")
                Log.i(TAG, "Registered tools (${tools.size}): ${tools.joinToString { it.name }}")

                tools.forEachIndexed { index, tool ->
                    Log.d(TAG, "  Tool[$index]: name=${tool.name}, description=${tool.description}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to start MCP Server", e)
            }
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        mcpServer.stop()
        Log.i(TAG, "MCP Server stopped")
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "MCP Server",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "MCP Server running on ${config.port}"
            setShowBadge(false)
        }
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }

    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(config.name)
            .setContentText("MCP Server running on ${config.host}:${config.port}")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()
    }

    companion object {
        const val TAG = "McpServerService"
        const val CHANNEL_ID = "mcp_server_channel"
        const val NOTIFICATION_ID = 1
    }
}