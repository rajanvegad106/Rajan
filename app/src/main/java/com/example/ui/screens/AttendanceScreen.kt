package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Comment
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AttendanceStatus
import com.example.data.model.StudentEntity
import com.example.ui.components.SearchHeaderBar
import com.example.ui.theme.AbsentRed
import com.example.ui.theme.AbsentRedContainer
import com.example.ui.theme.ExcusedBlue
import com.example.ui.theme.ExcusedBlueContainer
import com.example.ui.theme.LateAmber
import com.example.ui.theme.LateAmberContainer
import com.example.ui.theme.PresentGreen
import com.example.ui.theme.PresentGreenContainer
import com.example.ui.viewmodel.MainViewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
fun AttendanceScreen(viewModel: MainViewModel) {
    val classes by viewModel.allClasses.collectAsState()
    val selectedClassId by viewModel.selectedClassId.collectAsState()
    val selectedDate by viewModel.selectedDate.collectAsState()
    val selectedSession by viewModel.selectedSessionName.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()

    val students by viewModel.activeClassStudents.collectAsState()
    val attendanceRecords by viewModel.activeAttendanceRecords.collectAsState()

    val activeClass = classes.find { it.id == selectedClassId }

    LaunchedEffect(selectedClassId, selectedDate, selectedSession) {
        viewModel.loadSessionAttendance()
    }

    val presentCount = attendanceRecords.count { it.status == AttendanceStatus.PRESENT || it.status == AttendanceStatus.EXCUSED }
    val absentCount = attendanceRecords.count { it.status == AttendanceStatus.ABSENT }
    val lateCount = attendanceRecords.count { it.status == AttendanceStatus.LATE }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = activeClass?.name ?: "Select Class",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        )
                        Text(
                            text = activeClass?.subject ?: "No subject",
                            style = MaterialTheme.typography.bodySmall.copy(
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                            )
                        )
                    }

                    Surface(
                        color = MaterialTheme.colorScheme.primary,
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Schedule, contentDescription = null, tint = Color.White, modifier = Modifier.size(12.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(selectedSession, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val todayStr = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
                    val yesterdayStr = LocalDate.now().minusDays(1).format(DateTimeFormatter.ISO_LOCAL_DATE)

                    DateFilterChip(
                        label = "Today",
                        isSelected = selectedDate == todayStr,
                        onClick = { viewModel.setDate(todayStr) }
                    )
                    DateFilterChip(
                        label = "Yesterday",
                        isSelected = selectedDate == yesterdayStr,
                        onClick = { viewModel.setDate(yesterdayStr) }
                    )
                    DateFilterChip(
                        label = selectedDate,
                        isSelected = selectedDate != todayStr && selectedDate != yesterdayStr,
                        onClick = { }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                CounterBadge("Present", presentCount, PresentGreenContainer, PresentGreen)
                CounterBadge("Absent", absentCount, AbsentRedContainer, AbsentRed)
                CounterBadge("Late", lateCount, LateAmberContainer, LateAmber)
            }

            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                TextButton(
                    onClick = { viewModel.markAllStudents(AttendanceStatus.PRESENT) },
                    modifier = Modifier.testTag("mark_all_present_btn")
                ) {
                    Text("All Present", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = PresentGreen)
                }
                TextButton(
                    onClick = { viewModel.markAllStudents(AttendanceStatus.ABSENT) },
                    modifier = Modifier.testTag("mark_all_absent_btn")
                ) {
                    Text("All Absent", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = AbsentRed)
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        SearchHeaderBar(
            query = searchQuery,
            onQueryChange = { viewModel.searchQuery.value = it }
        )

        Spacer(modifier = Modifier.height(8.dp))

        if (students.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text("No students in this class. Add students from 'Classes' tab.")
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(students) { student ->
                    val record = attendanceRecords.find { it.studentId == student.id }
                    val currentStatus = record?.status ?: AttendanceStatus.PRESENT

                    StudentAttendanceMarkCard(
                        student = student,
                        currentStatus = currentStatus,
                        remarks = record?.remarks ?: "",
                        markedBy = record?.markedByStaff ?: "",
                        onStatusChange = { newStatus, remarks ->
                            viewModel.updateStudentAttendanceStatus(student.id, newStatus, remarks)
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun DateFilterChip(label: String, isSelected: Boolean, onClick: () -> Unit) {
    Surface(
        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(16.dp),
        border = if (!isSelected) androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline) else null,
        modifier = Modifier.clickable { onClick() }
    ) {
        Text(
            text = label,
            color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}

@Composable
fun CounterBadge(label: String, count: Int, bgColor: Color, textColor: Color) {
    Surface(
        color = bgColor,
        shape = RoundedCornerShape(8.dp)
    ) {
        Text(
            text = "$label: $count",
            color = textColor,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 10.sp),
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

@Composable
fun StudentAttendanceMarkCard(
    student: StudentEntity,
    currentStatus: AttendanceStatus,
    remarks: String,
    markedBy: String,
    onStatusChange: (AttendanceStatus, String) -> Unit
) {
    var showRemarkInput by remember { mutableStateOf(false) }
    var currentRemarkText by remember(remarks) { mutableStateOf(remarks) }

    Card(
        colors = CardDefaults.cardColors(
            containerColor = when (currentStatus) {
                AttendanceStatus.PRESENT -> MaterialTheme.colorScheme.surface
                AttendanceStatus.ABSENT -> AbsentRedContainer.copy(alpha = 0.25f)
                AttendanceStatus.LATE -> LateAmberContainer.copy(alpha = 0.25f)
                AttendanceStatus.EXCUSED -> ExcusedBlueContainer.copy(alpha = 0.25f)
            }
        ),
        shape = RoundedCornerShape(10.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("student_attendance_row_${student.id}")
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = student.rollNumber,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Column {
                        Text(
                            text = student.name,
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        if (markedBy.isNotBlank()) {
                            Text(
                                text = "Marked by: $markedBy",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                )
                            )
                        }
                    }
                }

                IconButton(
                    onClick = { showRemarkInput = !showRemarkInput },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        Icons.Default.Comment,
                        contentDescription = "Remark",
                        tint = if (remarks.isNotBlank()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                StatusToggleButton(
                    label = "Present",
                    isSelected = currentStatus == AttendanceStatus.PRESENT,
                    activeColor = PresentGreen,
                    modifier = Modifier.weight(1f),
                    onClick = { onStatusChange(AttendanceStatus.PRESENT, currentRemarkText) }
                )
                StatusToggleButton(
                    label = "Absent",
                    isSelected = currentStatus == AttendanceStatus.ABSENT,
                    activeColor = AbsentRed,
                    modifier = Modifier.weight(1f),
                    onClick = { onStatusChange(AttendanceStatus.ABSENT, currentRemarkText) }
                )
                StatusToggleButton(
                    label = "Late",
                    isSelected = currentStatus == AttendanceStatus.LATE,
                    activeColor = LateAmber,
                    modifier = Modifier.weight(1f),
                    onClick = { onStatusChange(AttendanceStatus.LATE, currentRemarkText) }
                )
                StatusToggleButton(
                    label = "Excused",
                    isSelected = currentStatus == AttendanceStatus.EXCUSED,
                    activeColor = ExcusedBlue,
                    modifier = Modifier.weight(1f),
                    onClick = { onStatusChange(AttendanceStatus.EXCUSED, currentRemarkText) }
                )
            }

            AnimatedVisibility(visible = showRemarkInput) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = currentRemarkText,
                        onValueChange = { currentRemarkText = it },
                        placeholder = { Text("Add remark (e.g. medical leave)", fontSize = 11.sp) },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Button(
                        onClick = {
                            onStatusChange(currentStatus, currentRemarkText)
                            showRemarkInput = false
                        }
                    ) {
                        Text("Save", fontSize = 10.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun StatusToggleButton(
    label: String,
    isSelected: Boolean,
    activeColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        color = if (isSelected) activeColor else MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(8.dp),
        border = if (!isSelected) androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)) else null,
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable { onClick() }
    ) {
        Box(
            modifier = Modifier.padding(vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                    fontSize = 11.sp
                )
            )
        }
    }
}
