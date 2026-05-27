package top.cyclops.mcp.room.plugin.tool

import top.cyclops.mcp.common.McpParam
import top.cyclops.mcp.common.McpTool
import top.cyclops.mcp.common.McpToolMarker
import top.cyclops.mcp.common.ToolResult
import android.util.Log
import top.cyclops.mcp.room.plugin.core.McpRoomProvider
import top.cyclops.mcp.room.plugin.tools.RoomExecutor
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RoomMcpToolMarker @Inject constructor(
    providers: Set<@JvmSuppressWildcards McpRoomProvider>
) : McpToolMarker {

    private val providerMap: Map<String, McpRoomProvider> =
        providers.associateBy { it.name }.also { map ->
            val duplicates = providers.map { it.name }.groupBy { it }.filter { it.value.size > 1 }.keys
            if (duplicates.isNotEmpty()) {
                Log.w(TAG, "检测到重复的数据库名称: $duplicates，仅最后一个生效")
            }
        }

    private val executor = RoomExecutor(providerMap)

    companion object {
        private const val TAG = "RoomMcpToolMarker"
    }

    @McpTool(
        name = "list_databases",
        description = "列出当前设备上所有可用的数据库，包含名称和描述信息"
    )
    fun listDatabases(): ToolResult {
        val output = providerMap.values.sortedBy { it.name }.joinToString("\n") { provider ->
            "${provider.name} - ${provider.description}"
        }
        return ToolResult.Text(output.ifEmpty { "暂无可用数据库" })
    }

    @McpTool(
        name = "inspect_schema",
        description = "获取指定数据库中所有业务表的建表语句（DDL）。在编写具体的业务 SQL 之前，必须先调用此工具了解数据库 Schema。"
    )
    suspend fun inspectSchema(
        @McpParam(name = "database", description = "数据库名称。可以通过list_databases获取 ，如果只有一个数据库可传空字符串。")
        database: String,
    ): ToolResult {
        val sql = """
            SELECT name as TableName, sql as DDL
            FROM sqlite_master
            WHERE type='table'
              AND name NOT LIKE 'sqlite_%'
              AND name NOT LIKE 'room_%'
              AND name != 'android_metadata'
        """.trimIndent()
        val targetDb = if (providerMap.size == 1 && database.isBlank()) {
            providerMap.keys.first()
        } else {
            database
        }
        return executeSqlInternal(targetDb, sql)
    }

    @McpTool(
        name = "execute_sql",
        description = "在设备上指定数据库执行原生 SQL 并返回 CSV 数据。警告：为了防止数据量过大导致内存溢出，所有的 SELECT 查询必须显式包含 LIMIT 子句（建议 LIMIT 20）！"
    )
    suspend fun executeSql(
        @McpParam(name = "database", description = "数据库名称。可以通过list_databases获取 ，如果只有一个数据库可传空字符串。")
        database: String,
        @McpParam(name = "sql", description = "要执行的原生 SQLite 语句")
        sql: String
    ): ToolResult {
        val targetDb = if (providerMap.size == 1 && database.isBlank()) {
            providerMap.keys.first()
        } else {
            database
        }
        return executeSqlInternal(targetDb, sql)
    }

    private suspend fun executeSqlInternal(database: String, sql: String): ToolResult {
        return try {
            ToolResult.Text(executor.execute(database, sql))
        } catch (e: IllegalStateException) {
            ToolResult.Error(e.message ?: "未找到数据库连接")
        } catch (e: Exception) {
            ToolResult.Error("SQL 报错: ${e.message}")
        }
    }
}
