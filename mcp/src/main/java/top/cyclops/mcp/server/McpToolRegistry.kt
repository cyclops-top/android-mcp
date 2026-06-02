package top.cyclops.mcp.server

import android.util.Log
import io.modelcontextprotocol.kotlin.sdk.types.ToolSchema
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import top.cyclops.mcp.common.McpParam
import top.cyclops.mcp.common.McpTool
import top.cyclops.mcp.common.McpToolMarker
import top.cyclops.mcp.common.ToolResult
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.reflect.KParameter
import kotlin.reflect.KType
import kotlin.reflect.full.callSuspendBy
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
                        val paramName = param.toolParameterName() ?: return@forEach
                        val mcpParam = param.findAnnotation<McpParam>()
                        put(paramName, buildJsonObject {
                            put("type", param.type.toJsonSchemaType())
                            put("description", mcpParam?.description ?: "Parameter: $paramName")
                        })
                    }
                }
            }

            // Extract required parameters (respected both Kotlin default values and @McpParam)
            val requiredList = function.parameters
                .filter { param ->
                    param.kind != KParameter.Kind.INSTANCE &&
                    !param.isOptional &&
                    param.findAnnotation<McpParam>()?.required != false
                }
                .mapNotNull { it.toolParameterName() }

            tools[name] = ToolHandler(
                description = description,
                inputSchema = ToolSchema(
                    properties = propertiesObj,
                    required = requiredList
                ),
                handler = { args ->
                    try {
                        val params = convertArgs(instance, args, function.parameters)
                        val result = withContext(Dispatchers.IO) {
                            // Safe call: handles both regular and suspend functions
                            if (function.isSuspend) {
                                function.callSuspendBy(params)
                            } else {
                                function.callBy(params)
                            }
                        }
                        (result as? ToolResult) ?: ToolResult.Error("Invalid return type")
                    } catch (e: IllegalArgumentException) {
                        ToolResult.Error(e.message ?: "Invalid arguments")
                    } catch (e: Exception) {
                        ToolResult.Error("Tool execution failed: ${e.message}")
                    }
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
    ): Map<KParameter, Any?> = buildMap {
        parameters.forEach { param ->
            if (param.kind == KParameter.Kind.INSTANCE) {
                put(param, instance)
                return@forEach
            }

            val paramName = param.toolParameterName() ?: return@forEach
            val hasValue = args.containsKey(paramName)
            if (!hasValue) {
                if (param.isOptional || param.type.isMarkedNullable || param.findAnnotation<McpParam>()?.required == false) {
                    return@forEach
                }
                throw IllegalArgumentException("Missing required parameter: $paramName")
            }
            put(param, convertValue(args[paramName], param))
        }
    }

    private fun convertValue(value: Any?, param: KParameter): Any? {
        if (value == null) return null
        return when (param.type.classifier) {
            String::class -> value.toString()
            Int::class -> value.toString().toIntOrNull()
            Long::class -> value.toString().toLongOrNull()
            Float::class -> value.toString().toFloatOrNull()
            Double::class -> value.toString().toDoubleOrNull()
            Boolean::class -> value.toString().toBooleanStrictOrNull()
            else -> value
        } ?: throw IllegalArgumentException("Invalid value for parameter: ${param.toolParameterName()}")
    }

    private fun KParameter.toolParameterName(): String? {
        val annotatedName = findAnnotation<McpParam>()?.name?.takeIf { it.isNotEmpty() }
        return annotatedName ?: name
    }

    private fun KType.toJsonSchemaType(): String {
        return when (classifier) {
            Int::class, Long::class -> "integer"
            Float::class, Double::class -> "number"
            Boolean::class -> "boolean"
            else -> "string"
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
