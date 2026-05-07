package com.example.frontend.ui

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun StudentManagementScreen(navController: NavController) {
    var searchQuery by remember { mutableStateOf("") }
    var students by remember { mutableStateOf<List<StudentResponse>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    
    val context = LocalContext.current
    val tokenManager = remember { TokenManager(context) }

    fun fetchStudents(query: String? = null) {
        isLoading = true
        val token = tokenManager.getAccessToken()
        if (token != null) {
            RetrofitClient.instance.getStudents("Bearer $token", query)
                .enqueue(object : Callback<List<StudentResponse>> {
                    override fun onResponse(
                        call: Call<List<StudentResponse>>,
                        response: Response<List<StudentResponse>>
                    ) {
                        isLoading = false
                        if (response.isSuccessful) {
                            // strictly getting records from backend response
                            students = response.body() ?: emptyList()
                        } else {
                            Toast.makeText(context, "Error: ${response.code()}", Toast.LENGTH_SHORT).show()
                        }
                    }

                    override fun onFailure(call: Call<List<StudentResponse>>, t: Throwable) {
                        isLoading = false
                        Toast.makeText(context, "Network Error: ${t.message}", Toast.LENGTH_SHORT).show()
                    }
                })
        } else {
            isLoading = false
            Toast.makeText(context, "Session expired. Please login again.", Toast.LENGTH_SHORT).show()
        }
    }

    // Initial fetch when screen opens
    LaunchedEffect(Unit) {
        fetchStudents()
    }

    // Handle search query changes
    LaunchedEffect(searchQuery) {
        // debounce slightly or wait for 2 characters
        if (searchQuery.length >= 2 || searchQuery.isEmpty()) {
            fetchStudents(if (searchQuery.isEmpty()) null else searchQuery)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Student Management", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        bottomBar = {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shadowElevation = 8.dp,
                color = Color.White
            ) {
                Button(
                    onClick = { navController.navigate("add_student") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .height(56.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = BluePrimary)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Add New Student", fontSize = 16.sp, fontWeight = FontWeight.Bold)
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
            // Search and Refresh
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Search students...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White,
                        unfocusedBorderColor = Color.Transparent,
                        focusedBorderColor = BluePrimary
                    )
                )
                Spacer(modifier = Modifier.width(12.dp))
                IconButton(
                    onClick = { fetchStudents(searchQuery) },
                    modifier = Modifier
                        .size(56.dp)
                        .background(Color.White, RoundedCornerShape(12.dp))
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = BluePrimary)
                }
            }

            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = BluePrimary)
                }
            } else if (students.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    // updated empty state as requested
                    Text("No record found", color = TextSecondary, fontSize = 16.sp)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(students) { student ->
                        StudentCard(
                            student = student,
                            onDelete = { id ->
                                val token = tokenManager.getAccessToken()
                                if (token != null) {
                                    RetrofitClient.instance.deleteStudent("Bearer $token", id)
                                        .enqueue(object : Callback<okhttp3.ResponseBody> {
                                            override fun onResponse(call: Call<okhttp3.ResponseBody>, response: Response<okhttp3.ResponseBody>) {
                                                if (response.isSuccessful) {
                                                    Toast.makeText(context, "Student deleted successfully", Toast.LENGTH_SHORT).show()
                                                    fetchStudents(searchQuery) // refresh list
                                                } else {
                                                    Toast.makeText(context, "Failed to delete", Toast.LENGTH_SHORT).show()
                                                }
                                            }
                                            override fun onFailure(call: Call<okhttp3.ResponseBody>, t: Throwable) {
                                                Toast.makeText(context, "Network error", Toast.LENGTH_SHORT).show()
                                            }
                                        })
                                }
                            },
                            onEdit = { id, request ->
                                val token = tokenManager.getAccessToken()
                                if (token != null) {
                                    RetrofitClient.instance.updateStudent("Bearer $token", id, request)
                                        .enqueue(object : Callback<okhttp3.ResponseBody> {
                                            override fun onResponse(call: Call<okhttp3.ResponseBody>, response: Response<okhttp3.ResponseBody>) {
                                                if (response.isSuccessful) {
                                                    Toast.makeText(context, "Student updated successfully", Toast.LENGTH_SHORT).show()
                                                    fetchStudents(searchQuery) // refresh list
                                                } else {
                                                    Toast.makeText(context, "Failed to update", Toast.LENGTH_SHORT).show()
                                                }
                                            }
                                            override fun onFailure(call: Call<okhttp3.ResponseBody>, t: Throwable) {
                                                Toast.makeText(context, "Network error", Toast.LENGTH_SHORT).show()
                                            }
                                        })
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun StudentCard(student: StudentResponse, onDelete: (String) -> Unit, onEdit: (String, CreateStudentRequest) -> Unit) {
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }
    var showHistoryDialog by remember { mutableStateOf(false) }
    var historyRecords by remember { mutableStateOf<List<AttendanceResponse>?>(null) }
    var isLoadingHistory by remember { mutableStateOf(false) }
    
    val context = LocalContext.current
    val tokenManager = remember { TokenManager(context) }

    fun fetchHistory() {
        showHistoryDialog = true
        isLoadingHistory = true
        val token = tokenManager.getAccessToken()
        if (token != null) {
            RetrofitClient.instance.getStudentAttendanceHistory("Bearer $token", student.id)
                .enqueue(object : Callback<List<AttendanceResponse>> {
                    override fun onResponse(call: Call<List<AttendanceResponse>>, response: Response<List<AttendanceResponse>>) {
                        isLoadingHistory = false
                        if (response.isSuccessful) {
                            historyRecords = response.body()
                        } else {
                            Toast.makeText(context, "Failed to load history", Toast.LENGTH_SHORT).show()
                            showHistoryDialog = false
                        }
                    }
                    override fun onFailure(call: Call<List<AttendanceResponse>>, t: Throwable) {
                        isLoadingHistory = false
                        Toast.makeText(context, "Network Error", Toast.LENGTH_SHORT).show()
                        showHistoryDialog = false
                    }
                })
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            containerColor = Color.White,
            title = { Text("Delete Student", fontWeight = FontWeight.Bold, color = RedError) },
            text = { Text("Are you sure you want to completely remove ${student.fullName}? This action cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteDialog = false
                        onDelete(student.id)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RedError)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        )
    }

    if (showEditDialog) {
        EditStudentDialog(
            student = student,
            onDismiss = { showEditDialog = false },
            onSave = { request ->
                showEditDialog = false
                onEdit(student.id, request)
            }
        )
    }

    if (showHistoryDialog) {
        AlertDialog(
            onDismissRequest = { showHistoryDialog = false },
            title = {
                Text("Attendance History: ${student.fullName}", fontWeight = FontWeight.Bold, color = BluePrimary)
            },
            text = {
                if (isLoadingHistory) {
                    Box(modifier = Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = BluePrimary)
                    }
                } else if (historyRecords.isNullOrEmpty()) {
                    Text("No attendance records found.", color = TextSecondary)
                } else {
                    LazyColumn(modifier = Modifier.heightIn(max = 300.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(historyRecords!!) { record ->
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
                TextButton(onClick = { showHistoryDialog = false }) {
                    Text("Close", color = BluePrimary)
                }
            },
            containerColor = Color.White
        )
    }

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
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .clip(CircleShape)
                    .background(GraySecondary),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Person, contentDescription = null, tint = TextSecondary)
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(student.fullName, fontWeight = FontWeight.Bold, color = TextPrimary)
                Text("${student.className}-${student.section}", fontSize = 12.sp, color = TextSecondary)
                Text("Parent: ${student.parentPhone}", fontSize = 12.sp, color = TextSecondary)
                
                val attendance = student.attendance_percentage ?: 0f
                val attColor = when {
                    attendance >= 80f -> GreenAccent
                    attendance >= 50f -> OrangeWarning
                    else -> RedError
                }
                Text("Attendance: ${attendance}%", fontSize = 12.sp, color = attColor, fontWeight = FontWeight.Bold)
            }
            Row {
                IconButton(onClick = { fetchHistory() }) {
                    Icon(Icons.Default.DateRange, contentDescription = "History", tint = GreenAccent, modifier = Modifier.size(20.dp))
                }
                IconButton(onClick = { showEditDialog = true }) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit", tint = BluePrimary, modifier = Modifier.size(20.dp))
                }
                IconButton(onClick = { showDeleteDialog = true }) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Red, modifier = Modifier.size(20.dp))
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EditStudentDialog(
    student: StudentResponse,
    onDismiss: () -> Unit,
    onSave: (CreateStudentRequest) -> Unit
) {
    var fullName by remember { mutableStateOf(student.fullName) }
    var className by remember { mutableStateOf(student.className) }
    var section by remember { mutableStateOf(student.section) }
    var parentName by remember { mutableStateOf(student.parentName) }
    var parentPhone by remember { mutableStateOf(student.parentPhone) }
    var address by remember { mutableStateOf(student.address ?: "") }
    
    var currentSubject by remember { mutableStateOf("") }
    val subjects = remember { mutableStateListOf(*(student.subjects?.toTypedArray() ?: emptyArray())) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Student Details", fontWeight = FontWeight.Bold, color = BluePrimary) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState()).fillMaxWidth()) {
                OutlinedTextField(value = fullName, onValueChange = { fullName = it }, label = { Text("Full Name") }, modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp))
                Row(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                    OutlinedTextField(value = className, onValueChange = { className = it }, label = { Text("Class") }, modifier = Modifier.weight(1f))
                    Spacer(modifier = Modifier.width(8.dp))
                    OutlinedTextField(value = section, onValueChange = { section = it }, label = { Text("Section") }, modifier = Modifier.weight(1f))
                }
                OutlinedTextField(value = parentName, onValueChange = { parentName = it }, label = { Text("Parent Name") }, modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp))
                OutlinedTextField(value = parentPhone, onValueChange = { parentPhone = it }, label = { Text("Parent Phone") }, modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp))
                OutlinedTextField(value = address, onValueChange = { address = it }, label = { Text("Address") }, modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp))
                
                Text("Subjects", fontWeight = FontWeight.Bold, modifier = Modifier.padding(vertical = 4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(value = currentSubject, onValueChange = { currentSubject = it }, placeholder = { Text("Add Subject") }, modifier = Modifier.weight(1f))
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val trimmed = currentSubject.trim()
                            if (trimmed.isNotBlank() && !subjects.contains(trimmed)) {
                                subjects.add(trimmed)
                                currentSubject = ""
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = BluePrimary)
                    ) { Text("Add") }
                }
                if (subjects.isNotEmpty()) {
                    FlowRow(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        subjects.forEach { subject ->
                            InputChip(
                                selected = false,
                                onClick = { subjects.remove(subject) },
                                label = { Text(subject) },
                                trailingIcon = { Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp)) }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(CreateStudentRequest(
                        full_name = fullName,
                        class_name = className,
                        section = section,
                        parentName = parentName,
                        parentPhone = parentPhone,
                        address = address,
                        dob = student.dob,
                        subjects = subjects.toList()
                    ))
                },
                colors = ButtonDefaults.buttonColors(containerColor = BluePrimary)
            ) { Text("Save Changes") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = Color.Gray) }
        },
        containerColor = Color.White
    )
}
