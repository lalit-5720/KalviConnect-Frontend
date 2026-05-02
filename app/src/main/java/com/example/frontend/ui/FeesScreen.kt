package com.example.frontend.ui

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
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
import com.example.frontend.models.FeeResponse
import com.example.frontend.ui.theme.*
import com.example.frontend.utils.TokenManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeesScreen(
    navController: NavController,
    viewModel: FeesViewModel = viewModel()
) {
    val context = LocalContext.current
    val tokenManager = remember { TokenManager(context) }
    
    val students by viewModel.students.collectAsState() // Fetching all global students
    val studentsFees by viewModel.studentsFees.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val isUpdating by viewModel.isUpdating.collectAsState()
    val error by viewModel.error.collectAsState()

    var selectedStudent by remember { mutableStateOf<com.example.frontend.models.StudentResponse?>(null) }
    var expanded by remember { mutableStateOf(false) }
    
    // Dual Input Requirements
    var totalFeeAmount by remember { mutableStateOf("") }
    var paymentAmount by remember { mutableStateOf("") }

    LaunchedEffect(error) {
        if (error != null) {
            Toast.makeText(context, error, Toast.LENGTH_SHORT).show()
            viewModel.clearError()
        }
    }

    LaunchedEffect(Unit) {
        val token = tokenManager.getAccessToken()
        if (token != null) {
            viewModel.fetchStudents(token) // Make sure global students are populated for dropdown
            viewModel.fetchFees(token, null) { } // Keep fee history pre-loaded
        }
    }

    LaunchedEffect(selectedStudent) {
        val matchingFeeHistory = studentsFees.find { it.studentName == selectedStudent?.fullName }
        if (matchingFeeHistory != null) {
            totalFeeAmount = matchingFeeHistory.amount.toString()
            paymentAmount = ""
        } else {
            totalFeeAmount = ""
            paymentAmount = ""
        }
    }

    fun handleFeeCreation() {
        val token = tokenManager.getAccessToken() ?: return
        val student = selectedStudent ?: return
        
        val total = totalFeeAmount.toDoubleOrNull() ?: 0.0
        val newlyPaid = paymentAmount.toDoubleOrNull() ?: 0.0
        
        val matchingFeeHistory = studentsFees.find { it.studentName == student.fullName }
        val prePaidVal = matchingFeeHistory?.amountPaid?.toDoubleOrNull() ?: 0.0
        val cumulativePaid = prePaidVal + newlyPaid

        if (total <= 0 || newlyPaid < 0 || cumulativePaid > total) {
            Toast.makeText(context, "Parameters Invalid: Ensure New Payment + Previous Paid <= Total and Total > 0.", Toast.LENGTH_SHORT).show()
            return
        }

        viewModel.createFeeRecord(token, student.id, total, cumulativePaid) {
            Toast.makeText(context, "Fee Record created successfully!", Toast.LENGTH_SHORT).show()
            paymentAmount = ""
            viewModel.fetchFees(token, student.id) { } // Refresh history
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Fees Management", color = Color.White, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BluePrimary)
            )
        },
        containerColor = Color(0xFFF8F9FA)
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
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // SELECT STUDENT SECTION
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
                                modifier = Modifier.fillMaxWidth().clickable { expanded = true },
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFFF3F4F6)
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(selectedStudent?.fullName ?: "Select a student", color = if (selectedStudent == null) Color.Gray else Color.Black)
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

                // DUAL FEE INPUT SECTION
                if (selectedStudent != null) {
                    val matchingFeeHistory = studentsFees.find { it.studentName == selectedStudent?.fullName }
                    
                    val dynamicTotal = totalFeeAmount.toDoubleOrNull() ?: 0.0
                    val newlyPaid = paymentAmount.toDoubleOrNull() ?: 0.0
                    val previousPaid = matchingFeeHistory?.amountPaid?.toDoubleOrNull() ?: 0.0
                    
                    val pendingBeforePayment = dynamicTotal - previousPaid
                    val due = pendingBeforePayment - newlyPaid

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("New Fee Parameters", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                                Surface(
                                    color = if (due <= 0 && dynamicTotal > 0) Color(0xFFE8F5E9) else Color(0xFFFFEBEE),
                                    shape = RoundedCornerShape(16.dp)
                                ) {
                                    Text(
                                        text = if (due <= 0 && dynamicTotal > 0) "Completed" else "Due calculation...",
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                                        color = if (due <= 0 && dynamicTotal > 0) Color(0xFF2E7D32) else Color(0xFFC62828),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            if (matchingFeeHistory != null) {
                                Text("Previous Paid: ₹${previousPaid}", fontSize = 14.sp)
                                Text("Pending Amount: ₹${pendingBeforePayment}", fontWeight = FontWeight.Bold, color = BluePrimary)
                                Spacer(modifier = Modifier.height(8.dp))
                            }
                            
                            Text("Total Fees (₹)", fontWeight = FontWeight.Bold)
                            TextField(
                                value = totalFeeAmount,
                                onValueChange = { totalFeeAmount = it },
                                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                                colors = TextFieldDefaults.colors(focusedContainerColor = Color(0xFFF3F4F6), unfocusedContainerColor = Color(0xFFF3F4F6), unfocusedIndicatorColor = Color.Transparent, focusedIndicatorColor = Color.Transparent),
                                shape = RoundedCornerShape(8.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            val payLabel = if (matchingFeeHistory != null) "New Payment Amount (₹)" else "Paid Fees (₹)"
                            Text(payLabel, fontWeight = FontWeight.Bold)
                            TextField(
                                value = paymentAmount,
                                onValueChange = { paymentAmount = it },
                                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                                colors = TextFieldDefaults.colors(focusedContainerColor = Color(0xFFE8F5E9), unfocusedContainerColor = Color(0xFFE8F5E9), unfocusedIndicatorColor = Color.Transparent, focusedIndicatorColor = Color.Transparent),
                                shape = RoundedCornerShape(8.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            FeeRowItem("Remaining Due", "₹${String.format("%,.0f", due)}", Color(0xFFC62828))
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            Button(
                                onClick = { handleFeeCreation() },
                                modifier = Modifier.fillMaxWidth().height(56.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = BluePrimary),
                                shape = RoundedCornerShape(12.dp),
                                enabled = !isUpdating 
                            ) {
                                if (isUpdating) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                                else Text("Commit Fees", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            }
                        }
                    }

                    // PAYMENT HISTORY SECTION
                    if (matchingFeeHistory != null) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text("Archived Fee Records", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                                Spacer(modifier = Modifier.height(16.dp))
                                
                                FeeRowItem("Historical Total Fee", "₹${matchingFeeHistory.amount}", Color.Black)
                                FeeRowItem("Total Paid Cumulatively", "₹${matchingFeeHistory.amountPaid}", Color(0xFF2E7D32))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FeeRowItem(label: String, value: String, valueColor: Color) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = Color.Gray, fontSize = 14.sp)
        Text(value, color = valueColor, fontWeight = FontWeight.Bold, fontSize = 18.sp)
    }
}
