package top.cyclops.mcp.room.plugin.tools

object SqlPolicy {
    private val limitRegex = Regex("""\blimit\b""", RegexOption.IGNORE_CASE)

    fun validate(sql: String): String? {
        val normalized = sql.trimStart()
        if (!normalized.startsWith("select", ignoreCase = true)) {
            return "Only read-only SELECT queries are allowed"
        }
        if (normalized.contains("sqlite_master", ignoreCase = true)) {
            return null
        }
        return if (limitRegex.containsMatchIn(normalized)) {
            null
        } else {
            "SELECT queries must include a LIMIT clause"
        }
    }
}
