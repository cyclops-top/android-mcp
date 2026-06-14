package top.cyclops.mcp.room.plugin.core

import androidx.sqlite.SQLiteStatement

interface McpRoom3DatabaseProvider : McpDatabaseProvider {
    suspend fun <R> useStatement(
        sql: String,
        isReadOnly: Boolean,
        block: suspend (SQLiteStatement) -> R
    ): R
}
