package top.cyclops.mcp.room.plugin.core

import androidx.sqlite.SQLiteConnection

interface McpRoom3Provider : McpRoomProvider {
    suspend fun <R> useConnection(block: suspend (SQLiteConnection) -> R): R
}
