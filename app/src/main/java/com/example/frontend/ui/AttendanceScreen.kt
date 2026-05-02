package com.example.frontend.ui

import android.app.DatePickerDialog
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.frontend.api.RetrofitClient
import com.example.frontend.models.*
import com.example.frontend.ui.theme.*
import com.example.frontend.utils.TokenManager
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AttendanceScreen(navController: NavController) {
    val context = LocalContext.current
    val tokenManager = remember { TokenManager(context) }
    
    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    var selectedDate by remember { mutableStateOf(sdf.format(Date())) }
    
    var attendanceMap by remember { mutableStateOf<Map<String, Boolean>>(emptyMap()) }
    var students by remember { mutableStateOf<List<StudentResponse>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var isSaving by remember { mutableStateOf(false) }
    var showConfirmDialog by remember { mutableStateOf(false) }
    var absentStudents by remember { mutableStateOf<List<StudentResponse>>(emptyList()) }
    var isAttendanceMarked by remember { mutableStateOf(false) }

    val calendar = Calendar.getInstance()
    val datePickerDialog = DatePickerDialog(
        context,
        { _, year, month, dayOfMonth ->
            val cal = Calendar.getInstance()
            cal.set(year, month, dayOfMonth)
            selectedDate = sdf.format(cal.time)
        },
        calendar.get(Calendar.YEAR),
        calendar.get(Calendar.MONTH),
        calendar.get(Calendar.DAY_OF_MONTH)
    ).apply {
        datePicker.maxDate = System.currentTimeMillis()
    }

    fun fetchStudentsAndAttendance() {
        isLoading = true
        isAttendanceMarked = false
        val token = tokenManager.getAccessToken()
        if (token != null) {
            RetrofitClient.instance.getStudents("Bearer $token")
                .enqueue(object : Callback<List<StudentResponse>> {
                    override fun onResponse(call: Call<List<StudentResponse>>, response: Response<List<StudentResponse>>) {
                        if (response.isSuccessful) {
                            val studentList = response.body() ?: emptyList()
                            students = studentList
                            val initialMap = studentList.associate { it.id to true }.toMutableMap()
                            
                            RetrofitClient.instance.getAttendance("Bearer $token", selectedDate)
                                .enqueue(object : Callback<List<AttendanceResponse>> {
                                    override fun onResponse(call: Call<List<AttendanceResponse>>, response: Response<List<AttendanceResponse>>) {
                                        isLoading = false
                                        if (response.isSuccessful) {
                                            val attendanceRecords = response.body()
                                            if (!attendanceRecords.isNullOrEmpty()) {
                                                isAttendanceMarked = true
                                                attendanceRecords.forEach { record ->
                                                    initialMap[record.id] = record.isPresent
                                                }
                                            } else {
                                                isAttendanceMarked = false
                                            }
                                        } else {
                                            isAttendanceMarked = false
                                        }
                                        attendanceMap = initialMap
                                    }
                                    override fun onFailure(call: Call<List<AttendanceResponse>>, t: Throwable) {
                                        isLoading = false
                                        isAttendanceMarked = false
                                        attendanceMap = initialMap
                                    }
                                })
                        } else {
                            isLoading = false
                            Toast.makeText(context, "Failed to load students", Toast.LENGTH_SHORT).show()
                        }
                    }
                    override fun onFailure(call: Call<List<StudentResponse>>, t: Throwable) {
                        isLoading = false
                        Toast.makeText(context, "Network error", Toast.LENGTH_SHORT).show()
                    }
                })
        }
    }

    fun saveAttendance() {
        val token = tokenManager.getAccessToken() ?: return

        // Ensure all students have value, defaulting to present
        val updatedMap = attendanceMap.toMutableMap()
        students.forEach { student ->
            if (!updatedMap.containsKey(student.id)) {
                updatedMap[student.id] = true
            }
        }

        val requestList = updatedMap.map { (student, isPresent) ->
            AttendanceRequest(
                student_id = student,
                is_present = isPresent
            )
        }
        
        val bulkRequest = BulkAttendanceRequest(
            date = selectedDate,
            records = requestList
        )

        Log.d("ATTENDANCE_DEBUG", "Sending: $bulkRequest")

        isSaving = true

        RetrofitClient.instance.saveAttendance("Bearer $token", bulkRequest)
            .enqueue(object : Callback<ResponseBody> {
                override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                    isSaving = false
                    if (response.isSuccessful) {
                        Toast.makeText(context, "Attendance saved successfully", Toast.LENGTH_SHORT).show()
                        navController.popBackStack()
                    } else {
                        val errorBody = response.errorBody()?.string()
                        Log.e("ATTENDANCE_DEBUG", "Error: $errorBody")
                        Toast.makeText(context, "Save Failed: $errorBody", Toast.LENGTH_LONG).show()
                    }
                }

                override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                    isSaving = false
                    Log.e("ATTENDANCE_DEBUG", "Failure: ${t.message}")
                    Toast.makeText(context, "Network Error", Toast.LENGTH_SHORT).show()
                }
            })
    }

    LaunchedEffect(selectedDate) {
        fetchStudentsAndAttendance()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Daily Attendance", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        bottomBar = {
            Surface(shadowElevation = 8.dp, color = Color.White) {
                Button(
                    onClick = {
                        val absent = students.filter { student ->
                            val isPresent = attendanceMap[student.id] ?: true
                            !isPresent
                        }
                        absentStudents = absent
                        showConfirmDialog = true
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .height(56.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = if (isAttendanceMarked) Color.Gray else BluePrimary),
                    enabled = !isSaving && !isLoading && !isAttendanceMarked
                ) {
                    if (isSaving) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                    } else {
                        val btnText = if (isAttendanceMarked) "Attendance Already Marked" else "Save Attendance"
                        Text(btnText, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        },
        containerColor = Background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.DateRange, contentDescription = null, tint = BluePrimary, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(selectedDate, fontWeight = FontWeight.Bold, color = TextPrimary)
                    Spacer(modifier = Modifier.weight(1f))
                    TextButton(onClick = { datePickerDialog.show() }) {
                        Text("Change", color = BluePrimary)
                    }
                }
            }

            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = BluePrimary)
                }
            } else if (students.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No record found", color = TextSecondary, fontSize = 16.sp)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(students) { student ->
                        val isPresent = attendanceMap[student.id] ?: true
                        AttendanceCard(
                            name = student.fullName,
                            rollNo = student.id,
                            attendancePercentage = student.attendance_percentage ?: 0f,
                            isPresent = isPresent,
                            isEditable = !isAttendanceMarked
                        ) { newStatus ->
                            attendanceMap = attendanceMap.toMutableMap().apply {
                                put(student.id, newStatus)
                            }
                        }
                    }
                }
            }
        }

        if (showConfirmDialog) {
            AlertDialog(
                onDismissRequest = { showConfirmDialog = false },
                title = { Text("Confirm Attendance", fontWeight = FontWeight.Bold) },
                text = {
                    if (absentStudents.isEmpty()) {
                        Text("All students are marked as present. Proceed to save?")
                    } else {
                        Column {
                            Text("The following students are marked as ABSENT:")
                            Spacer(modifier = Modifier.height(8.dp))
                            absentStudents.forEach { student ->
                                Text("- ${student.fullName}", color = Color.Red)
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Proceed to save?")
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        showConfirmDialog = false
                        saveAttendance()
                    }) {
                        Text("Confirm", color = BluePrimary)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showConfirmDialog = false }) {
                        Text("Cancel", color = TextSecondary)
                    }
                }
            )
        }
    }
}

