package com.example.util

import com.example.data.model.StudentEntity

data class ParsedCsvData(
    val headers: List<String>,
    val rows: List<List<String>>
)

data class ColumnMapping(
    val rollNumberIndex: Int = 0,
    val nameIndex: Int = 1,
    val emailIndex: Int = 2,
    val phoneIndex: Int = 3,
    val parentPhoneIndex: Int = 4
)

object CsvParser {

    fun parseRawText(csvText: String): ParsedCsvData {
        if (csvText.isBlank()) return ParsedCsvData(emptyList(), emptyList())

        val lines = csvText.lines()
            .map { it.trim() }
            .filter { it.isNotBlank() }

        if (lines.isEmpty()) return ParsedCsvData(emptyList(), emptyList())

        val delimiter = detectDelimiter(lines.first())
        val headers = parseLine(lines.first(), delimiter)
        val rows = lines.drop(1).map { parseLine(it, delimiter) }

        return ParsedCsvData(headers, rows)
    }

    private fun detectDelimiter(line: String): Char {
        return when {
            line.contains('\t') -> '\t'
            line.contains(';') -> ';'
            else -> ','
        }
    }

    private fun parseLine(line: String, delimiter: Char): List<String> {
        val result = mutableListOf<String>()
        val sb = StringBuilder()
        var inQuotes = false

        for (char in line) {
            when {
                char == '"' -> inQuotes = !inQuotes
                char == delimiter && !inQuotes -> {
                    result.add(sb.toString().trim())
                    sb.clear()
                }
                else -> sb.append(char)
            }
        }
        result.add(sb.toString().trim())
        return result
    }

    fun autoDetectMapping(headers: List<String>): ColumnMapping {
        var rollIdx = 0
        var nameIdx = 1
        var emailIdx = 2
        var phoneIdx = 3
        var parentPhoneIdx = 4

        headers.forEachIndexed { index, header ->
            val lower = header.lowercase()
            when {
                lower.contains("roll") || lower.contains("id") || lower.contains("number") -> rollIdx = index
                lower.contains("name") || lower.contains("student") -> nameIdx = index
                lower.contains("email") || lower.contains("mail") -> emailIdx = index
                lower.contains("parent") || lower.contains("guardian") -> parentPhoneIdx = index
                lower.contains("phone") || lower.contains("mobile") || lower.contains("contact") -> phoneIdx = index
            }
        }

        return ColumnMapping(
            rollNumberIndex = rollIdx,
            nameIndex = nameIdx,
            emailIndex = emailIdx,
            phoneIndex = phoneIdx,
            parentPhoneIndex = parentPhoneIdx
        )
    }

    fun mapToStudents(rows: List<List<String>>, mapping: ColumnMapping, classId: Long): List<StudentEntity> {
        return rows.mapNotNull { row ->
            if (row.isEmpty()) return@mapNotNull null

            val roll = if (mapping.rollNumberIndex in row.indices) row[mapping.rollNumberIndex] else ""
            val name = if (mapping.nameIndex in row.indices) row[mapping.nameIndex] else ""
            val email = if (mapping.emailIndex in row.indices) row[mapping.emailIndex] else ""
            val phone = if (mapping.phoneIndex in row.indices) row[mapping.phoneIndex] else ""
            val parentPhone = if (mapping.parentPhoneIndex in row.indices) row[mapping.parentPhoneIndex] else ""

            if (name.isBlank() && roll.isBlank()) return@mapNotNull null

            StudentEntity(
                classId = classId,
                rollNumber = roll.ifBlank { "100" },
                name = name.ifBlank { "Student" },
                email = email,
                phone = phone,
                parentPhone = parentPhone
            )
        }
    }

    fun generateSampleCsvTemplate(): String {
        return """
Roll No,Student Name,Email,Phone,Parent Phone
101,John Smith,john.smith@university.edu,+15550101,+15550199
102,Emma Watson,emma.w@university.edu,+15550102,+15550198
103,Liam Neeson,liam.n@university.edu,+15550103,+15550197
104,Sophia Martinez,sophia.m@university.edu,+15550104,+15550196
105,Noah Centineo,noah.c@university.edu,+15550105,+15550195
        """.trimIndent()
    }
}
