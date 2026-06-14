package top.cyclops.mcp.room.plugin.core

sealed interface McpDatabaseProvider {
    val name: String
    val description: String

    /**
     * Called after MCP executes a write statement and detects the affected Room table names.
     * Override this to refresh Room invalidation tracking so DAO Flow queries re-emit.
     */
    suspend fun notifyObserversByTableNames(tables: Set<String>) = Unit
}
