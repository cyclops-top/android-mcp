package top.cyclops.mcp.server

import android.util.Log
import io.ktor.http.HttpMethod
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.createApplicationPlugin
import io.ktor.server.application.install
import io.ktor.server.cio.CIO
import io.ktor.server.engine.embeddedServer
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.cors.routing.CORS
import io.ktor.server.request.httpMethod
import io.ktor.server.request.uri
import io.modelcontextprotocol.kotlin.sdk.server.Server
import io.modelcontextprotocol.kotlin.sdk.server.ServerOptions
import io.modelcontextprotocol.kotlin.sdk.server.mcp
import io.modelcontextprotocol.kotlin.sdk.types.CallToolResult
import io.modelcontextprotocol.kotlin.sdk.types.Implementation
import io.modelcontextprotocol.kotlin.sdk.types.ServerCapabilities
import io.modelcontextprotocol.kotlin.sdk.types.TextContent
import io.modelcontextprotocol.kotlin.sdk.types.Tool
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.longOrNull
import top.cyclops.mcp.common.McpConfig
import top.cyclops.mcp.common.ToolResult
import javax.inject.Inject
import javax.inject.Singleton


@Singleton
class McpServer @Inject constructor(
    private val toolRegistry: McpToolRegistry,
    private val config: McpConfig
) {
    private var serverJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO)
    private val _registeredTools = mutableListOf<Tool>()
    val tools: List<Tool> get() = _registeredTools
    private val mcpServerInstance = Server(
        serverInfo = Implementation(config.name, "1.0.0"),
        options = ServerOptions(
            capabilities = ServerCapabilities(
                tools = ServerCapabilities.Tools(listChanged = false)
            )
        )
    ).also { server ->

        toolRegistry.getToolDefinitions().forEach { toolDef ->
            val tool = Tool(
                name = toolDef.name,
                inputSchema = toolDef.inputSchema,
                description = toolDef.description
            )
            _registeredTools.add(tool)

            server.addTool(
                name = toolDef.name,
                description = toolDef.description,
                inputSchema = toolDef.inputSchema
            ) { request ->
                val args = request.arguments?.toMap() ?: emptyMap()
                when (val result = toolRegistry.executeTool(request.name, args)) {
                    is ToolResult.Text -> CallToolResult(
                        content = listOf(TextContent(text = result.value))
                    )

                    is ToolResult.Error -> CallToolResult(
                        content = listOf(TextContent(text = "Error: ${result.message}")),
                        isError = true
                    )

                    is ToolResult.Image -> CallToolResult(
                        content = listOf(TextContent(text = "[Image]"))
                    )
                }
            }
        }
    }

    fun start() {
        serverJob = scope.launch {
            embeddedServer(CIO, host = config.host, port = config.port) {

                if (config.debug) {
                    val androidLogPlugin = createApplicationPlugin("AndroidLogPlugin") {
                        onCall { call ->
                            Log.d(
                                TAG,
                                "🔗 [网络请求入站] -> ${call.request.uri} : ${call.request.httpMethod.value}"
                            )
                        }
                        onCallRespond { call ->
                            Log.d(TAG, "📤 [网络响应出站] <- 状态码: ${call.response.status()?.value}")
                        }
                    }
                    install(androidLogPlugin)
                }
                install(CORS) {
                    anyHost()
                    allowHeader("Content-Type")
                    allowMethod(HttpMethod.Options)
                    allowMethod(HttpMethod.Get)
                    allowMethod(HttpMethod.Post)
                }

                install(ContentNegotiation) {
                    json(Json {
                        ignoreUnknownKeys = true
                        isLenient = true
                    })
                }
                mcp {
                    mcpServerInstance
                }
            }.start(wait = false)
            Log.i(TAG, "🚀 MCP Server started at http://${config.host}:${config.port}/")
        }
    }

    fun stop() {
        serverJob?.cancel()
        serverJob = null
    }

    @Suppress("UNCHECKED_CAST")
    private fun Map<String, JsonElement>.toMap(): Map<String, Any> =
        mapValues { (_, value) -> jsonElementToAny(value) }.filterValues { it != null } as? Map<String, Any>
            ?: emptyMap()

    private fun jsonElementToAny(element: JsonElement): Any? = when (element) {
        is JsonPrimitive -> {
            element.contentOrNull?.takeIf { it.isNotEmpty() }
                ?: element.longOrNull
                ?: element.doubleOrNull
                ?: element.booleanOrNull
        }

        else -> null
    }

    companion object {
        const val TAG = "McpServer"
    }
}