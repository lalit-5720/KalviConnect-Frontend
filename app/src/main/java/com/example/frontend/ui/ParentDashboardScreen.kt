package com.example.frontend.ui

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.frontend.api.RetrofitClient
import com.example.frontend.models.ParentDashboardResponse
import com.example.frontend.models.ParentStudentData
import com.example.frontend.models.AnnouncementResponse
import com.example.frontend.models.MarkResponse
import com.example.frontend.ui.theme.*
import com.example.frontend.utils.TokenManager
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ParentDashboardScreen(navController: NavController) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val context = LocalContext.current
    val tokenManager = remember { TokenManager(context) }
    
    var dashboardData by remember { mutableStateOf<ParentDashboardResponse?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    fun fetchDashboard() {
        isLoading = true
        val token = tokenManager.getAccessToken()
        if (token != null) {
            RetrofitClient.instance.getParentDashboard("Bearer $token")
                .enqueue(object : Callback<ParentDashboardResponse> {
                    override fun onResponse(call: Call<ParentDashboardResponse>, response: Response<ParentDashboardResponse>) {
                        isLoading = false
                        if (response.isSuccessful) {
                            dashboardData = response.body()
                        } else {
                            Toast.makeText(context, "Failed to load data", Toast.LENGTH_SHORT).show()
                        }
                    }
                    override fun onFailure(call: Call<ParentDashboardResponse>, t: Throwable) {
                        isLoading = false
                        Toast.makeText(context, "Error: ${t.message}", Toast.LENGTH_SHORT).show()
                    }
                })
        }
    }

    LaunchedEffect(Unit) {
        fetchDashboard()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .background(BluePrimary, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("KA", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Kalvi Academy", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    }
                },
                actions = {
                    IconButton(onClick = { 
                        tokenManager.clearTokens()
                        navController.navigate("login") { popUpTo(0) }
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = "Logout", tint = RedError)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        bottomBar = {
            NavigationBar(containerColor = Color.White, tonalElevation = 8.dp) {
                val items = listOf(
                    Triple("Home", Icons.Default.Home, 0),
                    Triple("Attendance", Icons.Default.DateRange, 1),
                    Triple("Marks", Icons.Default.BarChart, 2),
                    Triple("Fees", Icons.Default.Payments, 3),
                    Triple("Announce", Icons.Default.Campaign, 4)
                )
                items.forEach { (label, icon, index) ->
                    NavigationBarItem(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        icon = { Icon(icon, contentDescription = label) },
                        label = { Text(label, fontSize = 10.sp) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = BluePrimary,
                            selectedTextColor = BluePrimary,
                            unselectedIconColor = TextSecondary,
                            unselectedTextColor = TextSecondary,
                            indicatorColor = BluePrimary.copy(alpha = 0.1f)
                        )
                    )
                }
            }
        },
        containerColor = Background
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = BluePrimary)
                }
            } else if (dashboardData == null) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No records found", color = TextSecondary)
                }
            } else {
                when (selectedTab) {
                    0 -> ParentHomeContent(dashboardData!!)
                    1 -> ParentAttendanceNavContent(dashboardData!!)
                    2 -> ParentMarksContent(dashboardData!!)
                    3 -> ParentFeesContent(dashboardData!!)
                    4 -> ParentAnnouncementsContent(dashboardData!!)
                }
            }
        }
    }
}

