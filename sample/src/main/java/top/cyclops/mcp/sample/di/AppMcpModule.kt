package top.cyclops.mcp.sample.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import top.cyclops.mcp.common.McpConfig
import top.cyclops.mcp.common.McpToolMarker
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
            port = 11432,
        )
    }

    @Provides
    @IntoSet
    fun provideTestToolMarker(testTool: TestTool): McpToolMarker {
        return testTool
    }
}