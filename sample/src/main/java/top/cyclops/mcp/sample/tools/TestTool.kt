package top.cyclops.mcp.sample.tools

import top.cyclops.mcp.common.McpParam
import top.cyclops.mcp.common.McpTool
import top.cyclops.mcp.common.McpToolMarker
import top.cyclops.mcp.common.ToolResult
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TestTool @Inject constructor() : McpToolMarker {

    @McpTool(name = "hello", description = "Say hello to the user")
    suspend fun hello(
        @McpParam(
            name = "name",
            description = "Your name"
        ) name: String
    ): ToolResult {
        return ToolResult.Text("Hello, $name!")
    }

    @McpTool(name = "echo", description = "Echo back the message")
    suspend fun echo(
        @McpParam(
            name = "message",
            description = "Message to echo"
        ) message: String
    ): ToolResult {
        return ToolResult.Text("Echo: $message")
    }
}