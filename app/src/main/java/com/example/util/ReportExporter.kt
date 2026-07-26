package com.example.util

import com.example.data.model.AttendanceRecordEntity
import com.example.data.model.ClassEntity
import com.example.data.model.StudentEntity

object ReportExporter {

    fun generateClassAttendanceCsv(
        classEntity: ClassEntity,
        students: List<StudentEntity>,
        records: List<AttendanceRecordEntity>
    ): String {
        val sb = StringBuilder()
        sb.append("Class: ${classEntity.name} - ${classEntity.subject}\n")
        sb.append("Room: ${classEntity.roomNumber} | Academic Year: ${classEntity.academicYear}\n")
        sb.append("Export Date: ${java.time.LocalDate.now()}\n\n")

        val distinctDates = records.map { it.date }.distinct().sorted()
        sb.append("Roll No,Student Name")
        distinctDates.forEach { date ->
            sb.append(",$date")
        }
        sb.append(",Total Present,Total Absent,Total Late,Attendance %\n")

        students.forEach { student ->
            sb.append("\"${student.rollNumber}\",\"${student.name}\"")
            var presentCount = 0
            var absentCount = 0
            var lateCount = 0
            val studentRecords = records.filter { it.studentId == student.id }

            distinctDates.forEach { date ->
                val rec = studentRecords.find { it.date == date }
                val statusText = when (rec?.status) {
                    com.example.data.model.AttendanceStatus.PRESENT -> { presentCount++; "P" }
                    com.example.data.model.AttendanceStatus.ABSENT -> { absentCount++; "A" }
                    com.example.data.model.AttendanceStatus.LATE -> { lateCount++; "L" }
                    com.example.data.model.AttendanceStatus.EXCUSED -> { presentCount++; "E" }
                    null -> "-"
                }
                sb.append(",$statusText")
            }

            val totalSessions = studentRecords.size
            val percentage = if (totalSessions > 0) ((presentCount.toDouble() / totalSessions) * 100).toInt() else 0

            sb.append(",$presentCount,$absentCount,$lateCount,$percentage%\n")
        }

        return sb.toString()
    }

    fun generateStudentReportCsv(
        student: StudentEntity,
        classEntity: ClassEntity?,
        records: List<AttendanceRecordEntity>
    ): String {
        val sb = StringBuilder()
        sb.append("STUDENT ATTENDANCE REPORT\n")
        sb.append("Student Name: ${student.name}\n")
        sb.append("Roll Number: ${student.rollNumber}\n")
        sb.append("Class: ${classEntity?.name ?: "N/A"} (${classEntity?.subject ?: "N/A"})\n")
        sb.append("Contact: ${student.phone} | Parent Contact: ${student.parentPhone}\n\n")

        val total = records.size
        val present = records.count { it.status == com.example.data.model.AttendanceStatus.PRESENT || it.status == com.example.data.model.AttendanceStatus.EXCUSED }
        val absent = records.count { it.status == com.example.data.model.AttendanceStatus.ABSENT }
        val late = records.count { it.status == com.example.data.model.AttendanceStatus.LATE }
        val pct = if (total > 0) ((present.toDouble() / total) * 100).toInt() else 0

        sb.append("SUMMARY STATS\n")
        sb.append("Total Sessions,$total\n")
        sb.append("Present,$present\n")
        sb.append("Absent,$absent\n")
        sb.append("Late,$late\n")
        sb.append("Attendance Rate,$pct%\n\n")

        sb.append("Date,Session Name,Status,Marked By Staff,Remarks\n")
        records.sortedByDescending { it.date }.forEach { rec ->
            sb.append("\"${rec.date}\",\"${rec.sessionName}\",\"${rec.status}\",\"${rec.markedByStaff}\",\"${rec.remarks}\"\n")
        }

        return sb.toString()
    }

