package top.cyclops.mcp.server

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import top.cyclops.mcp.common.McpParam
import top.cyclops.mcp.common.McpTool
import top.cyclops.mcp.common.McpToolMarker
import top.cyclops.mcp.common.ToolResult

class McpToolRegistryTest {

    @Test
    fun `schema maps Kotlin parameter types and custom parameter names`() {
        val registry = McpToolRegistry(setOf(TypedTool()))

        val tool = registry.getToolDefinitions().single { it.name == "typed" }

        assertEquals("integer", propertyValue(tool, "count", "type"))
        assertEquals("boolean", propertyValue(tool, "enabled", "type"))
        assertEquals("label", propertyValue(tool, "label", "description"))
        assertEquals(listOf("count", "enabled"), tool.inputSchema.required)
    }

    @Test
    fun `execute converts string arguments to Kotlin parameter types and uses defaults`() = kotlinx.coroutines.test.runTest {
        val registry = McpToolRegistry(setOf(TypedTool()))

        val result = registry.executeTool(
            "typed",
            mapOf(
                "count" to "7",
                "enabled" to "true"
            )
        )

        assertEquals(ToolResult.Text("count=7 enabled=true label=fallback"), result)
    }

    @Test
    fun `execute returns argument error instead of throwing for missing required parameter`() = kotlinx.coroutines.test.runTest {
        val registry = McpToolRegistry(setOf(TypedTool()))

        val result = registry.executeTool("typed", mapOf("enabled" to true))

        assertTrue(result is ToolResult.Error)
        assertEquals("Missing required parameter: count", (result as ToolResult.Error).message)
    }

    class TypedTool : McpToolMarker {
        @McpTool(name = "typed", description = "Typed parameters")
        fun typed(
            @McpParam(description = "count") count: Int,
            @McpParam(description = "enabled") enabled: Boolean,
            @McpParam(description = "label", required = false) label: String = "fallback"
        ): ToolResult = ToolResult.Text("count=$count enabled=$enabled label=$label")
    }

    private fun propertyValue(
        tool: McpToolRegistry.ToolDefinition,
        property: String,
        field: String
    ): String = tool.inputSchema.properties
        ?.get(property)
        ?.jsonObject
        ?.get(field)
        ?.jsonPrimitive
        ?.content
        ?: error("Missing $property.$field")
}
