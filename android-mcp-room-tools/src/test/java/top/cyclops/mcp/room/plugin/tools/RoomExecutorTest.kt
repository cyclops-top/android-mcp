package top.cyclops.mcp.room.plugin.tools

import androidx.sqlite.SQLiteStatement
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import top.cyclops.mcp.room.plugin.core.McpRoom3DatabaseProvider

class RoomExecutorTest {

    @Test
    fun `statement execution notifies affected tables`() = runTest {
        val provider = RecordingRoom3Provider()
        val executor = RoomExecutor(mapOf(provider.name to provider))

        val result = executor.execute(
            databaseName = provider.name,
            sql = "UPDATE users SET name = 'Ada'",
            mode = SqlExecutionMode.STATEMENT,
            affectedTables = setOf("users")
        )

        assertEquals("OK", result)
        assertTrue(provider.statement.stepCalled)
        assertEquals(listOf(setOf("users")), provider.notifications)
    }

    @Test
    fun `statement execution skips notification when affected tables are unknown`() = runTest {
        val provider = RecordingRoom3Provider()
        val executor = RoomExecutor(mapOf(provider.name to provider))

        val result = executor.execute(
            databaseName = provider.name,
            sql = "CREATE INDEX index_users_name ON users(name)",
            mode = SqlExecutionMode.STATEMENT,
            affectedTables = emptySet()
        )

        assertEquals("OK", result)
        assertTrue(provider.statement.stepCalled)
        assertEquals(emptyList<Set<String>>(), provider.notifications)
    }

    private class RecordingRoom3Provider : McpRoom3DatabaseProvider {
        override val name: String = "test.db"
        override val description: String = "Test database"
        val statement = RecordingStatement()
        val notifications = mutableListOf<Set<String>>()

        override suspend fun <R> useStatement(
            sql: String,
            isReadOnly: Boolean,
            block: suspend (SQLiteStatement) -> R
        ): R {
            return block(statement)
        }

        override suspend fun notifyObserversByTableNames(tables: Set<String>) {
            notifications += tables
        }
    }

    private class RecordingStatement : SQLiteStatement {
        var stepCalled: Boolean = false

        override fun bindBlob(index: Int, value: ByteArray) = unsupported()
        override fun bindDouble(index: Int, value: Double) = unsupported()
        override fun bindLong(index: Int, value: Long) = unsupported()
        override fun bindText(index: Int, value: String) = unsupported()
        override fun bindNull(index: Int) = unsupported()
        override fun getBlob(index: Int): ByteArray = unsupported()
        override fun getDouble(index: Int): Double = unsupported()
        override fun getLong(index: Int): Long = unsupported()
        override fun getText(index: Int): String = unsupported()
        override fun isNull(index: Int): Boolean = unsupported()
        override fun getColumnCount(): Int = 0
        override fun getColumnName(index: Int): String = unsupported()
        override fun getColumnType(index: Int): Int = unsupported()

        override fun step(): Boolean {
            stepCalled = true
            return false
        }

        override fun reset() = Unit
        override fun clearBindings() = Unit
        override fun close() = Unit

        private fun unsupported(): Nothing = throw UnsupportedOperationException()
    }
}
