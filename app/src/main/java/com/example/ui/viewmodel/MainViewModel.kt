package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.model.AttendanceRecordEntity
import com.example.data.model.AttendanceStatus
import com.example.data.model.ClassEntity
import com.example.data.model.StaffProfile
import com.example.data.model.StudentEntity
import com.example.data.model.SyncLogEntity
import com.example.data.repository.AttendanceRepository
import com.example.util.ColumnMapping
import com.example.util.CsvParser
import com.example.util.ParsedCsvData
import com.example.util.ReportExporter
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: AttendanceRepository

    val allClasses: StateFlow<List<ClassEntity>>
    val allStudents: StateFlow<List<StudentEntity>>
    val syncLogs: StateFlow<List<SyncLogEntity>>
    val currentStaff: StateFlow<StaffProfile>
    val isRealtimeSyncEnabled: StateFlow<Boolean>

    // Active Selection State
    val selectedClassId = kotlinx.coroutines.flow.MutableStateFlow<Long?>(null)
    val selectedDate = kotlinx.coroutines.flow.MutableStateFlow(
        LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
    )
    val selectedSessionName = kotlinx.coroutines.flow.MutableStateFlow("Regular Session")
    val searchQuery = kotlinx.coroutines.flow.MutableStateFlow("")

    // Selected Student for Individual Report Screen
    val selectedStudentId = kotlinx.coroutines.flow.MutableStateFlow<Long?>(null)

    // Bulk Import state
    val rawCsvInput = kotlinx.coroutines.flow.MutableStateFlow("")
    val parsedCsvData = kotlinx.coroutines.flow.MutableStateFlow<ParsedCsvData?>(null)
    val columnMapping = kotlinx.coroutines.flow.MutableStateFlow(ColumnMapping())
    val importSuccessMessage = kotlinx.coroutines.flow.MutableStateFlow<String?>(null)

    // Export/Preview State
    val generatedReportContent = kotlinx.coroutines.flow.MutableStateFlow<String?>(null)
    val generatedReportType = kotlinx.coroutines.flow.MutableStateFlow<String?>(null) // "CSV" or "PDF_HTML"

    init {
        val database = AppDatabase.getDatabase(application)
        repository = AttendanceRepository(database.attendanceDao())

        allClasses = repository.allClasses.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            emptyList()
        )

        allStudents = repository.allStudents.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            emptyList()
        )

        syncLogs = repository.syncLogs.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            emptyList()
        )

        currentStaff = repository.currentStaff
        isRealtimeSyncEnabled = repository.isRealtimeSyncEnabled

        viewModelScope.launch {
            repository.seedSampleDataIfEmpty()
            // Default select first class
            val classes = repository.allClasses.first()
            if (classes.isNotEmpty() && selectedClassId.value == null) {
                selectedClassId.value = classes.first().id
            }
        }
    }

    // Active Class Students
    val activeClassStudents: StateFlow<List<StudentEntity>> = combine(
        allStudents,
        selectedClassId,
        searchQuery
    ) { students, classId, query ->
        if (classId == null) emptyList()
        else {
            students.filter { it.classId == classId }
                .filter {
                    query.isBlank() ||
                            it.name.contains(query, ignoreCase = true) ||
                            it.rollNumber.contains(query, ignoreCase = true)
                }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Active Attendance Records
    val activeAttendanceRecords = kotlinx.coroutines.flow.MutableStateFlow<List<AttendanceRecordEntity>>(emptyList())

    fun loadSessionAttendance() {
        val classId = selectedClassId.value ?: return
        val date = selectedDate.value
        val session = selectedSessionName.value

        viewModelScope.launch {
            repository.getAttendanceForSession(classId, date, session).collect { records ->
                activeAttendanceRecords.value = records
            }
        }
    }

    fun selectClass(classId: Long) {
        selectedClassId.value = classId
        loadSessionAttendance()
    }

    fun setDate(date: String) {
        selectedDate.value = date
        loadSessionAttendance()
    }

    fun setSession(session: String) {
        selectedSessionName.value = session
        loadSessionAttendance()
    }

    fun setStaffProfile(profile: StaffProfile) {
        repository.setStaffProfile(profile)
    }

    fun toggleRealtimeSync(enabled: Boolean) {
        repository.toggleRealtimeSync(enabled)
    }

    fun updateStudentAttendanceStatus(studentId: Long, newStatus: AttendanceStatus, remarks: String = "") {
        val classId = selectedClassId.value ?: return
        val date = selectedDate.value
        val session = selectedSessionName.value

        val existing = activeAttendanceRecords.value.find { it.studentId == studentId }
        val record = existing?.copy(status = newStatus, remarks = remarks)
            ?: AttendanceRecordEntity(
                classId = classId,
                studentId = studentId,
                date = date,
                sessionName = session,
                status = newStatus,
                remarks = remarks
            )

        viewModelScope.launch {
            repository.saveAttendanceRecord(record)
            loadSessionAttendance()
        }
    }

    fun markAllStudents(status: AttendanceStatus) {
        val classId = selectedClassId.value ?: return
        val date = selectedDate.value
        val session = selectedSessionName.value
        val students = activeClassStudents.value

        val records = students.map { student ->
            AttendanceRecordEntity(
                classId = classId,
                studentId = student.id,
                date = date,
                sessionName = session,
                status = status
            )
        }

        viewModelScope.launch {
            repository.saveAttendanceRecordsBulk(records)
            loadSessionAttendance()
        }
    }

    fun addClass(name: String, subject: String, roomNumber: String, year: String) {
        viewModelScope.launch {
            val newId = repository.insertClass(
                ClassEntity(
                    name = name,
                    subject = subject,
                    roomNumber = roomNumber,
                    academicYear = year
                )
            )
            selectedClassId.value = newId
        }
    }

    fun addStudentToClass(rollNumber: String, name: String, email: String, phone: String, parentPhone: String) {
        val classId = selectedClassId.value ?: return
        viewModelScope.launch {
            repository.insertStudent(
                StudentEntity(
                    classId = classId,
                    rollNumber = rollNumber,
                    name = name,
                    email = email,
                    phone = phone,
                    parentPhone = parentPhone
                )
            )
        }
    }

    fun deleteStudent(student: StudentEntity) {
        viewModelScope.launch {
            repository.deleteStudent(student)
        }
    }

    // CSV / Excel Parsing Methods
    fun parseCsvInput(text: String) {
        rawCsvInput.value = text
        val rawData = CsvParser.parseRawText(text)
        parsedCsvData.value = rawData
        val autoMapping = CsvParser.autoDetectMapping(rawData.headers)
        columnMapping.value = autoMapping
    }

    fun updateMapping(mapping: ColumnMapping) {
        columnMapping.value = mapping
    }

    fun executeBulkImport(targetClassId: Long) {
        val rawData = parsedCsvData.value ?: return
        val mapping = columnMapping.value
        val studentList = CsvParser.mapToStudents(rawData.rows, mapping, targetClassId)

        viewModelScope.launch {
            repository.insertStudentsBulk(studentList)
            importSuccessMessage.value = "Successfully imported ${studentList.size} students into class!"
            // Reset CSV state
            rawCsvInput.value = ""
            parsedCsvData.value = null
        }
    }

    fun loadSampleCsvTemplate() {
        val template = CsvParser.generateSampleCsvTemplate()
        parseCsvInput(template)
    }

    // Report Generation
    fun generateClassCsvReport(classEntity: ClassEntity) {
        viewModelScope.launch {
            val students = repository.getStudentsForClassSync(classEntity.id)
            val records = repository.getAllAttendanceForClassSync(classEntity.id)
            val csv = ReportExporter.generateClassAttendanceCsv(classEntity, students, records)
            generatedReportContent.value = csv
            generatedReportType.value = "CSV"
        }
    }

    fun generateClassPdfHtmlReport(classEntity: ClassEntity) {
        viewModelScope.launch {
            val students = repository.getStudentsForClassSync(classEntity.id)
            val records = repository.getAllAttendanceForClassSync(classEntity.id)
            val html = ReportExporter.generateHtmlPdfReport(
                title = "Class Attendance Summary & Analytics",
                classEntity = classEntity,
                students = students,
                records = records
            )
            generatedReportContent.value = html
            generatedReportType.value = "PDF_HTML"
        }
    }

    fun generateStudentCsvReport(student: StudentEntity, classEntity: ClassEntity?) {
        viewModelScope.launch {
            val records = repository.getAttendanceForStudentSync(student.id)
            val csv = ReportExporter.generateStudentReportCsv(student, classEntity, records)
            generatedReportContent.value = csv
            generatedReportType.value = "CSV"
        }
    }

    fun triggerSimulatedStaffSync() {
        viewModelScope.launch {
            repository.logSync("MANUAL_SYNC", "Triggered cloud real-time delta sync across all active staff clients")
        }
    }
}
