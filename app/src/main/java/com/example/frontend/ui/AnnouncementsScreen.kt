package com.example.frontend.ui

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.frontend.models.AnnouncementResponse
import com.example.frontend.models.StudentResponse
import com.example.frontend.ui.theme.*
import com.example.frontend.utils.TokenManager
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnnouncementsScreen(
    navController: NavController,
    viewModel: AnnouncementsViewModel = viewModel()
) {
    val announcements by viewModel.announcements.collectAsState()
    val students by v2iewModel.students.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val isCreating by viewModel.isCreating.collectAsState()
    val error by viewModel.error.collectAsState()

    val context = LocalContext.current
    val tokenManager = remember { TokenManager(context) }
    var showCreateDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        val token = tokenManager.getAccessToken()
        if (token != null) {
            viewModel.fetchAnnouncements(token)
            viewModel.fetchStudents(token)
        }
    }

    LaunchedEffect(error) {
        if (error != null) {
            Toast.makeText(context, error, Toast.LENGTH_SHORT).show()
            viewModel.clearError()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Announcements", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showCreateDialog = true },
                containerColor = BluePrimary,
                contentColor = Color.White,
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add")
            }
        },
        containerColor = Background
    ) { innerPadding ->
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = BluePrimary)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (announcements.isEmpty()) {
                    item {
                        Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                            Text("No announcements available", color = TextSecondary)
                        }
                    }
                }
                items(announcements) { announcement ->
                    AnnouncementDetailCard(announcement)
                }
            }
        }
    }

    if (showCreateDialog) {
        CreateAnnouncementDialog(
            isCreating = isCreating,
            students = students,
            onDismiss = { showCreateDialog = false },
            onSubmit = { title, message, isPublic, targetStudentId ->
                val token = tokenManager.getAccessToken()
                if (token != null) {
                    viewModel.createAnnouncement(token, title, message, isPublic, targetStudentId) {
                        showCreateDialog = false
                        Toast.makeText(context, "Announcement posted!", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        )
    }
}

@Composable
fun CreateAnnouncementDialog(
    isCreating: Boolean,
    students: List<StudentResponse>,
    onDismiss: () -> Unit,
    onSubmit: (title: String, message: String, isPublic: Boolean, targetStudentId: String?) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }
    var isPublic by remember { mutableStateOf(true) }
    
    var selectedStudent by remember { mutableStateOf<StudentResponse?>(null) }
    var expanded by remember { mutableStateOf(false) }

    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        title = {
            Text("Create Announcement", fontWeight = FontWeight.Bold, fontSize = 20.sp)
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = message,
                    onValueChange = { message = it },
                    label = { Text("Message") },
                    modifier = Modifier.fillMaxWidth().height(120.dp),
                    maxLines = 5
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Announcement Type:", modifier = Modifier.weight(1f))
                    Text(if (isPublic) "Public" else "Private", fontWeight = FontWeight.Bold)
                    Switch(
                        checked = isPublic,
                        onCheckedChange = { isPublic = it },
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }

                if (!isPublic) {
                    Box(modifier = Modifier.fillMaxWidth()) {
                        Surface(
                            modifier = Modifier.fillMaxWidth().clickable { expanded = true },
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFF3F4F6)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(selectedStudent?.fullName ?: "Select a student...", color = if (selectedStudent == null) Color.Gray else Color.Black)
                                Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                            }
                        }
                        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                            students.forEach { student ->
                                DropdownMenuItem(
                                    text = { Text(student.fullName) },
                                    onClick = {
                                        selectedStudent = student
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotBlank() && message.isNotBlank()) {
                        if (!isPublic && selectedStudent == null) {
                            Toast.makeText(context, "Please select a student for Private announcements", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        onSubmit(title, message, isPublic, if (isPublic) null else selectedStudent?.id)
                    } else {
                        Toast.makeText(context, "Please fill in all fields", Toast.LENGTH_SHORT).show()
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = BluePrimary),
                enabled = !isCreating
            ) {
                if (isCreating) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp))
                } else {
                    Text("Post", fontWeight = FontWeight.Bold)
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextSecondary)
            }
        }
    )
}

@Composable
fun AnnouncementDetailCard(announcement: AnnouncementResponse) {
    val displayDate = try {
        val parser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
        val dateObj = parser.parse(announcement.created_at)
        val formatter = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
        dateObj?.let { formatter.format(it) } ?: announcement.created_at
    } catch (e: Exception) {
        announcement.created_at.take(10)
    }

    val visibilityLabel = when (announcement.visibility) {
        "all" -> "Public"
        "specific_student" -> "Private"
        "parents_only" -> "Private"
        else -> announcement.visibility
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(BluePrimary.copy(alpha = 0.1f), RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Campaign, contentDescription = null, tint = BluePrimary, modifier = Modifier.size(24.dp))
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(announcement.title, fontWeight = FontWeight.Bold, color = TextPrimary)
                    Text(displayDate, fontSize = 12.sp, color = TextSecondary)
                }
                Surface(
                    color = if (visibilityLabel == "Public") GreenAccent.copy(alpha = 0.1f) else GraySecondary,
                    shape = RoundedCornerShape(4.dp)
                ) {
                    Text(
                        text = visibilityLabel,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (visibilityLabel == "Public") GreenAccent else TextSecondary
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = announcement.message,
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
                lineHeight = 20.sp
            )
        }
    }
}
