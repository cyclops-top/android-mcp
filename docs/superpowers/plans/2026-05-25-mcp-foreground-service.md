# MCP Foreground Service Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Implement a foreground service in the `mcp` module that runs an MCP server via Ktor, with configuration and tools injected via Hilt DI. The service auto-starts via AppStartup.

**Architecture:** Foreground service hosts a Ktor server that exposes MCP protocol. Tools are discovered via DI (multi-binding) and registered dynamically through annotation processing. App module provides config and tool implementations.

**Tech Stack:** Kotlin MCP SDK 0.12.0, Ktor, Hilt, AppStartup, Foreground Service

---

## File Structure

```
mcp/src/main/java/top/cyclops/mcp/
├── di/
│   └── McpModule.kt              # Hilt module for DI
├── server/
│   ├── McpServerService.kt       # Foreground service with Ktor
│   ├── McpServer.kt              # MCP server wrapper
│   └── McpToolRegistry.kt        # Tool registration logic
├── annotation/
│   └── @McpTool, @McpParam       # From common module
├── ToolResultMapper.kt           # Convert ToolResult -> McpSchema
└── McpApplication.kt            # Hilt Application (already exists)

sample/src/main/java/top/cyclops/mcp/sample/
├── di/
│   └── AppMcpModule.kt           # App-specific DI
├── tools/
│   └── TestTool.kt               # Demo tool implementation
└── MainActivity.kt               # Already exists

common/src/main/java/top/cyclops/mcp/common/
├── McpToolMarker.kt             # Interface for tool classes
├── McpTool.kt                    # @McpTool annotation
├── McpParam.kt                   # @McpParam annotation
├── McpConfig.kt                  # Configuration data class
└── ToolResult.kt                # Text/Image/Error sealed class
```

---

## Tasks

### Task 1: Add Ktor and AppStartup dependencies

**Files:**
- Modify: `gradle/libs.versions.toml`
- Modify: `mcp/build.gradle.kts`

- [ ] **Step 1: Add Ktor and AppStartup versions to libs.versions.toml**

```toml
ktor = "3.0.2"
appstartup = "1.1.1"
```

- [ ] **Step 2: Add Ktor and AppStartup libraries**

```toml
ktor-server-netty = { group = "io.ktor", name = "ktor-server-netty", version.ref = "ktor" }
ktor-server-core = { group = "io.ktor", name = "ktor-server-core", version.ref = "ktor" }
androidx-appstartup = { group = "androidx.startup", name = "startup", version.ref = "appstartup" }
```

- [ ] **Step 3: Update mcp/build.gradle.kts**

```kotlin
dependencies {
    // ... existing
    implementation(libs.ktor.server.core)
    implementation(libs.ktor.server.netty)
    implementation(libs.androidx.appcompat) // for AppCompatActivity
    implementation(libs.androidx.appstartup)
}
```

---

### Task 2: Create ToolResultMapper (mcp module)

**Files:**
- Create: `mcp/src/main/java/top/cyclops/mcp/ToolResultMapper.kt`

- [ ] **Step 1: Create ToolResultMapper**

```kotlin
package top.cyclops.mcp

import io.modelcontextprotocol.spec.McpSchema
import top.cyclops.mcp.common.ToolResult

object ToolResultMapper {
    fun toMcpResult(result: ToolResult): McpSchema.CallToolResult {
        return when (result) {
            is ToolResult.Text -> McpSchema.CallToolResult(
                content = listOf(McpSchema.ContentOf(type = "text", text = result.value))
            )
            is ToolResult.Image -> McpSchema.CallToolResult(
                content = listOf(McpSchema.ContentOf(type = "image", data = result.base64, mimeType = result.mimeType))
            )
            is ToolResult.Error -> McpSchema.CallToolResult(
                content = listOf(McpSchema.ContentOf(type = "text", text = "Error: ${result.message}")),
                isError = true
            )
        }
    }
}
```

---

### Task 3: Create McpToolRegistry

**Files:**
- Create: `mcp/src/main/java/top/cyclops/mcp/server/McpToolRegistry.kt`

- [ ] **Step 1: Create McpToolRegistry**

