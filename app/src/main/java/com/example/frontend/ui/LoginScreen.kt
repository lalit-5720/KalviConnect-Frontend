package com.example.frontend.ui

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.example.frontend.api.RetrofitClient
import com.example.frontend.models.SendOtpRequest
import com.example.frontend.models.SendOtpResponse
import com.example.frontend.ui.theme.BluePrimary
import com.example.frontend.ui.theme.GraySecondary
import com.example.frontend.ui.theme.KalviConnectTheme
import com.example.frontend.ui.theme.TextPrimary
import com.example.frontend.ui.theme.TextSecondary
import com.example.frontend.utils.TokenManager
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(navController: NavController) {
    var phoneNumber by remember { mutableStateOf("") }
    var selectedRole by remember { mutableStateOf("teacher") }
    var isLoading by remember { mutableStateOf(false) }
    
    val context = LocalContext.current
    val isPreview = LocalInspectionMode.current
    val tokenManager = remember { if (isPreview) null else TokenManager(context) }

    LaunchedEffect(Unit) {
        if (!isPreview) {
            try {
                tokenManager?.clearTokens()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = Color.White
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(48.dp))
            
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .background(BluePrimary, RoundedCornerShape(20.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "KC",
                    color = Color.White,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Welcome to Kalvi Academy",
                style = MaterialTheme.typography.titleLarge,
                color = TextPrimary
            )

            Text(
                text = "Academy Management System",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
                modifier = Modifier.padding(top = 4.dp)
            )

            Spacer(modifier = Modifier.height(48.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .background(GraySecondary, RoundedCornerShape(12.dp))
                    .padding(4.dp)
            ) {
                RoleButton(
                    text = "Teacher",
                    isSelected = selectedRole == "teacher",
                    modifier = Modifier.weight(1f),
                    onClick = { selectedRole = "teacher" }
                )
                RoleButton(
                    text = "Parent",
                    isSelected = selectedRole == "parent",
                    modifier = Modifier.weight(1f),
                    onClick = { selectedRole = "parent" }
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            OutlinedTextField(
                value = phoneNumber,
                onValueChange = { input -> 
                    // Only allow digits and max length 10
                    if (input.all { it.isDigit() } && input.length <= 10) {
                        phoneNumber = input
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Phone Number") },
                prefix = { Text("+91 ", fontWeight = FontWeight.Bold, color = TextPrimary) },
                leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null, tint = BluePrimary) },
                shape = RoundedCornerShape(12.dp),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = BluePrimary,
                    unfocusedBorderColor = GraySecondary,
                    focusedLabelColor = BluePrimary
                )
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = { 
                    if (phoneNumber.length == 10) {
                        isLoading = true
                        val fullPhone = "+91$phoneNumber"
                        val request = SendOtpRequest(fullPhone, selectedRole)
                        
                        RetrofitClient.instance.sendOtp(request).enqueue(object : Callback<SendOtpResponse> {
                            override fun onResponse(call: Call<SendOtpResponse>, response: Response<SendOtpResponse>) {
                                isLoading = false
                                if (response.isSuccessful) {
                                    val body = response.body()
                                    if (body?.otpCode != null) {
                                        Toast.makeText(context, "OTP is: ${body.otpCode}", Toast.LENGTH_LONG).show()
                                    }
                                    navController.navigate("otp/$fullPhone/$selectedRole")
                                } else {
                                    Toast.makeText(context, "Failed: ${response.code()}", Toast.LENGTH_SHORT).show()
                                }
                            }

                            override fun onFailure(call: Call<SendOtpResponse>, t: Throwable) {
                                isLoading = false
                                Toast.makeText(context, "Check Connection", Toast.LENGTH_SHORT).show()
                            }
                        })
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = BluePrimary),
                enabled = phoneNumber.length == 10 && !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                } else {
                    Text("Generate OTP", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            Text(
                text = "By continuing, you agree to our Terms & Privacy Policy",
                textAlign = TextAlign.Center,
                fontSize = 12.sp,
                color = TextSecondary
            )
        }
    }
}

@Composable
fun RoleButton(text: String, isSelected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Box(
        modifier = modifier
            .fillMaxHeight()
            .background(
                color = if (isSelected) Color.White else Color.Transparent,
                shape = RoundedCornerShape(8.dp)
            )
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = if (isSelected) BluePrimary else TextSecondary,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
        )
    }
}

@Preview(showBackground = true)
@Composable
fun LoginScreenPreview() {
    KalviConnectTheme {
        LoginScreen(navController = rememberNavController())
    }
}
