package top.cyclops.mcp.room.plugin.core

import androidx.sqlite.db.SupportSQLiteDatabase

interface McpRoom2Provider {
    fun getReadableDatabase(): SupportSQLiteDatabase
}
