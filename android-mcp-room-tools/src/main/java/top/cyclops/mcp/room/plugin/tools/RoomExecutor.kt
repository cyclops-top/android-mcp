package top.cyclops.mcp.room.plugin.tools

import top.cyclops.mcp.room.plugin.core.McpRoom2DatabaseProvider
import top.cyclops.mcp.room.plugin.core.McpRoom3DatabaseProvider
import top.cyclops.mcp.room.plugin.core.McpDatabaseProvider
import top.cyclops.mcp.room.plugin.utils.CsvConverter

class RoomExecutor(
    private val providers: Map<String, McpDatabaseProvider>
) {
    suspend fun execute(
        databaseName: String,
        sql: String,
        mode: SqlExecutionMode = SqlExecutionMode.QUERY,
        affectedTables: Set<String> = emptySet()
    ): String {
        val provider = providers[databaseName]
            ?: throw IllegalStateException(
                if (providers.isEmpty()) {
                    "No database providers registered. Please implement McpRoom2DatabaseProvider or McpRoom3DatabaseProvider and register via @IntoSet"
                } else {
                    "Database not found: $databaseName. Available databases: ${providers.keys.joinToString()}"
                }
            )
        return when {
            provider is McpRoom3DatabaseProvider -> executeRoom3(provider, sql, mode, affectedTables)
            provider is McpRoom2DatabaseProvider -> executeRoom2(provider, sql, mode, affectedTables)
            else -> error("Unsupported database provider: ${provider::class.qualifiedName}")
        }
    }

    private suspend fun executeRoom3(
        provider: McpRoom3DatabaseProvider,
        sql: String,
        mode: SqlExecutionMode,
        affectedTables: Set<String>
    ): String {
        return provider.useStatement(sql, isReadOnly = mode == SqlExecutionMode.QUERY) { stmt ->
            when (mode) {
                SqlExecutionMode.QUERY -> CsvConverter.statementToCsv(stmt)
                SqlExecutionMode.STATEMENT -> {
                    stmt.step()
                    "OK"
                }
            }
        }.also {
            if (mode == SqlExecutionMode.STATEMENT) {
                provider.notifyAffectedTables(affectedTables)
            }
        }
    }

    private suspend fun executeRoom2(
        provider: McpRoom2DatabaseProvider,
        sql: String,
        mode: SqlExecutionMode,
        affectedTables: Set<String>
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
                provider.notifyAffectedTables(affectedTables)
                "OK"
            }
        }
    }

    private suspend fun McpDatabaseProvider.notifyAffectedTables(tables: Set<String>) {
        if (tables.isNotEmpty()) {
            notifyObserversByTableNames(tables)
        }
    }
}
