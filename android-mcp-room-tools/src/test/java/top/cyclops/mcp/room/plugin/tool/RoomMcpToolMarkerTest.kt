package top.cyclops.mcp.room.plugin.tool

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import top.cyclops.mcp.common.ToolResult
import top.cyclops.mcp.room.plugin.config.RoomMcpConfig
import top.cyclops.mcp.room.plugin.config.SqlPolicyConfig
import java.util.Optional

class RoomMcpToolMarkerTest {

    @Test
    fun `inspect schema returns clear error when no providers are registered`() = runTest {
        val tool = RoomMcpToolMarker(emptySet())

        val result = tool.inspectSchema(null)

        assertTrue(result is ToolResult.Error)
        assertEquals(
            NO_PROVIDERS_ERROR,
            (result as ToolResult.Error).message
        )
    }

    @Test
    fun `execute sql rejects select without limit`() = runTest {
        val tool = RoomMcpToolMarker(emptySet())

        val result = tool.executeSql(null, "SELECT * FROM users")

        assertTrue(result is ToolResult.Error)
        assertEquals("SELECT queries must include a LIMIT clause", (result as ToolResult.Error).message)
    }

    @Test
    fun `execute sql allows writes by default`() = runTest {
        val tool = RoomMcpToolMarker(emptySet())

        val result = tool.executeSql(null, "DELETE FROM users WHERE id = 1")

        assertTrue(result is ToolResult.Error)
        assertEquals(NO_PROVIDERS_ERROR, (result as ToolResult.Error).message)
    }

    @Test
    fun `execute sql rejects writes when configured read only`() = runTest {
        val tool = RoomMcpToolMarker(
            providers = emptySet(),
            roomConfig = Optional.of(RoomMcpConfig(SqlPolicyConfig(allowWrites = false)))
        )

        val result = tool.executeSql(null, "DELETE FROM users WHERE id = 1")

        assertTrue(result is ToolResult.Error)
        assertEquals("Only read-only SELECT queries are allowed", (result as ToolResult.Error).message)
    }

    @Test
    fun `execute sql uses configured write policy`() = runTest {
        val tool = RoomMcpToolMarker(
            providers = emptySet(),
            roomConfig = Optional.of(RoomMcpConfig(SqlPolicyConfig(allowWrites = true)))
        )

        val result = tool.executeSql(null, "DELETE FROM users WHERE id = 1")

        assertTrue(result is ToolResult.Error)
        assertEquals(NO_PROVIDERS_ERROR, (result as ToolResult.Error).message)
    }

    companion object {
        private const val NO_PROVIDERS_ERROR =
            "No database providers registered. Please implement McpRoom2DatabaseProvider or McpRoom3DatabaseProvider and register via @IntoSet"
    }
}
