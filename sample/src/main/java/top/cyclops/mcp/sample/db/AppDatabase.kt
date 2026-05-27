package top.cyclops.mcp.sample.db

import androidx.room.Database
import androidx.room.RoomDatabase
import top.cyclops.mcp.sample.dao.UserDao
import top.cyclops.mcp.sample.entity.User

@Database(entities = [User::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
}
