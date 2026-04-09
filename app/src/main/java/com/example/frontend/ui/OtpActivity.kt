package com.example.frontend.ui

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.frontend.R
import com.example.frontend.api.RetrofitClient
import com.example.frontend.models.VerifyOtpRequest
import com.example.frontend.models.VerifyOtpResponse
import com.example.frontend.utils.TokenManager
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class OtpActivity : AppCompatActivity() {

    private lateinit var etOtp: EditText
    private lateinit var btnVerifyOtp: Button
    private lateinit var progressBar: ProgressBar
    private lateinit var tokenManager: TokenManager
    private var phoneNumber: String = ""
    private var selectedRole: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_otp)

        tokenManager = TokenManager(this)
        phoneNumber = intent.getStringExtra("PHONE_NUMBER") ?: ""
        selectedRole = intent.getStringExtra("SELECTED_ROLE") ?: ""

        etOtp = findViewById(R.id.etOtp)
        btnVerifyOtp = findViewById(R.id.btnVerifyOtp)
        progressBar = findViewById(R.id.progressBar)

        btnVerifyOtp.setOnClickListener {
            val otp = etOtp.text.toString().trim()
            if (otp.length != 6) {
                Toast.makeText(this, getString(R.string.error_invalid_otp), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            verifyOtp(otp)
        }
    }

    private fun verifyOtp(otp: String) {
        showLoading(true)
        val finalRole = selectedRole.lowercase().trim()
        val request = VerifyOtpRequest(phoneNumber, otp, finalRole)
        
        Log.d("OTP_DEBUG", "Payload: phone=$phoneNumber, otp=$otp, role=$finalRole")

        RetrofitClient.instance.verifyOtp(request).enqueue(object : Callback<VerifyOtpResponse> {
            override fun onResponse(call: Call<VerifyOtpResponse>, response: Response<VerifyOtpResponse>) {
                showLoading(false)
                if (response.isSuccessful && response.body() != null) {
                    val body = response.body()!!
                    Log.d("OTP_DEBUG", "Response Success: $body")
                    
                    if (body.access != null && body.refresh != null && body.user != null) {
                        tokenManager.saveTokens(body.access, body.refresh)
                        tokenManager.saveUserRole(body.user.role)
                        tokenManager.saveUserPhone(body.user.phone)
                        if (body.student_id != null) {
                            tokenManager.saveStudentId(body.student_id)
                        }

                        val intent = Intent(this@OtpActivity, DashboardActivity::class.java)
                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        startActivity(intent)
                        finish()
                    } else {
                        Toast.makeText(this@OtpActivity, "Invalid response from server", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    val errorJson = response.errorBody()?.string() ?: "Unknown Error"
                    Log.e("OTP_DEBUG", "Response Error: $errorJson")
                    Toast.makeText(this@OtpActivity, "Error: $errorJson", Toast.LENGTH_LONG).show()
                }
            }

            override fun onFailure(call: Call<VerifyOtpResponse>, t: Throwable) {
                showLoading(false)
                Log.e("OTP_DEBUG", "Network failure: ${t.message}")
                Toast.makeText(this@OtpActivity, "Network Error: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun showLoading(isLoading: Boolean) {
        progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        btnVerifyOtp.isEnabled = !isLoading
    }
}
