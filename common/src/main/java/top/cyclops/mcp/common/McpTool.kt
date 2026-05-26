package top.cyclops.mcp.common

/**
 * Marks a method as an MCP tool.
 * The method will be exposed via the MCP protocol.
 */
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class McpTool(
    val name: String = "",
    val description: String = ""
)