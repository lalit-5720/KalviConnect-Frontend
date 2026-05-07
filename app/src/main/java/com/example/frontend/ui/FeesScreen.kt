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
    var newFeeChargeAmount by remember { mutableStateOf("") }
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
        newFeeChargeAmount = ""
        paymentAmount = ""
    }

    fun handleRecordPayment() {
        val token = tokenManager.getAccessToken() ?: return
        val student = selectedStudent ?: return
        
        val newlyPaid = paymentAmount.toDoubleOrNull() ?: 0.0
        
        if (newlyPaid <= 0) {
            Toast.makeText(context, "Please enter a valid payment amount.", Toast.LENGTH_SHORT).show()
            return
        }

        viewModel.updatePayment(token, student.id, newlyPaid) {
            Toast.makeText(context, "Payment recorded successfully!", Toast.LENGTH_SHORT).show()
            paymentAmount = ""
            viewModel.fetchFees(token, student.id) { }
        }
    }

    fun handleAddNewCharge() {
        val token = tokenManager.getAccessToken() ?: return
        val student = selectedStudent ?: return
        
        val newCharge = newFeeChargeAmount.toDoubleOrNull() ?: 0.0
        
        if (newCharge <= 0) {
            Toast.makeText(context, "Please enter a valid charge amount.", Toast.LENGTH_SHORT).show()
            return
        }

        viewModel.createFeeRecord(token, student.id, newCharge, 0.0) {
            Toast.makeText(context, "New charge added successfully!", Toast.LENGTH_SHORT).show()
            newFeeChargeAmount = ""
            viewModel.fetchFees(token, student.id) { }
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
                    val matchingFees = studentsFees.filter { it.studentName == selectedStudent?.fullName }
                    
                    val amt = matchingFees.sumOf { it.amount.toDoubleOrNull() ?: 0.0 }
                    val previousPaid = matchingFees.sumOf { it.amountPaid?.toDoubleOrNull() ?: 0.0 }
                    val pendingAmount = amt - previousPaid
                    
                    val newlyPaid = paymentAmount.toDoubleOrNull() ?: 0.0
                    val dueAfterPayment = pendingAmount - newlyPaid

                    // RECORD PAYMENT CARD
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
                                Text("Record a Payment", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = BluePrimary)
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            Text("Current Pending Fees: ₹${String.format("%.2f", pendingAmount)}", fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            Text("Payment Amount (₹)", fontWeight = FontWeight.Bold, color = TextSecondary)
                            TextField(
                                value = paymentAmount,
                                onValueChange = { paymentAmount = it },
                                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                                colors = TextFieldDefaults.colors(focusedContainerColor = Color(0xFFE8F5E9), unfocusedContainerColor = Color(0xFFE8F5E9), unfocusedIndicatorColor = Color.Transparent, focusedIndicatorColor = Color.Transparent),
                                shape = RoundedCornerShape(8.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            if (newlyPaid > 0) {
                                FeeRowItem("Remaining Due After Payment", "₹${String.format("%.2f", dueAfterPayment)}", if (dueAfterPayment <= 0) Color(0xFF2E7D32) else Color(0xFFC62828))
                                Spacer(modifier = Modifier.height(16.dp))
                            }
                            
                            Button(
                                onClick = { handleRecordPayment() },
                                modifier = Modifier.fillMaxWidth().height(48.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = GreenAccent),
                                shape = RoundedCornerShape(12.dp),
                                enabled = !isUpdating 
                            ) {
                                if (isUpdating) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp))
                                else Text("Record Payment", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            }
                        }
                    }

                    // ADD NEW CHARGE CARD
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
                                Text("Add New Fee Charge", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = OrangeWarning)
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Use this to add fees for a new subject, term, or fine.", fontSize = 12.sp, color = TextSecondary)
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            Text("New Charge Amount (₹)", fontWeight = FontWeight.Bold, color = TextSecondary)
                            TextField(
                                value = newFeeChargeAmount,
                                onValueChange = { newFeeChargeAmount = it },
                                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                                colors = TextFieldDefaults.colors(focusedContainerColor = Color(0xFFFFF3E0), unfocusedContainerColor = Color(0xFFFFF3E0), unfocusedIndicatorColor = Color.Transparent, focusedIndicatorColor = Color.Transparent),
                                shape = RoundedCornerShape(8.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            Button(
                                onClick = { handleAddNewCharge() },
                                modifier = Modifier.fillMaxWidth().height(48.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = OrangeWarning),
                                shape = RoundedCornerShape(12.dp),
                                enabled = !isUpdating 
                            ) {
                                if (isUpdating) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp))
                                else Text("Add Charge", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                            }
                        }
                    }

                    // PAYMENT HISTORY SECTION
                    if (matchingFees.isNotEmpty()) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text("Archived Fee Records", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                                Spacer(modifier = Modifier.height(16.dp))
                                
                                FeeRowItem("Historical Total Fee", "₹${String.format("%.2f", amt)}", Color.Black)
                                FeeRowItem("Total Paid Cumulatively", "₹${String.format("%.2f", previousPaid)}", Color(0xFF2E7D32))
                            }
                        }
                    }
                }

                // STUDENT FEE SUMMARY & PDF EXPORT
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
                            Text("Student Fee Summary", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                            Button(
                                onClick = { exportToPdf(context, students, studentsFees) },
                                colors = ButtonDefaults.buttonColors(containerColor = BluePrimary)
                            ) {
                                Text("Export PDF", color = Color.White)
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        students.forEach { student ->
                            val matchingFees = studentsFees.filter { it.studentName == student.fullName }
                            val amt = matchingFees.sumOf { it.amount.toDoubleOrNull() ?: 0.0 }
                            val paid = matchingFees.sumOf { it.amountPaid?.toDoubleOrNull() ?: 0.0 }
                            val pending = amt - paid
                            
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(student.fullName, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                                Text("₹${String.format("%.2f", pending)}", 
                                     color = if (pending > 0) Color(0xFFC62828) else Color(0xFF2E7D32),
                                     fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }
                            HorizontalDivider(color = Color(0xFFEEEEEE))
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

fun exportToPdf(context: android.content.Context, students: List<com.example.frontend.models.StudentResponse>, studentsFees: List<com.example.frontend.models.FeeResponse>) {
    val document = android.graphics.pdf.PdfDocument()
    val pageInfo = android.graphics.pdf.PdfDocument.PageInfo.Builder(595, 842, 1).create()
    val page = document.startPage(pageInfo)
    val canvas = page.canvas
    val paint = android.graphics.Paint()
    
    paint.textSize = 18f
    paint.isFakeBoldText = true
    canvas.drawText("Student Fee Summary", 50f, 50f, paint)
    
    paint.textSize = 14f
    paint.isFakeBoldText = false
    var yPosition = 100f
    
    canvas.drawText("Student Name", 50f, yPosition, paint)
    canvas.drawText("Pending Amount (Rs)", 350f, yPosition, paint)
    
    yPosition += 20f
    canvas.drawLine(50f, yPosition, 500f, yPosition, paint)
    yPosition += 20f
    
    for (student in students) {
        val matchingFees = studentsFees.filter { it.studentName == student.fullName }
        val amt = matchingFees.sumOf { it.amount.toDoubleOrNull() ?: 0.0 }
        val paid = matchingFees.sumOf { it.amountPaid?.toDoubleOrNull() ?: 0.0 }
        val pending = amt - paid
        
        canvas.drawText(student.fullName, 50f, yPosition, paint)
        canvas.drawText(String.format("%.2f", pending), 350f, yPosition, paint)
        yPosition += 30f
        
        if (yPosition > 800f) break
    }
    
    document.finishPage(page)
    
    val directory = context.getExternalFilesDir(android.os.Environment.DIRECTORY_DOWNLOADS)
    val file = java.io.File(directory, "Fee_Summary_${System.currentTimeMillis()}.pdf")
    
    try {
        document.writeTo(java.io.FileOutputStream(file))
        android.widget.Toast.makeText(context, "PDF saved to Downloads folder", android.widget.Toast.LENGTH_LONG).show()
    } catch (e: Exception) {
        android.widget.Toast.makeText(context, "Error saving PDF: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
    }
    document.close()
}
