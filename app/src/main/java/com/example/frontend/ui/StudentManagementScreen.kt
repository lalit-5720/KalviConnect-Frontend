package com.example.frontend.ui

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.example.frontend.models.StudentResponse
import com.example.frontend.ui.theme.*
import com.example.frontend.utils.TokenManager
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

@OptIn(ExperimentalMaterial3Api::class)
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
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun StudentCard(student: StudentResponse, onDelete: (String) -> Unit) {
    var showDeleteDialog by remember { mutableStateOf(false) }

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
            }
            Row {
                IconButton(onClick = { /* Edit Logic */ }) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit", tint = BluePrimary, modifier = Modifier.size(20.dp))
                }
                IconButton(onClick = { showDeleteDialog = true }) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Red, modifier = Modifier.size(20.dp))
                }
            }
        }
    }
}