```kotlin
package top.cyclops.mcp.server

import io.modelcontextprotocol.sdk.server.McpServer
import io.modelcontextprotocol.spec.McpSchema
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import top.cyclops.mcp.common.ToolResult
import top.cyclops.mcp.common.McpToolMarker
import top.cyclops.mcp.common.McpTool
import top.cyclops.mcp.common.McpParam
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.reflect.full.functions
import kotlin.reflect.full.findAnnotation

@Singleton
class McpToolRegistry @Inject constructor() {
    private val tools = mutableMapOf<String, ToolHandler>()

    fun registerTool(instance: McpToolMarker) {
        instance::class.functions.forEach { function ->
            val annotation = function.findAnnotation<McpTool>() ?: return@forEach
            val name = annotation.name.ifEmpty { function.name }
            val description = annotation.description

            val parameters = function.parameters.mapNotNull { param ->
                val paramAnnotation = param.findAnnotation<McpParam>()
                if (paramAnnotation != null) {
                    McpSchema.ToolArgument(
                        name = paramAnnotation.name.ifEmpty { param.name },
                        description = paramAnnotation.description,
                        required = paramAnnotation.required,
                        schema = null // simplified
                    )
                } else null
            }

            tools[name] = ToolHandler(
                description = description,
                inputSchema = McpSchema.JsonObject(parameters.mapValues {
                    buildMap {
                        put("type", "string")
                        it.value.description.let { d -> if (d.isNotEmpty()) put("description", d) }
                        if (it.value.required) put("minLength", 1) else put("optional", true)
                    }
                }),
                handler = { args ->
                    val params = function.parameters.map { it to args[it.name] }.toMap()
                    val result = withContext(Dispatchers.IO) {
                        function.call(instance, *params.values.toTypedArray())
                    }
                    (result as? ToolResult) ?: ToolResult.Error("Invalid return type")
                }
            )
        }
    }

    fun getTools(): List<McpSchema.Tool> = tools.map { (name, handler) ->
        McpSchema.Tool(name = name, description = handler.description, inputSchema = handler.inputSchema)
    }

    suspend fun executeTool(name: String, arguments: Map<String, Any>): ToolResult {
        return tools[name]?.handler(arguments) ?: ToolResult.Error("Tool not found: $name")
    }

    private data class ToolHandler(
        val description: String,
        val inputSchema: McpSchema.JsonObject,
        val handler: suspend (Map<String, Any>) -> ToolResult
    )
}
```

---

### Task 4: Create McpServer wrapper

**Files:**
- Create: `mcp/src/main/java/top/cyclops/mcp/server/McpServer.kt`

- [ ] **Step 1: Create McpServer**

```kotlin
package top.cyclops.mcp.server

import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.modelcontextprotocol.kotlin.McpServerProvider
import io.modelcontextprotocol.sdk.server.McpServer
import io.modelcontextprotocol.sdk.server.McpServerConfig
import io.modelcontextprotocol.sdk.server.transport.StdioServerTransport
import io.modelcontextprotocol.spec.McpSchema
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import top.cyclops.mcp.common.McpConfig
import top.cyclops.mcp.common.ToolResult
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class McpServer @Inject constructor(
    private val toolRegistry: McpToolRegistry,
    private val config: McpConfig
) {
    private var server: McpServer? = null
    private val scope = CoroutineScope(Dispatchers.IO)

    fun start() {
        // Implementation uses stdio transport for MCP
        val transport = StdioServerTransport()
        val mcpServer = McpServerProvider.create(
            McpServerConfig(
                name = config.name,
                version = "1.0.0"
            ),
            transport
        )

        mcpServer.setRequestHandler(McpSchema.RequestHandler::toolsList) { _ ->
            McpSchema.ListToolsResult(tools = toolRegistry.getTools())
        }

        mcpServer.setRequestHandler(McpSchema.RequestHandler::toolsCall) { request ->
            val result = toolRegistry.executeTool(
                request.params.name,
                request.params.arguments ?: emptyMap()
            )
            McpSchema.CallToolResult(
                content = listOf(
                    io.modelcontextprotocol.spec.McpSchema.ContentOf(
                        type = "text",
                        text = when (result) {
                            is ToolResult.Text -> result.value
                            is ToolResult.Error -> "Error: ${result.message}"
                            is ToolResult.Image -> "[Image: ${result.mimeType}]"
                        }
                    )
                ),
                isError = result is ToolResult.Error
            )
        }

        server = mcpServer
    }

    fun stop() {
        server = null
    }
}
```

---

### Task 5: Create Foreground Service

**Files:**
- Create: `mcp/src/main/java/top/cyclops/mcp/server/McpServerService.kt`

- [ ] **Step 1: Create Foreground Service**

```kotlin
package top.cyclops.mcp.server

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import top.cyclops.mcp.common.McpConfig
import javax.inject.Inject

@AndroidEntryPoint
class McpServerService : Service() {

    @Inject
    lateinit var mcpServer: McpServer

    @Inject
    lateinit var config: McpConfig

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(NOTIFICATION_ID, createNotification())
        serviceScope.launch {
            mcpServer.start()
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        mcpServer.stop()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "MCP Server",
                NotificationManager.IMPORTANCE_LOW
            ).apply { description = "MCP Server running" }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("MCP Server")
            .setContentText("Running on ${config.host}:${config.port}")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    companion object {
        const val CHANNEL_ID = "mcp_server_channel"
        const val NOTIFICATION_ID = 1
    }
}
```

---

### Task 6: Create Hilt DI Module

**Files:**
- Create: `mcp/src/main/java/top/cyclops/mcp/di/McpModule.kt`

