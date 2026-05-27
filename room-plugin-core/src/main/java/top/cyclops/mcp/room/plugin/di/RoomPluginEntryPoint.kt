package top.cyclops.mcp.room.plugin.di

import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import top.cyclops.mcp.room.plugin.core.McpRoom2Provider
import top.cyclops.mcp.room.plugin.core.McpRoom3Provider
import java.util.Optional

@EntryPoint
@InstallIn(SingletonComponent::class)
interface RoomPluginEntryPoint {
    fun getRoom2Provider(): Optional<McpRoom2Provider>
    fun getRoom3Provider(): Optional<McpRoom3Provider>
}
