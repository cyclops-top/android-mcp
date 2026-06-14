package top.cyclops.mcp.room.plugin.tools

import top.cyclops.mcp.room.plugin.core.McpRoom2Provider
import top.cyclops.mcp.room.plugin.core.McpRoom3Provider
import top.cyclops.mcp.room.plugin.core.McpRoomProvider
import top.cyclops.mcp.room.plugin.utils.CsvConverter

class RoomExecutor(
    private val providers: Map<String, McpRoomProvider>
) {
    suspend fun execute(
        databaseName: String,
        sql: String,
        mode: SqlExecutionMode = SqlExecutionMode.QUERY
    ): String {
        val provider = providers[databaseName]
            ?: throw IllegalStateException(
                if (providers.isEmpty()) {
                    "No database providers registered. Please implement McpRoom2Provider or McpRoom3Provider and register via @IntoSet"
                } else {
                    "Database not found: $databaseName. Available databases: ${providers.keys.joinToString()}"
                }
            )
        return when (provider) {
            is McpRoom3Provider -> executeRoom3(provider, sql, mode)
            is McpRoom2Provider -> executeRoom2(provider, sql, mode)
        }
    }

    private suspend fun executeRoom3(
        provider: McpRoom3Provider,
        sql: String,
        mode: SqlExecutionMode
    ): String {
        return provider.useConnection { connection ->
            connection.prepare(sql).use { stmt ->
                when (mode) {
                    SqlExecutionMode.QUERY -> CsvConverter.statementToCsv(stmt)
                    SqlExecutionMode.STATEMENT -> {
                        stmt.step()
                        "OK"
                    }
                }
            }
        }
    }

    private fun executeRoom2(
        provider: McpRoom2Provider,
        sql: String,
        mode: SqlExecutionMode
    ): String {
        val db = provider.getReadableDatabase()
        return when (mode) {
            SqlExecutionMode.QUERY -> {
                val cursor = db.query(sql)
                cursor.use { cursor ->
                    CsvConverter.cursorToCsv(cursor)
                }
            }

            SqlExecutionMode.STATEMENT -> {
                db.execSQL(sql)
                "OK"
            }
        }
    }
}
