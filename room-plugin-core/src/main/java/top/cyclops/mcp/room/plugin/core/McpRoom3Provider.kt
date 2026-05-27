package top.cyclops.mcp.room.plugin.core

import androidx.sqlite.SQLiteConnection

interface McpRoom3Provider {
    suspend fun <R> useConnection(block: suspend (SQLiteConnection) -> R): R
}
