package com.example.frontend.ui

import android.app.DatePickerDialog
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
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
import com.example.frontend.models.AttendanceRequest
import com.example.frontend.models.AttendanceResponse
import com.example.frontend.models.StudentResponse
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
    )

    fun fetchStudentsAndAttendance() {
        isLoading = true
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
                                            response.body()?.forEach { record ->
                                                initialMap[record.id] = record.isPresent
                                            }
                                        }
                                        attendanceMap = initialMap
                                    }
                                    override fun onFailure(call: Call<List<AttendanceResponse>>, t: Throwable) {
                                        isLoading = false
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
        isSaving = true
        
        val requestList = attendanceMap.map { (studentId, isPresent) ->
            AttendanceRequest(student_id = studentId, date = selectedDate, is_present = isPresent)
        }

        RetrofitClient.instance.saveAttendance("Bearer $token", requestList)
            .enqueue(object : Callback<ResponseBody> {
                override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                    isSaving = false
                    if (response.isSuccessful) {
                        Toast.makeText(context, "Attendance saved successfully", Toast.LENGTH_SHORT).show()
                        navController.popBackStack()
                    } else {
                        Toast.makeText(context, "Failed to save", Toast.LENGTH_SHORT).show()
                    }
                }
                override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                    isSaving = false
                    Toast.makeText(context, "Error saving attendance", Toast.LENGTH_SHORT).show()
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
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        bottomBar = {
            Surface(shadowElevation = 8.dp, color = Color.White) {
                Button(
                    onClick = { saveAttendance() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .height(56.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = BluePrimary),
                    enabled = !isSaving && !isLoading
                ) {
                    if (isSaving) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                    } else {
                        Text("Save Attendance", fontSize = 16.sp, fontWeight = FontWeight.Bold)
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
                            isPresent = isPresent
                        ) { newStatus ->
                            attendanceMap = attendanceMap.toMutableMap().apply {
                                put(student.id, newStatus)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AttendanceCard(name: String, rollNo: String, isPresent: Boolean, onStatusChange: (Boolean) -> Unit) {
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
                Text("Roll No: $rollNo", fontSize = 12.sp, color = TextSecondary)
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
                    onClick = { onStatusChange(true) }
                )
                StatusToggle(
                    text = "A",
                    isSelected = !isPresent,
                    activeColor = Color.Red,
                    onClick = { onStatusChange(false) }
                )
            }
        }
    }
}

@Composable
fun StatusToggle(text: String, isSelected: Boolean, activeColor: Color, onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .size(36.dp)
            .clickable { onClick() },
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
