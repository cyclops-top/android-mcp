# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build Commands

```bash
./gradlew assembleDebug           # Build debug AAR
./gradlew assembleRelease        # Build release AAR (publishes to repository)
./gradlew lint                    # Run lint analysis
./gradlew test                    # Run unit tests
./gradlew connectedAndroidTest    # Run instrumented tests (requires emulator/device)
```

Single test execution:
```bash
./gradlew test --tests "top.cyclops.mcp.*"
./gradlew connectedAndroidTest --tests "top.cyclops.mcp.*"
```

## Architecture

This is an **Android library** for MCP (Model Context Protocol) server implementation on Android. The library provides dependency injection via Hilt, runs an MCP server using Ktor, and uses dynamic proxy annotation processing (similar to Retrofit) to auto-register MCP tools.

### Key Structure
- `app/src/main/java/top/cyclops/mcp/` - Main library code
  - `di/` - Hilt modules for dependency injection
  - `mcp/` - MCP server implementation using Ktor
  - `annotation/` - Annotations for tool/resource/prompt registration
  - `processor/` - Annotation processors for dynamic proxy generation
- `app/src/main/res/` - Android resources

### Technology Stack
- **Kotlin MCP SDK** - Official Kotlin SDK for MCP protocol
- **Ktor** - Embedded server for running MCP on Android
- **Hilt** - Dependency injection
- **Dynamic Proxy** - Annotation-based tool registration (similar to Retrofit)
- Kotlin 2.2.10, AGP 9.2.1, compileSdk 36, minSdk 29

### MCP Tool Registration Pattern

Tools are registered via annotations on interface methods:

```kotlin
@McpTool
suspend fun getWeather(city: String): String
```

The processor generates a dynamic proxy that handles JSON-RPC serialization/deserialization.

### Running MCP Server

The library provides a Ktor-based server that:
1. Receives MCP JSON-RPC requests
2. Routes to registered tools via generated proxies
3. Returns responses via the MCP protocol

## Local Configuration

Local settings in `.claude/settings.local.json` grant permissions for `rtk ls` and `rtk find` commands.

<!-- code-review-graph MCP tools -->
## MCP Tools: code-review-graph

**IMPORTANT: This project has a knowledge graph. ALWAYS use the
code-review-graph MCP tools BEFORE using Grep/Glob/Read to explore
the codebase.** The graph is faster, cheaper (fewer tokens), and gives
you structural context (callers, dependents, test coverage) that file
scanning cannot.

### When to use graph tools FIRST

- **Exploring code**: `semantic_search_nodes` or `query_graph` instead of Grep
- **Understanding impact**: `get_impact_radius` instead of manually tracing imports
- **Code review**: `detect_changes` + `get_review_context` instead of reading entire files
- **Finding relationships**: `query_graph` with callers_of/callees_of/imports_of/tests_for
- **Architecture questions**: `get_architecture_overview` + `list_communities`

Fall back to Grep/Glob/Read **only** when the graph doesn't cover what you need.

### Key Tools

| Tool | Use when |
| ------ | ---------- |
| `detect_changes` | Reviewing code changes — gives risk-scored analysis |
| `get_review_context` | Need source snippets for review — token-efficient |
| `get_impact_radius` | Understanding blast radius of a change |
| `get_affected_flows` | Finding which execution paths are impacted |
| `query_graph` | Tracing callers, callees, imports, tests, dependencies |
| `semantic_search_nodes` | Finding functions/classes by name or keyword |
| `get_architecture_overview` | Understanding high-level codebase structure |
| `refactor_tool` | Planning renames, finding dead code |

### Workflow

1. The graph auto-updates on file changes (via hooks).
2. Use `detect_changes` for code review.
3. Use `get_affected_flows` to understand impact.
4. Use `query_graph` pattern="tests_for" to check coverage.
