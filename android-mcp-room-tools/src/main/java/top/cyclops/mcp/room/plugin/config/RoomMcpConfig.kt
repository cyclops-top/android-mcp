package top.cyclops.mcp.room.plugin.config

data class RoomMcpConfig(
    val sqlPolicy: SqlPolicyConfig = SqlPolicyConfig()
)

data class SqlPolicyConfig(
    val allowWrites: Boolean = true,
    val requireLimitForSelect: Boolean = true,
    val allowSchemaInspectionWithoutLimit: Boolean = true,
    val rejectMultipleStatements: Boolean = true,
)