@Composable
fun AttendanceCard(name: String, rollNo: String, attendancePercentage: Float, isPresent: Boolean, isEditable: Boolean = true, onStatusChange: (Boolean) -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(name, fontWeight = FontWeight.Bold, color = TextPrimary)
                
                val attColor = when {
                    attendancePercentage >= 80f -> GreenAccent
                    attendancePercentage >= 50f -> OrangeWarning
                    else -> RedError
                }
                Text("Attendance History: ${attendancePercentage}%", fontSize = 12.sp, color = attColor, fontWeight = FontWeight.Bold)
            }
            
            Row(
                modifier = Modifier
                    .background(GraySecondary, RoundedCornerShape(12.dp))
                    .padding(4.dp)
            ) {
                StatusToggle(
                    text = "P",
                    isSelected = isPresent,
                    activeColor = GreenAccent,
                    isEditable = isEditable,
                    onClick = { onStatusChange(true) }
                )
                StatusToggle(
                    text = "A",
                    isSelected = !isPresent,
                    activeColor = Color.Red,
                    isEditable = isEditable,
                    onClick = { onStatusChange(false) }
                )
            }
        }
    }
}

@Composable
fun StatusToggle(text: String, isSelected: Boolean, activeColor: Color, isEditable: Boolean = true, onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .size(36.dp)
            .clickable(enabled = isEditable) { onClick() },
        shape = RoundedCornerShape(8.dp),
        color = if (isSelected) activeColor else Color.Transparent
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = text,
                color = if (isSelected) Color.White else TextSecondary,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
        }
    }
}
