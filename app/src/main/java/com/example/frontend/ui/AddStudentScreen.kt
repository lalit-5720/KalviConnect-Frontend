package com.example.frontend.ui

import android.app.DatePickerDialog
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
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
import com.example.frontend.models.CreateStudentRequest
import com.example.frontend.ui.theme.*
import com.example.frontend.utils.TokenManager
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddStudentScreen(navController: NavController) {
    var fullName by remember { mutableStateOf("") }
    var className by remember { mutableStateOf("") }
    var section by remember { mutableStateOf("") }
    var parentName by remember { mutableStateOf("") }
    var parentPhone by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    
    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    var dob by remember { mutableStateOf(sdf.format(Date())) }
    
    // Subject management
    var currentSubject by remember { mutableStateOf("") }
    val subjects = remember { mutableStateListOf<String>() }

    var isLoading by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val tokenManager = remember { TokenManager(context) }

    val calendar = Calendar.getInstance()
    val datePickerDialog = DatePickerDialog(
        context,
        { _, year, month, dayOfMonth ->
            val cal = Calendar.getInstance()
            cal.set(year, month, dayOfMonth)
            dob = sdf.format(cal.time)
        },
        calendar.get(Calendar.YEAR),
        calendar.get(Calendar.MONTH),
        calendar.get(Calendar.DAY_OF_MONTH)
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Add New Student", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        containerColor = Background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .background(GraySecondary, RoundedCornerShape(50.dp)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.PhotoCamera, contentDescription = null, tint = TextSecondary)
                    Text("Upload", fontSize = 10.sp, color = TextSecondary)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            FormTextField(value = fullName, onValueChange = { fullName = it }, label = "Full Name")
            
            Row(modifier = Modifier.fillMaxWidth()) {
                Box(modifier = Modifier.weight(1f)) {
                    FormTextField(value = className, onValueChange = { className = it }, label = "Class")
                }
                Spacer(modifier = Modifier.width(16.dp))
                Box(modifier = Modifier.weight(1f)) {
                    FormTextField(value = section, onValueChange = { section = it }, label = "Section")
                }
            }

            FormTextField(value = parentName, onValueChange = { parentName = it }, label = "Parent Name")
            FormTextField(value = parentPhone, onValueChange = { parentPhone = it }, label = "Parent Phone")
            FormTextField(value = address, onValueChange = { address = it }, label = "Address", singleLine = false, modifier = Modifier.height(100.dp))

            // NEW: SUBJECT ADDITION SECTION
            Spacer(modifier = Modifier.height(16.dp))
            Column(modifier = Modifier.fillMaxWidth()) {
                Text("Add Subjects", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = currentSubject,
                        onValueChange = { currentSubject = it },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("e.g. Mathematics") },
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = BluePrimary)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val trimmed = currentSubject.trim()
                            if (trimmed.isNotBlank() && !subjects.contains(trimmed)) {
                                subjects.add(trimmed)
                                currentSubject = ""
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = BluePrimary)
                    ) {
                        Text("Add")
                    }
                }
                
                if (subjects.isNotEmpty()) {
                    FlowRow(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
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

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedButton(
                onClick = { datePickerDialog.show() },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary)
            ) {
                Icon(Icons.Default.DateRange, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Date of Birth: $dob")
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = { 
                    if (fullName.isNotBlank() && className.isNotBlank() && parentPhone.isNotBlank()) {
                        if (subjects.isEmpty()) {
                            Toast.makeText(context, "Please add at least one subject", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        isLoading = true
                        val request = CreateStudentRequest(
                            full_name = fullName,
                            class_name = className,
                            section = section,
                            parentName = parentName,
                            parentPhone = parentPhone,
                            address = address,
                            dob = dob,
                            subjects = subjects.toList()
                        )
                        
                        val token = tokenManager.getAccessToken()
                        if (token != null) {
                            RetrofitClient.instance.createStudent("Bearer $token", request)
                                .enqueue(object : Callback<ResponseBody> {
                                    override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                                        isLoading = false
                                        if (response.isSuccessful) {
                                            Toast.makeText(context, "Student added successfully!", Toast.LENGTH_SHORT).show()
                                            navController.popBackStack()
                                        } else {
                                            Toast.makeText(context, "Error: ${response.code()}", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                    override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                                        isLoading = false
                                        Toast.makeText(context, "Network Error", Toast.LENGTH_SHORT).show()
                                    }
                                })
                        }
                    } else {
                        Toast.makeText(context, "Please fill required fields", Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = BluePrimary),
                enabled = !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                } else {
                    Text("Save Student Details", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun FormTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    singleLine: Boolean = true,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.padding(vertical = 8.dp)) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = TextSecondary, modifier = Modifier.padding(bottom = 4.dp))
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            singleLine = singleLine,
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color.White,
                focusedBorderColor = BluePrimary,
                unfocusedBorderColor = Color.Transparent
            )
        )
    }
}
