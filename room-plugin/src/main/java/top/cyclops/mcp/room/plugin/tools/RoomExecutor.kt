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
                    "未找到任何数据库连接: 请实现 McpRoom2Provider 或 McpRoom3Provider 并通过 @IntoSet 注册"
                } else {
                    "未找到数据库: $databaseName，可用数据库: ${providers.keys.joinToString()}"
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