    fun generateHtmlPdfReport(
        title: String,
        classEntity: ClassEntity?,
        students: List<StudentEntity>,
        records: List<AttendanceRecordEntity>
    ): String {
        val totalSessions = records.map { it.date }.distinct().size
        val presentCount = records.count { it.status == com.example.data.model.AttendanceStatus.PRESENT || it.status == com.example.data.model.AttendanceStatus.EXCUSED }
        val absentCount = records.count { it.status == com.example.data.model.AttendanceStatus.ABSENT }
        val lateCount = records.count { it.status == com.example.data.model.AttendanceStatus.LATE }
        val totalCount = records.size
        val overallPct = if (totalCount > 0) ((presentCount.toDouble() / totalCount) * 100).toInt() else 0

        val studentRowsHtml = students.map { student ->
            val sRecords = records.filter { it.studentId == student.id }
            val sPresent = sRecords.count { it.status == com.example.data.model.AttendanceStatus.PRESENT || it.status == com.example.data.model.AttendanceStatus.EXCUSED }
            val sAbsent = sRecords.count { it.status == com.example.data.model.AttendanceStatus.ABSENT }
            val sLate = sRecords.count { it.status == com.example.data.model.AttendanceStatus.LATE }
            val sTotal = sRecords.size
            val sPct = if (sTotal > 0) ((sPresent.toDouble() / sTotal) * 100).toInt() else 0
            val statusBadge = if (sPct >= 75) "<span class='badge bg-green'>GOOD ($sPct%)</span>" else "<span class='badge bg-red'>LOW ($sPct%)</span>"

            """
            <tr>
                <td><b>${student.rollNumber}</b></td>
                <td>${student.name}</td>
                <td>$sPresent</td>
                <td>$sAbsent</td>
                <td>$sLate</td>
                <td>$sTotal</td>
                <td>$statusBadge</td>
            </tr>
            """.trimIndent()
        }.joinToString("\n")

        return """
        <!DOCTYPE html>
        <html>
        <head>
            <meta charset="utf-8"/>
            <style>
                body { font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; padding: 20px; color: #1e293b; }
                .header { border-bottom: 2px solid #4f46e5; padding-bottom: 12px; margin-bottom: 20px; }
                .header h1 { color: #4f46e5; margin: 0; font-size: 24px; }
                .header p { margin: 4px 0 0 0; color: #64748b; font-size: 14px; }
                .grid { display: flex; gap: 12px; margin-bottom: 20px; }
                .card { background: #f8fafc; border: 1px solid #e2e8f0; border-radius: 8px; padding: 12px 16px; flex: 1; text-align: center; }
                .card .num { font-size: 20px; font-weight: bold; color: #1e293b; }
                .card .lbl { font-size: 12px; color: #64748b; }
                table { width: 100%; border-collapse: collapse; margin-top: 10px; }
                th, td { border: 1px solid #cbd5e1; padding: 8px 12px; text-align: left; font-size: 13px; }
                th { background-color: #EEF2FF; color: #3730A3; font-weight: 600; }
                tr:nth-child(even) { background-color: #F8FAFC; }
                .badge { padding: 3px 8px; border-radius: 12px; font-size: 11px; font-weight: bold; }
                .bg-green { background: #DCFCE7; color: #166534; }
                .bg-red { background: #FEE2E2; color: #991B1B; }
                .footer { margin-top: 30px; font-size: 11px; color: #94a3b8; text-align: center; }
            </style>
        </head>
        <body>
            <div class="header">
                <h1>$title</h1>
                <p>Class: ${classEntity?.name ?: "All Classes"} | Subject: ${classEntity?.subject ?: "-"} | Generated on ${java.time.LocalDate.now()}</p>
            </div>

            <div class="grid">
                <div class="card"><div class="num">${students.size}</div><div class="lbl">Total Students</div></div>
                <div class="card"><div class="num">$totalSessions</div><div class="lbl">Total Sessions</div></div>
                <div class="card"><div class="num" style="color: #16a34a;">$presentCount</div><div class="lbl">Total Present</div></div>
                <div class="card"><div class="num" style="color: #dc2626;">$absentCount</div><div class="lbl">Total Absent</div></div>
                <div class="card"><div class="num" style="color: #4f46e5;">$overallPct%</div><div class="lbl">Avg Attendance</div></div>
            </div>

            <h3>Student Summary Roster</h3>
            <table>
                <thead>
                    <tr>
                        <th>Roll No</th>
                        <th>Student Name</th>
                        <th>Present</th>
                        <th>Absent</th>
                        <th>Late</th>
                        <th>Sessions</th>
                        <th>Status</th>
                    </tr>
                </thead>
                <tbody>
                    $studentRowsHtml
                </tbody>
            </table>

            <div class="footer">
                Generated automatically by Student Attendance Real-Time Staff Portal System
            </div>
        </body>
        </html>
        """.trimIndent()
    }
}
