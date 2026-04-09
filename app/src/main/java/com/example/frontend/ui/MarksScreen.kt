package com.example.frontend.ui

import android.app.DatePickerDialog
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.frontend.api.RetrofitClient
import com.example.frontend.models.CreateMarkRequest
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
fun MarksScreen(navController: NavController) {
    val context = LocalContext.current
    val tokenManager = remember { TokenManager(context) }
    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    var students by remember { mutableStateOf<List<StudentResponse>>(emptyList()) }
    var selectedStudent by remember { mutableStateOf<StudentResponse?>(null) }
    var studentDropdownExpanded by remember { mutableStateOf(false) }

    var selectedSubject by remember { mutableStateOf("") }
    var subjectDropdownExpanded by remember { mutableStateOf(false) }

    var selectedExam by remember { mutableStateOf("Mid-term") }
    var examDate by remember { mutableStateOf(sdf.format(Date())) }
    var maxMarks by remember { mutableStateOf("100") }
    var marksObtained by remember { mutableStateOf("") }
    
    var isLoading by remember { mutableStateOf(true) }
    var isSaving by remember { mutableStateOf(false) }

    val calendar = Calendar.getInstance()
    val datePickerDialog = DatePickerDialog(
        context,
        { _, year, month, dayOfMonth ->
            val cal = Calendar.getInstance()
            cal.set(year, month, dayOfMonth)
            examDate = sdf.format(cal.time)
        },
        calendar.get(Calendar.YEAR),
        calendar.get(Calendar.MONTH),
        calendar.get(Calendar.DAY_OF_MONTH)
    )

    fun fetchStudents() {
        isLoading = true
        val token = tokenManager.getAccessToken()
        if (token != null) {
            RetrofitClient.instance.getStudents("Bearer $token")
                .enqueue(object : Callback<List<StudentResponse>> {
                    override fun onResponse(call: Call<List<StudentResponse>>, response: Response<List<StudentResponse>>) {
                        isLoading = false
                        if (response.isSuccessful) {
                            students = response.body() ?: emptyList()
                        } else {
                            Toast.makeText(context, "Failed to load students", Toast.LENGTH_SHORT).show()
                        }
                    }
                    override fun onFailure(call: Call<List<StudentResponse>>, t: Throwable) {
                        isLoading = false
                        Toast.makeText(context, "Error: ${t.message}", Toast.LENGTH_SHORT).show()
                    }
                })
        }
    }

    fun saveSingleMark() {
        val studentId = selectedStudent?.id ?: return
        if (selectedSubject.isBlank()) {
            Toast.makeText(context, "Please select a subject", Toast.LENGTH_SHORT).show()
            return
        }

        val token = tokenManager.getAccessToken() ?: return
        val maxM = maxMarks.toFloatOrNull() ?: 100f
        val marksO = marksObtained.toFloatOrNull() ?: 0f

        if (marksO > maxM) {
            Toast.makeText(context, "Marks cannot exceed max marks ($maxM)", Toast.LENGTH_SHORT).show()
            return
        }

        val request = CreateMarkRequest(
            studentId = studentId,
            subject = selectedSubject,
            examType = selectedExam,
            marksObtained = marksO,
            maxMarks = maxM,
            grade = calculateGrade(marksO, maxM),
            examDate = examDate
        )

        isSaving = true
        // Backend API expects a single record as defined by the user
        RetrofitClient.instance.createMarks("Bearer $token", request)
            .enqueue(object : Callback<ResponseBody> {
                override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                    isSaving = false
                    if (response.isSuccessful) {
                        Toast.makeText(context, "Marks saved successfully for ${selectedStudent?.fullName}!", Toast.LENGTH_SHORT).show()
                        marksObtained = "" // Reset entry field
                    } else {
                        Toast.makeText(context, "Failed to save marks", Toast.LENGTH_SHORT).show()
                    }
                }
                override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                    isSaving = false
                    Toast.makeText(context, "Error: ${t.message}", Toast.LENGTH_SHORT).show()
                }
            })
    }

    LaunchedEffect(Unit) {
        fetchStudents()
    }

    // Automatically select the first class subject when a student gets selected
    LaunchedEffect(selectedStudent) {
        if (selectedStudent != null) {
            val validSubjects = selectedStudent?.subjects
            if (!validSubjects.isNullOrEmpty()) {
                selectedSubject = validSubjects.first()
            } else {
                selectedSubject = "" // Reset if they are missing subjects
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Enter Marks", fontWeight = FontWeight.Bold, color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BluePrimary)
            )
        },
        containerColor = Background
    ) { innerPadding ->
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = BluePrimary)
            }
        } else {
            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // STUDENT SELECTION
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Select Student", fontWeight = FontWeight.Bold, color = TextPrimary)
                        Spacer(modifier = Modifier.height(8.dp))
                        Box {
                            Surface(
                                modifier = Modifier.fillMaxWidth().clickable { studentDropdownExpanded = true },
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFFF3F4F6)
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = selectedStudent?.fullName ?: "Select a student...",
                                        color = if (selectedStudent == null) Color.Gray else Color.Black
                                    )
                                    Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                                }
                            }
                            DropdownMenu(expanded = studentDropdownExpanded, onDismissRequest = { studentDropdownExpanded = false }) {
                                students.forEach { student ->
                                    DropdownMenuItem(
                                        text = { Text(student.fullName) },
                                        onClick = {
                                            selectedStudent = student
                                            studentDropdownExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                // DETAILS ENTRY
                if (selectedStudent != null) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            // Subject Dropdown mapped from student.subjects
                            Text("Subject", fontWeight = FontWeight.Bold, color = TextPrimary)
                            val assignedSubjects = selectedStudent?.subjects ?: emptyList()
                            
                            if (assignedSubjects.isEmpty()) {
                                Text("This student has no subjects assigned.", color = RedError, fontSize = 14.sp)
                            } else {
                                Box {
                                    Surface(
                                        modifier = Modifier.fillMaxWidth().clickable { subjectDropdownExpanded = true },
                                        shape = RoundedCornerShape(8.dp),
                                        color = Color(0xFFF3F4F6)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(12.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = if (selectedSubject.isNotBlank()) selectedSubject else "Select a subject...",
                                                color = Color.Black
                                            )
                                            Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                                        }
                                    }
                                    DropdownMenu(expanded = subjectDropdownExpanded, onDismissRequest = { subjectDropdownExpanded = false }) {
                                        assignedSubjects.forEach { subject ->
                                            DropdownMenuItem(
                                                text = { Text(subject) },
                                                onClick = {
                                                    selectedSubject = subject
                                                    subjectDropdownExpanded = false
                                                }
                                            )
                                        }
                                    }
                                }
                            }

                            // Exam Type Input
                            OutlinedTextField(
                                value = selectedExam,
                                onValueChange = { selectedExam = it },
                                label = { Text("Exam Name (e.g. Mid-term)") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )

                            // Exam Date and Max Marks Row
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Exam Date", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
                                    Row(
                                        modifier = Modifier
                                            .padding(top = 8.dp)
                                            .clickable { datePickerDialog.show() },
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Default.DateRange, contentDescription = null, tint = BluePrimary, modifier = Modifier.size(20.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(examDate, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                    }
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    OutlinedTextField(
                                        value = maxMarks,
                                        onValueChange = { maxMarks = it },
                                        label = { Text("Max Marks") },
                                        modifier = Modifier.fillMaxWidth(),
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        singleLine = true
                                    )
                                }
                            }
                            
                            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = Color(0xFFF3F4F6))

                            val maxM = maxMarks.toFloatOrNull() ?: 100f
                            val currentMarks = marksObtained.toFloatOrNull() ?: 0f

                            // Marks obtained Input
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Marks Obtained", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = TextPrimary)
                                    Text("Grade: ${calculateGrade(currentMarks, maxM)}", fontSize = 14.sp, color = GreenAccent, fontWeight = FontWeight.Bold)
                                }
                                OutlinedTextField(
                                    value = marksObtained,
                                    onValueChange = { marksObtained = it },
                                    modifier = Modifier.width(100.dp),
                                    placeholder = { Text("0") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    singleLine = true,
                                    textStyle = LocalTextStyle.current.copy(textAlign = androidx.compose.ui.text.style.TextAlign.Center, fontWeight = FontWeight.Bold, fontSize = 18.sp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedContainerColor = GraySecondary.copy(alpha = 0.5f),
                                        unfocusedContainerColor = GraySecondary.copy(alpha = 0.5f),
                                        unfocusedBorderColor = Color.Transparent,
                                        focusedBorderColor = BluePrimary
                                    )
                                )
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))

                    Button(
                        onClick = { saveSingleMark() },
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = BluePrimary),
                        enabled = !isSaving && selectedStudent != null && selectedSubject.isNotBlank() && marksObtained.isNotBlank()
                    ) {
                        if (isSaving) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                        } else {
                            Text("Save Marks", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

fun calculateGrade(marks: Float, maxMarks: Float): String {
    if (maxMarks <= 0) return "F"
    val percentage = (marks / maxMarks) * 100
    return when {
        percentage >= 90 -> "A+"
        percentage >= 80 -> "A"
        percentage >= 70 -> "B"
        percentage >= 60 -> "C"
        percentage >= 50 -> "D"
        else -> "F"
    }
}
