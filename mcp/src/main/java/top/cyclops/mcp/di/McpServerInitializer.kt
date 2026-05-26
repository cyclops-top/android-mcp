package top.cyclops.mcp.di

import android.content.Context
import androidx.startup.Initializer
import top.cyclops.mcp.server.McpServerService
import android.content.Intent

class McpServerInitializer : Initializer<Unit> {
    override fun create(context: Context) {
        val intent = Intent(context, McpServerService::class.java)
        context.startForegroundService(intent)
    }

    override fun dependencies(): List<Class<out Initializer<*>>> = emptyList()
}