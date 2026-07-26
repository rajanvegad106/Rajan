package com.example.data.repository

import com.example.data.local.AttendanceDao
import com.example.data.model.AttendanceRecordEntity
import com.example.data.model.ClassEntity
import com.example.data.model.StaffProfile
import com.example.data.model.StudentEntity
import com.example.data.model.SyncLogEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

class AttendanceRepository(private val dao: AttendanceDao) {

    val allClasses: Flow<List<ClassEntity>> = dao.getAllClasses()
    val allStudents: Flow<List<StudentEntity>> = dao.getAllStudents()
    val syncLogs: Flow<List<SyncLogEntity>> = dao.getSyncLogs()

    private val _currentStaff = MutableStateFlow(
        StaffProfile("staff_1", "Prof. Sarah Miller", "Lead Instructor", "#4F46E5")
    )
    val currentStaff: StateFlow<StaffProfile> = _currentStaff.asStateFlow()

    private val _isRealtimeSyncEnabled = MutableStateFlow(true)
    val isRealtimeSyncEnabled: StateFlow<Boolean> = _isRealtimeSyncEnabled.asStateFlow()

    fun setStaffProfile(profile: StaffProfile) {
        _currentStaff.value = profile
    }

    fun updateStaffName(name: String) {
        _currentStaff.value = _currentStaff.value.copy(name = name)
    }

    fun toggleRealtimeSync(enabled: Boolean) {
        _isRealtimeSyncEnabled.value = enabled
    }

    fun getStudentsForClass(classId: Long): Flow<List<StudentEntity>> {
        return dao.getStudentsForClass(classId)
    }

    suspend fun getStudentsForClassSync(classId: Long): List<StudentEntity> {
        return dao.getStudentsForClassSync(classId)
    }

    fun getAttendanceForSession(classId: Long, date: String, sessionName: String): Flow<List<AttendanceRecordEntity>> {
        return dao.getAttendanceForSession(classId, date, sessionName)
    }

    fun getAttendanceForStudent(studentId: Long): Flow<List<AttendanceRecordEntity>> {
        return dao.getAttendanceForStudent(studentId)
    }

    suspend fun getAttendanceForStudentSync(studentId: Long): List<AttendanceRecordEntity> {
        return dao.getAttendanceForStudentSync(studentId)
    }

    fun getAllAttendanceForClass(classId: Long): Flow<List<AttendanceRecordEntity>> {
        return dao.getAllAttendanceForClass(classId)
    }

    suspend fun getAllAttendanceForClassSync(classId: Long): List<AttendanceRecordEntity> {
        return dao.getAllAttendanceForClassSync(classId)
    }

    suspend fun getClassById(classId: Long): ClassEntity? {
        return dao.getClassById(classId)
    }

    suspend fun getStudentById(studentId: Long): StudentEntity? {
        return dao.getStudentById(studentId)
    }

    suspend fun insertClass(classEntity: ClassEntity): Long {
        val id = dao.insertClass(classEntity)
        logSync("CREATE_CLASS", "Created class '${classEntity.name}' (${classEntity.subject})")
        return id
    }

    suspend fun updateClass(classEntity: ClassEntity) {
        dao.updateClass(classEntity)
        logSync("UPDATE_CLASS", "Updated class '${classEntity.name}'")
    }

    suspend fun deleteClass(classEntity: ClassEntity) {
        dao.deleteClass(classEntity)
        logSync("DELETE_CLASS", "Deleted class '${classEntity.name}'")
    }

    suspend fun insertStudent(student: StudentEntity): Long {
        val id = dao.insertStudent(student)
        logSync("ADD_STUDENT", "Added student ${student.name} (Roll: ${student.rollNumber})")
        return id
    }

    suspend fun insertStudentsBulk(students: List<StudentEntity>): List<Long> {
        val ids = dao.insertStudents(students)
        logSync("BULK_IMPORT_STUDENTS", "Imported ${students.size} students via CSV/Excel")
        return ids
    }

    suspend fun updateStudent(student: StudentEntity) {
        dao.updateStudent(student)
        logSync("UPDATE_STUDENT", "Updated student ${student.name}")
    }

    suspend fun deleteStudent(student: StudentEntity) {
        dao.deleteStudent(student)
        logSync("DELETE_STUDENT", "Removed student ${student.name}")
    }

    suspend fun saveAttendanceRecord(record: AttendanceRecordEntity) {
        val updated = record.copy(
            markedByStaff = _currentStaff.value.name,
            updatedTimestamp = System.currentTimeMillis()
        )
        dao.insertAttendanceRecord(updated)
        logSync("MARK_ATTENDANCE", "Marked ${updated.status} for student ID ${updated.studentId} on ${updated.date}")
    }

    suspend fun saveAttendanceRecordsBulk(records: List<AttendanceRecordEntity>) {
        val staffName = _currentStaff.value.name
        val now = System.currentTimeMillis()
        val updatedRecords = records.map {
            it.copy(markedByStaff = staffName, updatedTimestamp = now)
        }
        dao.insertAttendanceRecords(updatedRecords)
        if (records.isNotEmpty()) {
            val sample = records.first()
            logSync("SESSION_ATTENDANCE", "Saved session '${sample.sessionName}' attendance (${records.size} students) for date ${sample.date}")
        }
    }

