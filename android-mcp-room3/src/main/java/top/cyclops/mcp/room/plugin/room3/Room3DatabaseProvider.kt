package top.cyclops.mcp.room.plugin.room3

import androidx.room3.RoomDatabase
import androidx.room3.useReaderConnection
import androidx.room3.useWriterConnection
import androidx.sqlite.SQLiteStatement
import top.cyclops.mcp.room.plugin.core.McpRoom3DatabaseProvider

interface Room3DatabaseProvider : McpRoom3DatabaseProvider {
    val database: RoomDatabase

    override suspend fun <R> useStatement(
        sql: String,
        isReadOnly: Boolean,
        block: suspend (SQLiteStatement) -> R
    ): R {
        return if (isReadOnly) {
            database.useReaderConnection { connection ->
                connection.usePrepared(sql, block)
            }
        } else {
            database.useWriterConnection { connection ->
                connection.usePrepared(sql, block)
            }
        }
    }

    @Suppress("UNUSED_PARAMETER")
    override suspend fun notifyObserversByTableNames(tables: Set<String>) {
        // Room3's table-targeted refresh API is restricted; use the public pending refresh hook.
        database.invalidationTracker.refreshAsync()
    }
}
