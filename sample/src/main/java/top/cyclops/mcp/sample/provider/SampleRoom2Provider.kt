package top.cyclops.mcp.sample.provider

import androidx.sqlite.db.SupportSQLiteDatabase
import top.cyclops.mcp.room.plugin.core.McpRoom2Provider
import top.cyclops.mcp.sample.db.AppDatabase
import javax.inject.Inject

class SampleRoom2Provider @Inject constructor(
    private val database: AppDatabase,
) : McpRoom2Provider {
    override val name: String = "sample.db"
    override val description: String = "示例用户数据库，包含 users 表（id, name, email）"

    override fun getReadableDatabase(): SupportSQLiteDatabase {
        return database.openHelper.readableDatabase
    }
}
