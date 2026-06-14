package top.cyclops.mcp.room.plugin.tools

import top.cyclops.mcp.room.plugin.config.SqlPolicyConfig

object SqlPolicy {
    private val limitRegex = Regex("""\blimit\b""", RegexOption.IGNORE_CASE)
    private const val IDENTIFIER = """(?:"[^"]+"|`[^`]+`|\[[^]]+]|\w+)"""

    fun validate(sql: String): String? {
        return validate(sql, SqlPolicyConfig())
    }

    fun validate(sql: String, config: SqlPolicyConfig): String? {
        return when (val result = check(sql, config)) {
            is SqlPolicyResult.Allowed -> null
            is SqlPolicyResult.Rejected -> result.reason
        }
    }

    fun check(sql: String, config: SqlPolicyConfig = SqlPolicyConfig()): SqlPolicyResult {
        val normalized = stripLeadingComments(sql).trimStart()
        if (normalized.isEmpty()) {
            return SqlPolicyResult.Rejected("SQL statement is empty")
        }
        if (config.rejectMultipleStatements && hasMultipleStatements(normalized)) {
            return SqlPolicyResult.Rejected("Multiple SQL statements are not allowed")
        }

        val operation = detectOperation(normalized)
        if (operation != SqlOperation.SELECT && !config.allowWrites) {
            return SqlPolicyResult.Rejected("Only read-only SELECT queries are allowed")
        }
        if (operation == SqlOperation.UNKNOWN) {
            return SqlPolicyResult.Rejected("Unsupported SQL operation")
        }
        if (operation == SqlOperation.SELECT && config.requireLimitForSelect) {
            val isSchemaInspection = normalized.contains("sqlite_master", ignoreCase = true)
            if (!isSchemaInspection || !config.allowSchemaInspectionWithoutLimit) {
                if (!limitRegex.containsMatchIn(normalized)) {
                    return SqlPolicyResult.Rejected("SELECT queries must include a LIMIT clause")
                }
            }
        }

        return SqlPolicyResult.Allowed(
            operation = operation,
            executionMode = operation.executionMode(),
            affectedTables = operation.affectedTables(normalized)
        )
    }

    private fun detectOperation(sql: String): SqlOperation {
        val keyword = sql.takeWhile { it.isLetter() }.uppercase()
        return SqlOperation.entries.firstOrNull { it.name == keyword } ?: SqlOperation.UNKNOWN
    }

    private fun SqlOperation.executionMode(): SqlExecutionMode {
        return when (this) {
            SqlOperation.SELECT,
            SqlOperation.PRAGMA,
            SqlOperation.WITH,
            SqlOperation.EXPLAIN -> SqlExecutionMode.QUERY

            SqlOperation.INSERT,
            SqlOperation.UPDATE,
            SqlOperation.DELETE,
            SqlOperation.REPLACE,
            SqlOperation.CREATE,
            SqlOperation.ALTER,
            SqlOperation.DROP,
            SqlOperation.UNKNOWN -> SqlExecutionMode.STATEMENT
        }
    }

    private fun stripLeadingComments(sql: String): String {
        var remaining = sql.trimStart()
        while (true) {
            remaining = when {
                remaining.startsWith("--") -> {
                    val newlineIndex = remaining.indexOf('\n')
                    if (newlineIndex == -1) return "" else remaining.substring(newlineIndex + 1).trimStart()
                }

                remaining.startsWith("/*") -> {
                    val endIndex = remaining.indexOf("*/", startIndex = 2)
                    if (endIndex == -1) return "" else remaining.substring(endIndex + 2).trimStart()
                }

                else -> return remaining
            }
        }
    }

    private fun hasMultipleStatements(sql: String): Boolean {
        val firstSemicolon = sql.indexOf(';')
        if (firstSemicolon == -1) return false
        return sql.substring(firstSemicolon + 1).trim().isNotEmpty()
    }

    private fun SqlOperation.affectedTables(sql: String): Set<String> {
        val pattern = when (this) {
            SqlOperation.INSERT,
            SqlOperation.REPLACE -> """\b(?:insert|replace)\b(?:\s+or\s+\w+)?\s+into\s+($IDENTIFIER(?:\s*\.\s*$IDENTIFIER)?)"""

            SqlOperation.UPDATE -> """\bupdate\b(?:\s+or\s+\w+)?\s+($IDENTIFIER(?:\s*\.\s*$IDENTIFIER)?)"""

            SqlOperation.DELETE -> """\bdelete\s+from\s+($IDENTIFIER(?:\s*\.\s*$IDENTIFIER)?)"""

            SqlOperation.CREATE -> """\bcreate\s+(?:temp(?:orary)?\s+)?table\s+(?:if\s+not\s+exists\s+)?($IDENTIFIER(?:\s*\.\s*$IDENTIFIER)?)"""

            SqlOperation.ALTER -> """\balter\s+table\s+($IDENTIFIER(?:\s*\.\s*$IDENTIFIER)?)"""

            SqlOperation.DROP -> """\bdrop\s+table\s+(?:if\s+exists\s+)?($IDENTIFIER(?:\s*\.\s*$IDENTIFIER)?)"""

            else -> return emptySet()
        }
        return Regex(pattern, RegexOption.IGNORE_CASE)
            .find(sql)
            ?.groupValues
            ?.getOrNull(1)
            ?.let { setOf(it.toTableName()) }
            ?: emptySet()
    }

    private fun String.toTableName(): String =
        split('.')
            .last()
            .trim()
            .removeSurrounding("\"")
            .removeSurrounding("`")
            .removeSurrounding("[", "]")
}

enum class SqlOperation {
    SELECT,
    INSERT,
    UPDATE,
    DELETE,
    REPLACE,
    CREATE,
    ALTER,
    DROP,
    PRAGMA,
    WITH,
    EXPLAIN,
    UNKNOWN
}

enum class SqlExecutionMode {
    QUERY,
    STATEMENT
}

sealed class SqlPolicyResult {
    data class Allowed(
        val operation: SqlOperation,
        val executionMode: SqlExecutionMode,
        val affectedTables: Set<String>
    ) : SqlPolicyResult()

    data class Rejected(
        val reason: String
    ) : SqlPolicyResult()
}
