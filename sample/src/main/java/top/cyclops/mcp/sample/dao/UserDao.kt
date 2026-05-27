package top.cyclops.mcp.sample.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import top.cyclops.mcp.sample.entity.User

@Dao
interface UserDao {
    @Query("SELECT * FROM users")
    suspend fun getAll(): List<User>

    @Insert
    suspend fun insert(user: User)
}
