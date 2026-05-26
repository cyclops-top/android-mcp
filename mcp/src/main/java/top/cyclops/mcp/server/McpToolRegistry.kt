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

            // 1. 动态生成 Schema：根据 Kotlin 函数的参数自动拼装 JSON Schema
            val propertiesObj = buildJsonObject {
                function.parameters.forEach { param ->
                    // 跳过 INSTANCE (类本身)
                    if (param.kind != KParameter.Kind.INSTANCE) {
                        val paramName = param.name ?: return@forEach
                        val mcpParam = param.findAnnotation<McpParam>()
                        put(paramName, buildJsonObject {
                            // 默认按 string 处理，实际可根据 param.type 进阶映射类型
                            put("type", "string")
                            put("description", mcpParam?.description ?: "参数 $paramName")
                        })
                    }
                }
            }

            // 提取必填参数
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
                    // 2. 修复反射调用：将 instance 实例传入参数转换器
                    val params = convertArgs(instance, args, function.parameters)
                    val result = withContext(Dispatchers.IO) {
                        // 3. 安全调用：兼容 suspend 挂起函数
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
        // 修复 1：KParameter.Kind.INSTANCE 必须返回目标对象实例，决不能返回 null
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
            else -> value.toString() // 兜底处理
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