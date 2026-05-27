package top.cyclops.mcp.room.plugin.core

sealed interface McpRoomProvider {
    val name: String
    val description: String
}