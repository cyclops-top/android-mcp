package top.cyclops.mcp.common

/**
 * Marks a parameter as an MCP tool parameter.
 */
@Target(AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.RUNTIME)
annotation class McpParam(
    val name: String = "",
    val description: String = "",
    val required: Boolean = true
)