@Composable
fun ParentHomeContent(data: ParentDashboardResponse) {
    var selectedStudentForAttendance by remember { mutableStateOf<ParentStudentData?>(null) }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(data.students) { student ->
            StudentCard(
                student = student,
                onAttendanceClick = { selectedStudentForAttendance = student }
            )
        }
    }

    if (selectedStudentForAttendance != null) {
        AlertDialog(
            onDismissRequest = { selectedStudentForAttendance = null },
            title = {
                Text(
                    text = "Attendance History",
                    fontWeight = FontWeight.Bold,
                    color = BluePrimary
                )
            },
            text = {
                val records = selectedStudentForAttendance?.recent_attendance ?: emptyList()
                if (records.isEmpty()) {
                    Text("No recent attendance records found.", color = TextSecondary)
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(records) { record ->
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(record.date, fontSize = 14.sp, color = TextPrimary)
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (record.isPresent) GreenAccent.copy(alpha = 0.2f) else RedError.copy(alpha = 0.2f)
                                ) {
                                    Text(
                                        text = if (record.isPresent) "Present" else "Absent",
                                        color = if (record.isPresent) GreenAccent else RedError,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { selectedStudentForAttendance = null }) {
                    Text("Close", color = BluePrimary)
                }
            },
            containerColor = Color.White
        )
    }
}

@Composable
fun ParentAttendanceNavContent(data: ParentDashboardResponse) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        data.students.forEach { student ->
            item {
                Text(
                    text = "Attendance History for ${student.full_name}",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }
            val records = student.recent_attendance ?: emptyList()
            if (records.isEmpty()) {
                item { Text("No attendance record available", color = TextSecondary, fontSize = 14.sp) }
            } else {
                items(records) { record ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White)
                    ) {
                        Row(modifier = Modifier.padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text(record.date, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.weight(1f))
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (record.isPresent) GreenAccent.copy(alpha = 0.2f) else RedError.copy(alpha = 0.2f)
                            ) {
                                Text(
                                    text = if (record.isPresent) "Present" else "Absent",
                                    color = if (record.isPresent) GreenAccent else RedError,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ParentMarksContent(data: ParentDashboardResponse) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        data.students.forEach { student ->
            item {
                Text(
                    text = "Marks for ${student.full_name}",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }
            if (student.recent_marks.isEmpty()) {
                item { Text("No marks record available", color = TextSecondary, fontSize = 14.sp) }
            } else {
                items(student.recent_marks) { mark ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White)
                    ) {
                        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(mark.subject, fontWeight = FontWeight.Bold)
                                Text("${mark.examType} - ${mark.examDate}", fontSize = 12.sp, color = TextSecondary)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("${mark.marksObtained}/${mark.maxMarks}", fontWeight = FontWeight.Bold, color = BluePrimary)
                                Text("Grade: ${mark.grade}", fontSize = 12.sp, color = GreenAccent, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ParentFeesContent(data: ParentDashboardResponse) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(data.students) { student ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(student.full_name, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Total Fees", color = TextSecondary)
                        Text("Pending: ₹${student.pending_fees}", color = if (student.pending_fees > 0) RedError else GreenAccent, fontWeight = FontWeight.Bold)
                    }
                    LinearProgressIndicator(
                        progress = { if (student.pending_fees > 0) 0.5f else 1f },
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp).height(8.dp),
                        color = if (student.pending_fees > 0) OrangeWarning else GreenAccent,
                        trackColor = GraySecondary,
                        strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
                    )
                    Text(
                        text = if (student.pending_fees > 0) "Status: Pending" else "Status: Paid",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (student.pending_fees > 0) RedError else GreenAccent
                    )
                    
                    if (!student.fee_details.isNullOrEmpty()) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Payment History", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TextPrimary)
                        Spacer(modifier = Modifier.height(8.dp))
                        student.fee_details.forEach { fee ->
                            Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Fee Amount: ₹${fee.amount}", fontSize = 13.sp, color = TextSecondary)
                                    val statusColor = when(fee.status.lowercase()) {
                                        "paid" -> GreenAccent
                                        "partial" -> OrangeWarning
                                        else -> RedError
                                    }
                                    Text(fee.status.uppercase(), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = statusColor)
                                }
                                Text("Paid: ₹${fee.amountPaid ?: "0.0"}", fontSize = 13.sp, color = TextPrimary, fontWeight = FontWeight.Bold)
                                if (fee.date != null) {
                                    Text("Last Payment: ${fee.date.take(10)}", fontSize = 11.sp, color = TextSecondary)
                                }
                                Box(modifier = Modifier.padding(top = 8.dp).fillMaxWidth().height(1.dp).background(GraySecondary.copy(alpha=0.5f)))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ParentAnnouncementsContent(data: ParentDashboardResponse) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (data.announcements.isEmpty()) {
            item { Box(modifier = Modifier.fillParentMaxSize(), contentAlignment = Alignment.Center) { Text("No announcements") } }
        } else {
            items(data.announcements) { announce ->
                AnnouncementCard(
                    title = announce.title, 
                    date = announce.created_at, 
                    message = announce.message, 
                    visibility = announce.visibility,
                    targetStudentName = announce.targetStudentName
                )
            }
        }
    }
}

@Composable
fun StudentCard(student: ParentStudentData, onAttendanceClick: () -> Unit = {}) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(60.dp).clip(CircleShape).background(GraySecondary), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Person, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(32.dp))
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(student.full_name, style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                    Text("${student.class_name} - ${student.section}", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                val attColor = when {
                    student.attendance_percentage >= 80f -> GreenAccent
                    student.attendance_percentage >= 50f -> OrangeWarning
                    else -> RedError
                }
                StatCard(title = "Attendance", value = "${student.attendance_percentage}%", icon = Icons.Default.CheckCircle, color = attColor, modifier = Modifier.weight(1f), onClick = onAttendanceClick)
                StatCard(title = "Pending Fees", value = "₹${student.pending_fees}", icon = Icons.Default.Payments, color = if (student.pending_fees > 0) RedError else GreenAccent, modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
fun StatCard(title: String, value: String, icon: ImageVector, color: Color, modifier: Modifier, onClick: (() -> Unit)? = null) {
    Card(
        modifier = if (onClick != null) modifier.clickable { onClick() } else modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.height(8.dp))
            Text(value, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
            Text(title, fontSize = 12.sp, color = TextSecondary)
        }
    }
}

@Composable
fun AnnouncementCard(title: String, date: String, message: String, visibility: String, targetStudentName: String? = null) {
    val displayDate = try { date.split("T")[0] } catch (e: Exception) { date }
    val visibilityLabel = when {
        visibility == "all" -> "Public"
        targetStudentName != null -> "Private - ${targetStudentName.split(" ").first()}"
        else -> "Private"
    }

    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(title, modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold, color = TextPrimary)
                Surface(
                    color = if (visibilityLabel == "Public") GreenAccent.copy(alpha = 0.1f) else GraySecondary,
                    shape = RoundedCornerShape(4.dp),
                    modifier = Modifier.padding(end = 8.dp)
                ) {
                    Text(
                        text = visibilityLabel,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (visibilityLabel == "Public") GreenAccent else TextSecondary
                    )
                }
                Text(displayDate, fontSize = 10.sp, color = TextSecondary)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(message, fontSize = 13.sp, color = TextSecondary)
        }
    }
}
