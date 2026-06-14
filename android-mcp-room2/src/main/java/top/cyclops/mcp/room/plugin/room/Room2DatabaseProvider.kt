package top.cyclops.mcp.room.plugin.room

import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import top.cyclops.mcp.room.plugin.core.McpRoom2DatabaseProvider

interface Room2DatabaseProvider : McpRoom2DatabaseProvider {
    val database: RoomDatabase

    override fun getReadableDatabase(): SupportSQLiteDatabase =
        database.openHelper.readableDatabase

    @Suppress("UNUSED_PARAMETER")
    override suspend fun notifyObserversByTableNames(tables: Set<String>) {
        // Room's table-targeted refresh API is restricted; use the public pending refresh hook.
        database.invalidationTracker.refreshAsync()
    }
}
