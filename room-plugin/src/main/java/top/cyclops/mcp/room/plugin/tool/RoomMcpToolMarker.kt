package top.cyclops.mcp.room.plugin.tool

import android.content.Context
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.android.qualifiers.ApplicationContext
import top.cyclops.mcp.common.McpParam
import top.cyclops.mcp.common.McpTool
import top.cyclops.mcp.common.McpToolMarker
import top.cyclops.mcp.common.ToolResult
import top.cyclops.mcp.room.plugin.core.McpRoom2Provider
import top.cyclops.mcp.room.plugin.core.McpRoom3Provider
import top.cyclops.mcp.room.plugin.di.RoomPluginEntryPoint
import top.cyclops.mcp.room.plugin.tools.RoomExecutor
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.jvm.optionals.getOrNull

@Singleton
class RoomMcpToolMarker @Inject constructor(
    @ApplicationContext private val context: Context
) : McpToolMarker {

    private val executor by lazy {
        val entryPoint = EntryPointAccessors.fromApplication(
            context.applicationContext,
            RoomPluginEntryPoint::class.java
        )
        RoomExecutor(
            room2Provider = entryPoint.getRoom2Provider().getOrNull(),
            room3Provider = entryPoint.getRoom3Provider().getOrNull()
        )
    }

    @McpTool(
        name = "inspect_schema",
        description = "获取当前手机数据库中所有业务表的建表语句（DDL）。在编写具体的业务 SQL 之前，必须先调用此工具了解数据库 Schema。"
    )
    suspend fun inspectSchema(): ToolResult {
        val sql = """
            SELECT name as TableName, sql as DDL
            FROM sqlite_master
            WHERE type='table'
              AND name NOT LIKE 'sqlite_%'
              AND name NOT LIKE 'room_%'
              AND name != 'android_metadata'
        """.trimIndent()
        return executeSqlInternal(sql)
    }

    @McpTool(
        name = "execute_sql",
        description = "在设备上的 Room 数据库执行原生 SQL 并返回 CSV 数据。警告：为了防止数据量过大导致内存溢出，所有的 SELECT 查询必须显式包含 LIMIT 子句（建议 LIMIT 20）！"
    )
    suspend fun executeSql(
        @McpParam(name = "sql", description = "要执行的原生 SQLite 语句（示例：SELECT * FROM user_table LIMIT 10）")
        sql: String
    ): ToolResult {
        return executeSqlInternal(sql)
    }

    private suspend fun executeSqlInternal(sql: String): ToolResult {
        return try {
            ToolResult.Text(executor.execute(sql))
        } catch (e: IllegalStateException) {
            ToolResult.Error(e.message ?: "未找到数据库连接")
        } catch (e: Exception) {
            ToolResult.Error("SQL 报错: ${e.message}")
        }
    }
}
