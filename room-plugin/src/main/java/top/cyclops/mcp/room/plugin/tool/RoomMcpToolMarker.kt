package top.cyclops.mcp.room.plugin.tool

import android.util.Log
import top.cyclops.mcp.common.McpParam
import top.cyclops.mcp.common.McpTool
import top.cyclops.mcp.common.McpToolMarker
import top.cyclops.mcp.common.ToolResult
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
            val duplicates =
                providers.map { it.name }.groupBy { it }.filter { it.value.size > 1 }.keys
            if (duplicates.isNotEmpty()) {
                Log.w(
                    TAG,
                    "Duplicate database names detected: $duplicates — only the last registered provider will be used"
                )
            }
        }

    private val executor = RoomExecutor(providerMap)

    companion object {
        private const val TAG = "RoomMcpToolMarker"
    }

    @McpTool(
        name = "list_databases",
        description = "List all available databases on the device with names and descriptions"
    )
    fun listDatabases(): ToolResult {
        val output = providerMap.values.sortedBy { it.name }.joinToString("\n") { provider ->
            "${provider.name} - ${provider.description}"
        }
        return ToolResult.Text(output.ifEmpty { "No databases available" })
    }

    @McpTool(
        name = "inspect_schema",
        description = "Retrieve the DDL (CREATE TABLE statements) for all user tables in the specified database. Call this tool FIRST before writing any SQL queries to understand the database schema."
    )
    suspend fun inspectSchema(
        @McpParam(
            name = "database",
            description = "Database name (optional, defaults to the first available database). Use list_databases to see available databases.",
            required = false
        )
        database: String?,
    ): ToolResult {
        val sql = """
            SELECT name as TableName, sql as DDL
            FROM sqlite_master
            WHERE type='table'
              AND name NOT LIKE 'sqlite_%'
              AND name NOT LIKE 'room_%'
              AND name != 'android_metadata'
        """.trimIndent()
        val targetDb = database?.takeIf { it.isNotEmpty() }?: providerMap.keys.first()
        return executeSqlInternal(targetDb, sql)
    }

    @McpTool(
        name = "execute_sql",
        description = "Execute raw SQL on the specified database and return results as CSV. WARNING: To prevent OOM crashes from excessive data, ALL SELECT queries MUST include a LIMIT clause (recommended: LIMIT 20)!"
    )
    suspend fun executeSql(
        @McpParam(
            name = "database",
            description = "Database name (optional, defaults to the first available database). Use list_databases to see available databases.",
            required = false
        )
        database: String?,
        @McpParam(
            name = "sql",
            description = "Raw SQLite statement to execute (example: SELECT * FROM user_table LIMIT 10)"
        )
        sql: String
    ): ToolResult {
        val targetDb = database?.takeIf { it.isNotEmpty() }?: providerMap.keys.first()
        return executeSqlInternal(targetDb, sql)
    }

    private suspend fun executeSqlInternal(database: String, sql: String): ToolResult {
        return try {
            ToolResult.Text(executor.execute(database, sql))
        } catch (e: IllegalStateException) {
            ToolResult.Error(e.message ?: "Database connection not found")
        } catch (e: Exception) {
            ToolResult.Error("SQL error: ${e.message}")
        }
    }
}
