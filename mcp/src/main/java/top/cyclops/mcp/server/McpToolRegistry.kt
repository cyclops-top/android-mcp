package top.cyclops.mcp.server

import android.util.Log
import io.modelcontextprotocol.kotlin.sdk.types.ToolSchema
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.JsonObjectBuilder
import top.cyclops.mcp.common.McpParam
import top.cyclops.mcp.common.McpTool
import top.cyclops.mcp.common.McpToolMarker
import top.cyclops.mcp.common.ToolResult
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.reflect.KParameter
import kotlin.reflect.full.callSuspend
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.functions

@Singleton
class McpToolRegistry @Inject constructor(
    private val toolMarkers: Set<@JvmSuppressWildcards McpToolMarker>
) {
    private val tools = mutableMapOf<String, ToolHandler>()

    init {
        Log.d(TAG, "McpToolRegistry init with ${toolMarkers.size} markers")
        toolMarkers.forEach { marker ->
            registerTool(marker)
        }
    }

    private fun registerTool(instance: McpToolMarker) {
        val className = instance::class.qualifiedName
        Log.d(TAG, "registerTool: scanning $className")

        instance::class.functions.forEach { function ->
            val annotation = function.findAnnotation<McpTool>() ?: return@forEach
            val name = annotation.name.ifEmpty { function.name }
            val description = annotation.description

            // Dynamically generate JSON Schema from Kotlin function parameters
            val propertiesObj = buildJsonObject {
                function.parameters.forEach { param ->
                    // Skip INSTANCE (the class itself)
                    if (param.kind != KParameter.Kind.INSTANCE) {
                        val paramName = param.name ?: return@forEach
                        val mcpParam = param.findAnnotation<McpParam>()
                        put(paramName, buildJsonObject {
                            // Default to string type; could map by param.type for richer types
                            put("type", "string")
                                                    put("description", mcpParam?.description ?: "Parameter: $paramName")
                        })
                    }
                }
            }

            // Extract required parameters
            val requiredList = function.parameters
                .filter { it.kind != KParameter.Kind.INSTANCE && !it.isOptional }
                .mapNotNull { it.name }

            tools[name] = ToolHandler(
                description = description,
                inputSchema = ToolSchema(
                    properties = propertiesObj,
                    required = requiredList
                ),
                handler = { args ->
                    // Pass the instance to the argument converter for reflective invocation
                    val params = convertArgs(instance, args, function.parameters)
                    val result = withContext(Dispatchers.IO) {
                        // Safe call: handles both regular and suspend functions
                        if (function.isSuspend) {
                            function.callSuspend(*params)
                        } else {
                            function.call(*params)
                        }
                    }
                    (result as? ToolResult) ?: ToolResult.Error("Invalid return type")
                }
            )
            Log.d(TAG, "  registered: $name")
        }
    }

    fun getToolDefinitions(): List<ToolDefinition> = tools.map { (name, handler) ->
        ToolDefinition(
            name = name,
            description = handler.description,
            inputSchema = handler.inputSchema
        )
    }

    suspend fun executeTool(name: String, arguments: Map<String, Any>): ToolResult {
        return tools[name]?.handler?.invoke(arguments) ?: ToolResult.Error("Tool not found: $name")
    }

    private fun convertArgs(
        instance: McpToolMarker,
        args: Map<String, Any>,
        parameters: List<KParameter>
    ): Array<Any?> = parameters.map { param ->
        // KParameter.Kind.INSTANCE must return the target object instance, never null
        if (param.kind == KParameter.Kind.INSTANCE) return@map instance

        val paramName = param.name ?: return@map null
        val value = args[paramName] ?: return@map null
        convertValue(value, param)
    }.toTypedArray()

    private fun convertValue(value: Any, param: KParameter): Any? {
        return when (value) {
            is String -> value
            is Number -> value
            is Boolean -> value
                        else -> value.toString() // fallback
        }
    }

    data class ToolDefinition(
        val name: String,
        val description: String,
        val inputSchema: ToolSchema
    )

    private data class ToolHandler(
        val description: String,
        val inputSchema: ToolSchema,
        val handler: suspend (Map<String, Any>) -> ToolResult
    )

    companion object {
        const val TAG = "McpToolRegistry"
    }
}