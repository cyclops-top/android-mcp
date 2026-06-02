package top.cyclops.mcp.room.plugin.tools

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SqlPolicyTest {

    @Test
    fun `allows schema inspection query without limit`() {
        assertNull(SqlPolicy.validate("SELECT name, sql FROM sqlite_master"))
    }

    @Test
    fun `rejects select query without limit`() {
        assertEquals("SELECT queries must include a LIMIT clause", SqlPolicy.validate("SELECT * FROM users"))
    }

    @Test
    fun `rejects mutating statements`() {
        assertEquals("Only read-only SELECT queries are allowed", SqlPolicy.validate("DELETE FROM users"))
    }
}
