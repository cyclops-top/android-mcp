package top.cyclops.mcp.sample.di

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import top.cyclops.mcp.common.McpConfig
import top.cyclops.mcp.common.McpToolMarker
import top.cyclops.mcp.room.plugin.core.McpDatabaseProvider
import top.cyclops.mcp.sample.dao.UserDao
import top.cyclops.mcp.sample.db.AppDatabase
import top.cyclops.mcp.sample.provider.SampleRoom2Provider
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
            debug = true,
        )
    }

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(context, AppDatabase::class.java, "sample.db").build()
    }

    @Provides
    fun provideUserDao(database: AppDatabase): UserDao = database.userDao()

    @Provides
    @IntoSet
    fun provideRoom2Provider(provider: SampleRoom2Provider): McpDatabaseProvider {
        return provider
    }

    @Provides
    @IntoSet
    fun provideTestToolMarker(testTool: TestTool): McpToolMarker {
        return testTool
    }
}
