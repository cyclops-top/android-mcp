package top.cyclops.mcp.room.plugin.tools

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import top.cyclops.mcp.room.plugin.config.SqlPolicyConfig

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
    fun `allows mutating statements by default`() {
        assertNull(SqlPolicy.validate("DELETE FROM users"))
    }

    @Test
    fun `rejects mutating statements when writes are disabled`() {
        val config = SqlPolicyConfig(allowWrites = false)

        assertEquals("Only read-only SELECT queries are allowed", SqlPolicy.validate("DELETE FROM users", config))
    }

    @Test
    fun `allows writes when configured`() {
        val config = SqlPolicyConfig(allowWrites = true)

        assertNull(SqlPolicy.validate("INSERT INTO users(name) VALUES ('Ada')", config))
        assertNull(SqlPolicy.validate("UPDATE users SET name = 'Ada' WHERE id = 1", config))
        assertNull(SqlPolicy.validate("DELETE FROM users WHERE id = 1", config))
    }

    @Test
    fun `write configuration does not disable select limit requirement`() {
        val config = SqlPolicyConfig(allowWrites = true)

        assertEquals("SELECT queries must include a LIMIT clause", SqlPolicy.validate("SELECT * FROM users", config))
    }

    @Test
    fun `allows select without limit when limit requirement is disabled`() {
        val config = SqlPolicyConfig(requireLimitForSelect = false)

        assertNull(SqlPolicy.validate("SELECT * FROM users", config))
    }

    @Test
    fun `rejects schema inspection without limit when schema exemption is disabled`() {
        val config = SqlPolicyConfig(allowSchemaInspectionWithoutLimit = false)

        assertEquals(
            "SELECT queries must include a LIMIT clause",
            SqlPolicy.validate("SELECT name, sql FROM sqlite_master", config)
        )
    }

    @Test
    fun `rejects multiple statements by default`() {
        assertEquals(
            "Multiple SQL statements are not allowed",
            SqlPolicy.validate("SELECT * FROM users LIMIT 1; DELETE FROM users")
        )
    }

    @Test
    fun `allows single trailing semicolon`() {
        assertNull(SqlPolicy.validate("SELECT * FROM users LIMIT 1;"))
    }

    @Test
    fun `strips leading comments before detecting operation`() {
        val config = SqlPolicyConfig(allowWrites = false)

        assertEquals(
            "Only read-only SELECT queries are allowed",
            SqlPolicy.validate("/* comment */ DELETE FROM users", config)
        )
        assertEquals(
            "Only read-only SELECT queries are allowed",
            SqlPolicy.validate("-- comment\nDELETE FROM users", config)
        )
    }

    @Test
    fun `check returns execution mode for allowed statements`() {
        val result = SqlPolicy.check(
            "DELETE FROM users WHERE id = 1",
            SqlPolicyConfig(allowWrites = true)
        )

        assertTrue(result is SqlPolicyResult.Allowed)
        assertEquals(SqlExecutionMode.STATEMENT, (result as SqlPolicyResult.Allowed).executionMode)
    }
}
