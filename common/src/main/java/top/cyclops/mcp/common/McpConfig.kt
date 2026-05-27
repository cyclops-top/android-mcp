package top.cyclops.mcp.common

/**
 * Configuration for MCP server.
 */
data class McpConfig(
    val name: String = "android-mcp",
    val port: Int = 8080,
    val host: String = "0.0.0.0",
    val debug: Boolean = false,
)