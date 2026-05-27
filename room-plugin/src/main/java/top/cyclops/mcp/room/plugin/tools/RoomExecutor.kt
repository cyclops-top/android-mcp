package top.cyclops.mcp.room.plugin.tools

import top.cyclops.mcp.room.plugin.core.McpRoom2Provider
import top.cyclops.mcp.room.plugin.core.McpRoom3Provider
import top.cyclops.mcp.room.plugin.core.McpRoomProvider
import top.cyclops.mcp.room.plugin.utils.CsvConverter

class RoomExecutor(
    private val providers: Map<String, McpRoomProvider>
) {
    suspend fun execute(databaseName: String, sql: String): String {
        val provider = providers[databaseName]
            ?: throw IllegalStateException(
                if (providers.isEmpty()) {
                    "No database providers registered. Please implement McpRoom2Provider or McpRoom3Provider and register via @IntoSet"
                } else {
                    "Database not found: $databaseName. Available databases: ${providers.keys.joinToString()}"
                }
            )
        return when (provider) {
            is McpRoom3Provider -> provider.useConnection { connection ->
                connection.prepare(sql).use { stmt ->
                    CsvConverter.statementToCsv(stmt)
                }
            }

            is McpRoom2Provider -> {
                val db = provider.getReadableDatabase()
                val cursor = db.query(sql)
                try {
                    CsvConverter.cursorToCsv(cursor)
                } finally {
                    cursor.close()
                }
            }
        }
    }
}
