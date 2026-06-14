# AndroidMcp

MCP (Model Context Protocol) server library for Android. Embed an MCP server directly inside your
Android app — AI agents can query databases, invoke business logic, and interact with your app at
runtime through the standard MCP protocol.

## Architecture

```
┌──────────────────────────────────────────┐
│  AI Client (Claude Desktop / Cursor)     │
└──────────────┬───────────────────────────┘
               │ Streamable HTTP (via adb forward)
┌──────────────▼───────────────────────────┐
│  Android Device                          │
│  ┌────────────────────────────────────┐  │
│  │  Ktor CIO embedded server          │  │
│  │  ┌──────────────────────────────┐  │  │
│  │  │  McpServer                    │  │  │
│  │  │  ┌────────────────────────┐  │  │  │
│  │  │  │  McpToolRegistry        │  │  │  │
│  │  │  │  (auto-discovers tools) │  │  │  │
│  │  │  └────────────────────────┘  │  │  │
│  │  └──────────────────────────────┘  │  │
│  └────────────────────────────────────┘  │
│  ┌────────────────────────────────────┐  │
│  │  Plugins (android-mcp-room-tools)  │  │
│  │  → @McpTool methods auto-registered│  │
│  └────────────────────────────────────┘  │
└──────────────────────────────────────────┘
```

## Quick Start

### 1. Define a tool

```kotlin
@Singleton
class GreetingTool @Inject constructor() : McpToolMarker {

    @McpTool(name = "hello", description = "Say hello to the user")
    suspend fun hello(
        @McpParam(name = "name", description = "Your name") name: String
    ): ToolResult {
        return ToolResult.Text("Hello, $name!")
    }
}
```

### 2. Register tool and configure server

```kotlin
@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideMcpConfig(): McpConfig = McpConfig(
        name = "my-app-mcp",
        port = 11432,
        debug = true,
    )

    @Provides
    @IntoSet
    fun provideGreetingTool(tool: GreetingTool): McpToolMarker = tool
}
```

### 3. Connect

The server auto-starts via AppStartup. Forward the port and connect:

```bash
adb forward tcp:11432 tcp:11432
```

Then add to your MCP client config:

```json
{
  "mcpServers": {
    "my-app": {
      "url": "http://localhost:11432/"
    }
  }
}
```

## Modules

| Module                  | Purpose                                                                                       | Ships in release |
|-------------------------|-----------------------------------------------------------------------------------------------|:----------------:|
| `common`                | Shared types: `McpConfig`, `McpToolMarker`, annotations, `ToolResult`                         |       Yes        |
| `mcp`                   | Ktor server, `McpToolRegistry`, auto-start via AppStartup                                     |       Yes        |
| `android-mcp-room-core` | Abstract interfaces: `McpDatabaseProvider`, `McpRoom2DatabaseProvider`, `McpRoom3DatabaseProvider` |       Yes        |
| `android-mcp-room2`     | Room 2.x adapter defaults for `McpRoom2DatabaseProvider`                                      |       Yes        |
| `android-mcp-room3`     | Room 3.x adapter defaults for `McpRoom3DatabaseProvider`                                      |       Yes        |
| `android-mcp-room-tools`| Debug MCP tools: `list_databases`, `inspect_schema`, `execute_sql`                            |    Debug only    |
| `sample`                | Demo app with Room database + tools                                                           |        —         |

The split between `android-mcp-room-core` (always included, no MCP dependency) and
`android-mcp-room-tools` (debug-only, depends on `:mcp`) means Room database tooling should be
added with `debugImplementation` and kept out of release builds.

## Room Tools

The built-in `android-mcp-room-tools` module exposes your Room databases as MCP tools:

```
list_databases  → "sample.db - 示例用户数据库"
inspect_schema  → GET DDL for all tables
execute_sql     → Run raw SQL, returns CSV
```

`android-mcp-room-tools` is intended for developer builds. By default, `execute_sql` allows write
statements so developers can inspect and repair local debug data quickly. If your workflow needs a
read-only debug surface, provide a `RoomMcpConfig` with `SqlPolicyConfig(allowWrites = false)`.

Add the core provider API, the adapter that matches your Room generation, and the debug-only tools:

```kotlin
implementation("top.cyclops:android-mcp-room-core:<version>")

// Room 2.x / androidx.room
implementation("top.cyclops:android-mcp-room2:<version>")

// Room 3.x / androidx.room3
implementation("top.cyclops:android-mcp-room3:<version>")

debugImplementation("top.cyclops:android-mcp-room-tools:<version>")
```

Then implement a provider and register it:

```kotlin
// 1. Implement a Room 2.x provider
class SampleRoom2Provider @Inject constructor(
    override val database: AppDatabase,
) : Room2DatabaseProvider {
    override val name: String = "sample.db"
    override val description: String = "User database with users table (id, name, email)"
}

// 2. Bind it into the set — tools are auto-discovered
@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    @Provides @IntoSet
    fun provideRoomProvider(provider: SampleRoom2Provider): McpDatabaseProvider = provider
}
```

For Room 3.x, use `Room3DatabaseProvider` instead:

```kotlin
class SampleRoom3Provider @Inject constructor(
    override val database: AppDatabase,
) : Room3DatabaseProvider {
    override val name: String = "sample.db"
    override val description: String = "User database with users table (id, name, email)"
}
```

## Creating Custom Plugins

Any class implementing `McpToolMarker` with `@McpTool`-annotated methods is auto-discovered.
Register it via Hilt `@IntoSet`:

```kotlin
@Module
@InstallIn(SingletonComponent::class)
abstract class MyPluginModule {
    @Binds @IntoSet
    abstract fun bindMyTool(tool: MyCustomTool): McpToolMarker
}
```

## Configuration

```kotlin
data class McpConfig(
    val name: String = "android-mcp",  // Server name shown to clients
    val port: Int = 8080,             // HTTP port
    val host: String = "0.0.0.0",    // Bind address
    val debug: Boolean = false,       // Enable request/response logging
)
```

## Requirements

- minSdk 29
- Kotlin 2.3+
- Hilt 2.59+

## License

Apache 2.0
