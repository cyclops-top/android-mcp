package top.cyclops.mcp.room.plugin.tool

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import top.cyclops.mcp.common.ToolResult

class RoomMcpToolMarkerTest {

    @Test
    fun `inspect schema returns clear error when no providers are registered`() = runTest {
        val tool = RoomMcpToolMarker(emptySet())

        val result = tool.inspectSchema(null)

        assertTrue(result is ToolResult.Error)
        assertEquals(
            "No database providers registered. Please implement McpRoom2Provider or McpRoom3Provider and register via @IntoSet",
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
}
