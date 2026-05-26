package top.cyclops.mcp.common

/**
 * Result type for MCP tool responses.
 */
sealed class ToolResult {
    data class Text(val value: String) : ToolResult()
    data class Image(val base64: String, val mimeType: String = "image/png") : ToolResult()
    data class Error(val message: String) : ToolResult()
}