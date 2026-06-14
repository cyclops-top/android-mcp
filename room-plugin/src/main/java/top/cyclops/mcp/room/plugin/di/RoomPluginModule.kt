package top.cyclops.mcp.room.plugin.di

import dagger.Binds
import dagger.BindsOptionalOf
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import top.cyclops.mcp.common.McpToolMarker
import top.cyclops.mcp.room.plugin.config.RoomMcpConfig
import top.cyclops.mcp.room.plugin.tool.RoomMcpToolMarker

@Module
@InstallIn(SingletonComponent::class)
abstract class RoomPluginModule {

    @Binds
    @IntoSet
    abstract fun bindRoomMcpToolMarker(tool: RoomMcpToolMarker): McpToolMarker

    @BindsOptionalOf
    abstract fun optionalRoomMcpConfig(): RoomMcpConfig
}
