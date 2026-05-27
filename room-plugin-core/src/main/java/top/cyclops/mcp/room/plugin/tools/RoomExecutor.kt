package top.cyclops.mcp.room.plugin.tools

import top.cyclops.mcp.room.plugin.core.McpRoom2Provider
import top.cyclops.mcp.room.plugin.core.McpRoom3Provider
import top.cyclops.mcp.room.plugin.utils.CsvConverter

class RoomExecutor(
    private val room2Provider: McpRoom2Provider?,
    private val room3Provider: McpRoom3Provider?
) {
    suspend fun execute(sql: String): String {
        room3Provider?.let {
            return it.useConnection { connection ->
                connection.prepare(sql).use { stmt ->
                    CsvConverter.statementToCsv(stmt)
                }
            }
        }
        room2Provider?.let {
            val db = it.getReadableDatabase()
            val cursor = db.query(sql)
            val csv = CsvConverter.cursorToCsv(cursor)
            cursor.close()
            return csv
        }
        throw IllegalStateException("未找到数据库连接: 请实现 McpRoom2Provider 或 McpRoom3Provider")
    }
}
