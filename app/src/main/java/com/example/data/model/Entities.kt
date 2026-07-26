package com.example.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "classes")
data class ClassEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val subject: String,
    val roomNumber: String = "",
    val academicYear: String = "2025-2026",
    val createdTimestamp: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "students",
    foreignKeys = [
        ForeignKey(
            entity = ClassEntity::class,
            parentColumns = ["id"],
            childColumns = ["classId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("classId")]
)
data class StudentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val classId: Long,
    val rollNumber: String,
    val name: String,
    val email: String = "",
    val phone: String = "",
    val parentPhone: String = "",
    val avatarUrl: String? = null
)

enum class AttendanceStatus {
    PRESENT, ABSENT, LATE, EXCUSED
}

@Entity(
    tableName = "attendance_records",
    foreignKeys = [
        ForeignKey(
            entity = ClassEntity::class,
            parentColumns = ["id"],
            childColumns = ["classId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = StudentEntity::class,
            parentColumns = ["id"],
            childColumns = ["studentId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("classId"),
        Index("studentId"),
        Index(value = ["studentId", "date", "sessionName"], unique = true)
    ]
)
data class AttendanceRecordEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val classId: Long,
    val studentId: Long,
    val date: String, // Format YYYY-MM-DD
    val sessionName: String = "Regular Session",
    val status: AttendanceStatus,
    val remarks: String = "",
    val markedByStaff: String = "Prof. Sarah Miller",
    val updatedTimestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "sync_logs")
data class SyncLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val staffName: String,
    val actionType: String,
    val details: String,
    val timestamp: Long = System.currentTimeMillis(),
    val status: String = "SUCCESS"
)

data class StaffProfile(
    val id: String,
    val name: String,
    val role: String,
    val avatarColorHex: String
)