- [ ] **Step 1: Create Hilt Module**

```kotlin
package top.cyclops.mcp.di

import android.content.Context
import com.google.gson.Gson
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import top.cyclops.mcp.common.McpConfig
import top.cyclops.mcp.server.McpToolRegistry
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object McpModule {

    @Provides
    @Singleton
    fun provideMcpToolRegistry(): McpToolRegistry {
        return McpToolRegistry()
    }

    @Provides
    @Singleton
    fun provideGson(): Gson {
        return Gson()
    }
}
```

---

### Task 7: Create App Startup Initializer

**Files:**
- Create: `mcp/src/main/java/top/cyclops/mcp/di/McpServerInitializer.kt`

- [ ] **Step 1: Create Initializer**

```kotlin
package top.cyclops.mcp.di

import android.content.Context
import androidx.startup.Initializer
import top.cyclops.mcp.server.McpServerService

class McpServerInitializer : Initializer<Unit> {
    override fun create(context: Context) {
        val intent = Intent(context, McpServerService::class.java)
        context.startForegroundService(intent)
    }

    override fun dependencies(): List<Class<out Initializer<*>>> = emptyList()
}
```

- [ ] **Step 2: Add to AndroidManifest.xml (mcp module)**

In `<application>`:
```xml
<provider
    android:name="androidx.startup.InitializationProvider"
    android:authorities="${applicationId}.androidx-startup"
    android:exported="false"
    tools:node="merge">
    <meta-data
        android:name="top.cyclops.mcp.di.McpServerInitializer"
        android:value="androidx.startup" />
</provider>
```

---

### Task 8: Create Test Tool

**Files:**
- Create: `sample/src/main/java/top/cyclops/mcp/sample/tools/TestTool.kt`

- [ ] **Step 1: Create TestTool**

```kotlin
package top.cyclops.mcp.sample.tools

import top.cyclops.mcp.common.McpToolMarker
import top.cyclops.mcp.common.McpTool
import top.cyclops.mcp.common.McpParam
import top.cyclops.mcp.common.ToolResult
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TestTool @Inject constructor() : McpToolMarker {

    @McpTool(name = "hello", description = "Say hello to the user")
    suspend fun hello(@McpParam(name = "name", description = "Your name") name: String): ToolResult {
        return ToolResult.Text("Hello, $name!")
    }

    @McpTool(name = "echo", description = "Echo back the message")
    suspend fun echo(@McpParam(name = "message", description = "Message to echo") message: String): ToolResult {
        return ToolResult.Text("Echo: $message")
    }
}
```

---

### Task 9: Create App DI Module

**Files:**
- Create: `sample/src/main/java/top/cyclops/mcp/sample/di/AppMcpModule.kt`

- [ ] **Step 1: Create App DI Module**

```kotlin
package top.cyclops.mcp.sample.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import top.cyclops.mcp.common.McpConfig
import top.cyclops.mcp.server.McpServer
import top.cyclops.mcp.server.McpToolRegistry
import top.cyclops.mcp.sample.tools.TestTool
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppMcpModule {

    @Provides
    @Singleton
    fun provideMcpConfig(): McpConfig {
        return McpConfig(
            name = "android-mcp-sample",
            port = 8080,
            host = "0.0.0.0"
        )
    }

    @Provides
    @Singleton
    fun registerTools(registry: McpToolRegistry, testTool: TestTool) {
        registry.registerTool(testTool)
    }
}
```

---

### Task 10: Update sample AndroidManifest

**Files:**
- Modify: `sample/src/main/AndroidManifest.xml`

- [ ] **Step 1: Add permissions and service**

```xml
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE_SPECIAL_USE" />

<application
    android:name=".McpSampleApplication">
    <!-- existing activities -->
</application>
```

- [ ] **Step 2: Create McpSampleApplication**

```kotlin
package top.cyclops.mcp.sample

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class McpSampleApplication : Application()
```

- [ ] **Step 3: Add service to AndroidManifest**

```xml
<service
    android:name="top.cyclops.mcp.server.McpServerService"
    android:foregroundServiceType="specialUse"
    android:exported="false">
    <property
        android:name="android.app.PROPERTY_SPECIAL_USE_FGS_SUBTYPE"
        android:value="MCP Server for Android" />
</service>
```

---

## Verification

```bash
./gradlew :sample:assembleDebug
```

Expected:
- Build succeeds
- MCP server service registered
- TestTool registered via DI
- AppStartup triggers service on app launch

---

## Potential Gaps

1. **Ktor transport**: MCP SDK 0.12.0 may use stdio transport by default for Android. May need to verify Ktor integration works for Android foreground service scenario.
2. **Multi-binding**: For multiple tool classes, consider using `Set<McpToolMarker>` injection instead of individual registration.
3. **AppStartup manifest merger**: Ensure meta-data is properly merged in final APK.

**Which approach for execution?**