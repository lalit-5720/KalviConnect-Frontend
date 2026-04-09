package com.example.frontend.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.example.frontend.R
import com.example.frontend.api.RetrofitClient
import com.example.frontend.models.SendOtpRequest
import com.example.frontend.models.SendOtpResponse
import com.example.frontend.utils.TokenManager
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class LoginActivity : AppCompatActivity() {

    private lateinit var etPhone: EditText
    private lateinit var btnSendOtp: Button
    private lateinit var progressBar: ProgressBar
    private lateinit var tokenManager: TokenManager
    private lateinit var tvTeacher: TextView
    private lateinit var tvParent: TextView
    private var selectedRole: String = "teacher"

    override fun onCreate(savedInstanceState: Bundle?) {
        // Handle the splash screen transition.
        installSplashScreen()
        
        super.onCreate(savedInstanceState)

        tokenManager = TokenManager(this)
        
        // Auto-login check BEFORE setContentView to prevent UI flash
        if (!tokenManager.getAccessToken().isNullOrEmpty()) {
            val intent = Intent(this, DashboardActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
            return
        }

        setContentView(R.layout.activity_login)

        etPhone = findViewById(R.id.etPhone)
        btnSendOtp = findViewById(R.id.btnSendOtp)
        progressBar = findViewById(R.id.progressBar)
        tvTeacher = findViewById(R.id.tvTeacher)
        tvParent = findViewById(R.id.tvParent)

        // Initialize segmented control
        updateRoleUI()

        tvTeacher.setOnClickListener {
            selectedRole = "teacher"
            updateRoleUI()
        }

        tvParent.setOnClickListener {
            selectedRole = "parent"
            updateRoleUI()
        }

        btnSendOtp.setOnClickListener {
            val phone = etPhone.text.toString().trim()
            if (phone.isEmpty() || phone.length < 10) {
                Toast.makeText(this, getString(R.string.error_invalid_phone), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            sendOtp(phone)
        }
    }

    private fun updateRoleUI() {
        if (selectedRole == "teacher") {
            tvTeacher.setBackgroundResource(R.drawable.bg_segmented_active)
            tvTeacher.setTextColor(ContextCompat.getColor(this, R.color.white))
            
            tvParent.setBackgroundResource(0)
            tvParent.setTextColor(ContextCompat.getColor(this, R.color.textColorSecondary))
        } else {
            tvParent.setBackgroundResource(R.drawable.bg_segmented_active)
            tvParent.setTextColor(ContextCompat.getColor(this, R.color.white))
            
            tvTeacher.setBackgroundResource(0)
            tvTeacher.setTextColor(ContextCompat.getColor(this, R.color.textColorSecondary))
        }
    }

    private fun sendOtp(phone: String) {
        showLoading(true)
        val request = SendOtpRequest(phone, selectedRole)
        
        RetrofitClient.instance.sendOtp(request).enqueue(object : Callback<SendOtpResponse> {
            override fun onResponse(call: Call<SendOtpResponse>, response: Response<SendOtpResponse>) {
                showLoading(false)
                if (response.isSuccessful) {
                    val body = response.body()
                    if (body?.otpCode != null) {
                        Toast.makeText(this@LoginActivity, "OTP is: ${body.otpCode}", Toast.LENGTH_LONG).show()
                    }
                    val intent = Intent(this@LoginActivity, OtpActivity::class.java)
                    intent.putExtra("PHONE_NUMBER", phone)
                    intent.putExtra("SELECTED_ROLE", selectedRole)
                    startActivity(intent)
                } else {
                    Toast.makeText(this@LoginActivity, "Failed to send OTP", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<SendOtpResponse>, t: Throwable) {
                showLoading(false)
                Toast.makeText(this@LoginActivity, "Network Error: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun showLoading(isLoading: Boolean) {
        progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        btnSendOtp.isEnabled = !isLoading
    }
}