    suspend fun logSync(actionType: String, details: String) {
        dao.insertSyncLog(
            SyncLogEntity(
                staffName = _currentStaff.value.name,
                actionType = actionType,
                details = details,
                timestamp = System.currentTimeMillis()
            )
        )
    }

    suspend fun seedSampleDataIfEmpty() = withContext(Dispatchers.IO) {
        val existingClasses = dao.getClassById(1)
        if (existingClasses == null) {
            // Seed classes
            val class1Id = dao.insertClass(
                ClassEntity(
                    name = "CS-101",
                    subject = "Computer Science Foundations",
                    roomNumber = "Lab 3B",
                    academicYear = "2025-2026"
                )
            )
            val class2Id = dao.insertClass(
                ClassEntity(
                    name = "MATH-202",
                    subject = "Advanced Linear Algebra",
                    roomNumber = "Hall 104",
                    academicYear = "2025-2026"
                )
            )
            val class3Id = dao.insertClass(
                ClassEntity(
                    name = "ENG-105",
                    subject = "Technical Communication",
                    roomNumber = "Room 201",
                    academicYear = "2025-2026"
                )
            )

            // Seed students for CS-101
            val csStudents = listOf(
                StudentEntity(classId = class1Id, rollNumber = "101", name = "Alex Johnson", email = "alex.j@univ.edu", phone = "+1 555-0101", parentPhone = "+1 555-9001"),
                StudentEntity(classId = class1Id, rollNumber = "102", name = "Sophia Chen", email = "sophia.c@univ.edu", phone = "+1 555-0102", parentPhone = "+1 555-9002"),
                StudentEntity(classId = class1Id, rollNumber = "103", name = "Marcus Vance", email = "marcus.v@univ.edu", phone = "+1 555-0103", parentPhone = "+1 555-9003"),
                StudentEntity(classId = class1Id, rollNumber = "104", name = "Emma Watson", email = "emma.w@univ.edu", phone = "+1 555-0104", parentPhone = "+1 555-9004"),
                StudentEntity(classId = class1Id, rollNumber = "105", name = "David Miller", email = "david.m@univ.edu", phone = "+1 555-0105", parentPhone = "+1 555-9005"),
                StudentEntity(classId = class1Id, rollNumber = "106", name = "Olivia Taylor", email = "olivia.t@univ.edu", phone = "+1 555-0106", parentPhone = "+1 555-9006"),
                StudentEntity(classId = class1Id, rollNumber = "107", name = "Ethan Davis", email = "ethan.d@univ.edu", phone = "+1 555-0107", parentPhone = "+1 555-9007")
            )
            val csStudentIds = dao.insertStudents(csStudents)

            // Seed students for MATH-202
            val mathStudents = listOf(
                StudentEntity(classId = class2Id, rollNumber = "201", name = "Liam Smith", email = "liam.s@univ.edu", phone = "+1 555-0201", parentPhone = "+1 555-9201"),
                StudentEntity(classId = class2Id, rollNumber = "202", name = "Ava Martinez", email = "ava.m@univ.edu", phone = "+1 555-0202", parentPhone = "+1 555-9202"),
                StudentEntity(classId = class2Id, rollNumber = "203", name = "Noah Wilson", email = "noah.w@univ.edu", phone = "+1 555-0203", parentPhone = "+1 555-9203"),
                StudentEntity(classId = class2Id, rollNumber = "204", name = "Isabella Anderson", email = "isabella.a@univ.edu", phone = "+1 555-0204", parentPhone = "+1 555-9204")
            )
            dao.insertStudents(mathStudents)

            // Seed historical attendance for past 5 days
            val today = java.time.LocalDate.now()
            val formatter = java.time.format.DateTimeFormatter.ISO_LOCAL_DATE
            val staffList = listOf("Prof. Sarah Miller", "Dr. Robert Chen", "Alex Vance (TA)")

            for (i in 0..6) {
                val pastDate = today.minusDays(i.toLong()).format(formatter)
                val staff = staffList[i % staffList.size]

                csStudentIds.forEachIndexed { index, sId ->
                    val status = when {
                        index == 2 && i % 2 == 0 -> com.example.data.model.AttendanceStatus.ABSENT
                        index == 4 && i == 1 -> com.example.data.model.AttendanceStatus.LATE
                        index == 5 && i == 3 -> com.example.data.model.AttendanceStatus.EXCUSED
                        else -> com.example.data.model.AttendanceStatus.PRESENT
                    }
                    dao.insertAttendanceRecord(
                        AttendanceRecordEntity(
                            classId = class1Id,
                            studentId = sId,
                            date = pastDate,
                            sessionName = "Regular Lecture",
                            status = status,
                            remarks = if (status == com.example.data.model.AttendanceStatus.ABSENT) "Medical leave" else "",
                            markedByStaff = staff,
                            updatedTimestamp = System.currentTimeMillis() - (i * 86400000)
                        )
                    )
                }
            }

            // Sync logs
            dao.insertSyncLog(SyncLogEntity(staffName = "Prof. Sarah Miller", actionType = "INITIAL_SYNC", details = "Database seeded & synchronized across staff cluster", status = "SUCCESS"))
            dao.insertSyncLog(SyncLogEntity(staffName = "Dr. Robert Chen", actionType = "REALTIME_POLL", details = "Synced 35 student attendance entries for CS-101", status = "SUCCESS"))
        }
    }
}
