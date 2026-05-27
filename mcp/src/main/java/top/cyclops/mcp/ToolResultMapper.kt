package top.cyclops.mcp

import io.modelcontextprotocol.kotlin.sdk.types.CallToolResult
import top.cyclops.mcp.common.ToolResult

object ToolResultMapper {
    fun toMcpResult(result: ToolResult): CallToolResult {
        return when (result) {
            is ToolResult.Text -> CallToolResult(
                content = listOf(io.modelcontextprotocol.kotlin.sdk.types.TextContent(text = result.value))
            )

            is ToolResult.Image -> CallToolResult(
                content = listOf(
                    io.modelcontextprotocol.kotlin.sdk.types.ImageContent(
                        data = result.base64,
                        mimeType = result.mimeType
                    )
                )
            )

            is ToolResult.Error -> CallToolResult(
                content = listOf(io.modelcontextprotocol.kotlin.sdk.types.TextContent(text = "Error: ${result.message}")),
                isError = true
            )
        }
    }
}