package top.cyclops.mcp.sample.provider

import top.cyclops.mcp.room.plugin.room.Room2DatabaseProvider
import top.cyclops.mcp.sample.db.AppDatabase
import javax.inject.Inject

class SampleRoom2Provider @Inject constructor(
    override val database: AppDatabase,
) : Room2DatabaseProvider {
    override val name: String = "sample.db"
    override val description: String = "Sample user database with a users table (id, name, email)"
}
