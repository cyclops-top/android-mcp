package top.cyclops.mcp.room.plugin.utils

import android.database.Cursor

object CsvConverter {

    fun cursorToCsv(cursor: Cursor): String {
        val columnCount = cursor.columnCount
        return buildCsv(
            columnCount = columnCount,
            headerFor = { cursor.getColumnName(it) },
            hasNext = { cursor.moveToNext() },
            valueFor = { i ->
                when (cursor.getType(i)) {
                    Cursor.FIELD_TYPE_INTEGER -> cursor.getLong(i).toString()
                    Cursor.FIELD_TYPE_FLOAT -> cursor.getDouble(i).toString()
                    Cursor.FIELD_TYPE_NULL -> "NULL"
                    Cursor.FIELD_TYPE_BLOB -> "[BLOB]"
                    else -> escapeCsv(cursor.getString(i) ?: "")
                }
            }
        )
    }

    fun statementToCsv(stmt: androidx.sqlite.SQLiteStatement): String {
        val columnCount = stmt.getColumnCount()
        return buildCsv(
            columnCount = columnCount,
            headerFor = { stmt.getColumnName(it) },
            hasNext = { stmt.step() },
            valueFor = { i ->
                if (stmt.isNull(i)) "NULL"
                else escapeCsv(stmt.getText(i))
            }
        )
    }

    private fun buildCsv(
        columnCount: Int,
        headerFor: (Int) -> String,
        hasNext: () -> Boolean,
        valueFor: (Int) -> String
    ): String {
        val builder = StringBuilder()
        val headers = (0 until columnCount).map { escapeCsv(headerFor(it)) }
        builder.append(headers.joinToString(",")).append("\n")

        while (hasNext()) {
            val row = (0 until columnCount).map(valueFor)
            builder.append(row.joinToString(",")).append("\n")
        }
        return builder.toString()
    }

    private fun escapeCsv(str: String): String {
        return if (str.contains(",") || str.contains("\"") || str.contains("\n")) {
            "\"${str.replace("\"", "\"\"")}\""
        } else {
            str
        }
    }
}
