package com.example.frontend.ui

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.frontend.api.RetrofitClient
import com.example.frontend.models.VerifyOtpRequest
import com.example.frontend.models.VerifyOtpResponse
import com.example.frontend.ui.theme.BluePrimary
import com.example.frontend.ui.theme.GraySecondary
import com.example.frontend.ui.theme.TextPrimary
import com.example.frontend.ui.theme.TextSecondary
import com.example.frontend.utils.TokenManager
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OtpScreen(navController: NavController, phone: String, role: String) {
    var otpValue by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val tokenManager = remember { TokenManager(context) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        containerColor = Color.White
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Verify OTP",
                style = MaterialTheme.typography.titleLarge,
                color = TextPrimary
            )

            Text(
                text = "We've sent a 6-digit code to $phone",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 8.dp)
            )

            Spacer(modifier = Modifier.height(48.dp))

            // OTP Input
            OutlinedTextField(
                value = otpValue,
                onValueChange = { if (it.length <= 6) otpValue = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("000000", modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center) },
                shape = RoundedCornerShape(12.dp),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                singleLine = true,
                textStyle = LocalTextStyle.current.copy(
                    textAlign = TextAlign.Center,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 8.sp
                ),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = BluePrimary,
                    unfocusedBorderColor = GraySecondary
                )
            )

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = { 
                    if (otpValue.length == 6) {
                        isLoading = true
                        val finalRole = role.lowercase().trim()
                        val request = VerifyOtpRequest(phone, otpValue, finalRole)
                        
                        Log.d("OTP_DEBUG", "Payload: phone=$phone, otp=$otpValue, role=$finalRole")

                        RetrofitClient.instance.verifyOtp(request).enqueue(object : Callback<VerifyOtpResponse> {
                            override fun onResponse(call: Call<VerifyOtpResponse>, response: Response<VerifyOtpResponse>) {
                                isLoading = false
                                if (response.isSuccessful && response.body() != null) {
                                    val body = response.body()!!
                                    Log.d("OTP_DEBUG", "Response Success: $body")
                                    
                                    val user = body.user ?: return
                                    val confirmedRole = user.role.lowercase().trim().ifEmpty { finalRole }
                                    
                                    tokenManager.saveTokens(body.access!!, body.refresh!!)
                                    tokenManager.saveUserRole(confirmedRole)
                                    tokenManager.saveUserPhone(user.phone)

                                    if (confirmedRole == "teacher" || confirmedRole == "admin") {
                                        navController.navigate("teacher_dashboard") {
                                            popUpTo("login") { inclusive = true }
                                        }
                                    } else {
                                        navController.navigate("parent_dashboard") {
                                            popUpTo("login") { inclusive = true }
                                        }
                                    }
                                } else {
                                    val errorJson = response.errorBody()?.string() ?: "Unknown Error"
                                    Log.e("OTP_DEBUG", "Response Error: $errorJson")
                                    Toast.makeText(context, "Error: $errorJson", Toast.LENGTH_LONG).show()
                                }
                            }

                            override fun onFailure(call: Call<VerifyOtpResponse>, t: Throwable) {
                                isLoading = false
                                Log.e("OTP_DEBUG", "Network failure: ${t.message}")
                                Toast.makeText(context, "Network Error: ${t.message}", Toast.LENGTH_SHORT).show()
                            }
                        })
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = BluePrimary),
                enabled = otpValue.length == 6 && !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                } else {
                    Text("Verify & Continue", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            TextButton(onClick = { /* Resend */ }) {
                Text("Resend Code", color = BluePrimary, fontWeight = FontWeight.Bold)
            }
        }
    }
}
