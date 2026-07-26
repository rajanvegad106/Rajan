package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Class
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.ui.components.AppHeaderBanner
import com.example.ui.screens.AttendanceScreen
import com.example.ui.screens.BulkImportScreen
import com.example.ui.screens.ClassReportScreen
import com.example.ui.screens.ClassesScreen
import com.example.ui.screens.MultiStaffSyncScreen
import com.example.ui.screens.StudentReportScreen
import com.example.ui.theme.StudentAttendanceTheme
import com.example.ui.viewmodel.MainViewModel

sealed class NavigationItem(val route: String, val title: String, val icon: ImageVector) {
    object Classes : NavigationItem("classes", "Classes", Icons.Default.Class)
    object Attendance : NavigationItem("attendance", "Attendance", Icons.Default.CheckCircle)
    object StudentReport : NavigationItem("student_report", "Student Report", Icons.Default.Person)
    object ClassReport : NavigationItem("class_report", "Reports", Icons.Default.Assessment)
    object BulkImport : NavigationItem("bulk_import", "Bulk Import", Icons.Default.FileUpload)
    object MultiStaffSync : NavigationItem("multi_staff", "Staff Sync", Icons.Default.Sync)
}

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            StudentAttendanceTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AttendanceMainApp(viewModel = viewModel)
                }
            }
        }
    }
}

@Composable
fun AttendanceMainApp(viewModel: MainViewModel) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: NavigationItem.Attendance.route

    val currentStaff by viewModel.currentStaff.collectAsState()
    val isSyncActive by viewModel.isRealtimeSyncEnabled.collectAsState()

    val bottomNavItems = listOf(
        NavigationItem.Classes,
        NavigationItem.Attendance,
        NavigationItem.StudentReport,
        NavigationItem.ClassReport,
        NavigationItem.BulkImport,
        NavigationItem.MultiStaffSync
    )

    Scaffold(
        topBar = {
            AppHeaderBanner(
                title = "Student Attendance Portal",
                subtitle = when (currentRoute) {
                    NavigationItem.Classes.route -> "Manage classes & student rosters"
                    NavigationItem.Attendance.route -> "Mark & sync live session attendance"
                    NavigationItem.StudentReport.route -> "Particular student performance report"
                    NavigationItem.ClassReport.route -> "Class-wide attendance trends & alerts"
                    NavigationItem.BulkImport.route -> "Import students via Excel/CSV file"
                    NavigationItem.MultiStaffSync.route -> "Multi-staff real-time synchronization"
                    else -> "Student Attendance System"
                },
                staff = currentStaff,
                isSyncActive = isSyncActive,
                onStaffClick = {
                    navController.navigate(NavigationItem.MultiStaffSync.route)
                }
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                bottomNavItems.forEach { screen ->
                    val isSelected = currentRoute == screen.route
                    NavigationBarItem(
                        selected = isSelected,
                        onClick = {
                            if (currentRoute != screen.route) {
                                navController.navigate(screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        },
                        icon = {
                            Icon(
                                imageVector = screen.icon,
                                contentDescription = screen.title,
                                tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        },
                        label = {
                            Text(
                                text = screen.title,
                                fontSize = 10.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        },
                        modifier = Modifier.testTag("nav_item_${screen.route}")
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            NavHost(
                navController = navController,
                startDestination = NavigationItem.Attendance.route
            ) {
                composable(NavigationItem.Classes.route) {
                    ClassesScreen(
                        viewModel = viewModel,
                        onNavigateToAttendance = { navController.navigate(NavigationItem.Attendance.route) },
                        onNavigateToBulkImport = { navController.navigate(NavigationItem.BulkImport.route) }
                    )
                }
                composable(NavigationItem.Attendance.route) {
                    AttendanceScreen(viewModel = viewModel)
                }
                composable(NavigationItem.StudentReport.route) {
                    StudentReportScreen(viewModel = viewModel)
                }
                composable(NavigationItem.ClassReport.route) {
                    ClassReportScreen(viewModel = viewModel)
                }
                composable(NavigationItem.BulkImport.route) {
                    BulkImportScreen(viewModel = viewModel)
                }
                composable(NavigationItem.MultiStaffSync.route) {
                    MultiStaffSyncScreen(viewModel = viewModel)
                }
            }
        }
    }
}
