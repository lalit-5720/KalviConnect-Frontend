package com.example.frontend.ui

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.frontend.R
import com.example.frontend.utils.TokenManager

class DashboardActivity : AppCompatActivity() {

    private lateinit var tokenManager: TokenManager
    private lateinit var tvDashboardTitle: TextView
    private lateinit var tvPhone: TextView
    private lateinit var tvRole: TextView
    private lateinit var ivLogout: ImageView
    private lateinit var appBarLayout: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dashboard)

        tokenManager = TokenManager(this)

        tvDashboardTitle = findViewById(R.id.tvDashboardTitle)
        tvPhone = findViewById(R.id.tvPhone)
        tvRole = findViewById(R.id.tvRole)
        ivLogout = findViewById(R.id.ivLogout)
        appBarLayout = findViewById(R.id.appBarLayout)

        setupDashboard()

        ivLogout.setOnClickListener {
            // Logout
            tokenManager.clearTokens()
            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }
    }

    private fun setupDashboard() {
        val role = tokenManager.getUserRole() ?: "parent"
        val phone = tokenManager.getUserPhone() ?: "N/A"

        tvPhone.text = phone
        tvRole.text = role

        // Apply dynamic design based on role (Teacher or Parent)
        if (role.lowercase() == "teacher") {
            tvDashboardTitle.text = getString(R.string.teacher_dashboard_title)
            appBarLayout.setBackgroundColor(getColor(R.color.colorPrimary)) // Blue for Teacher
        } else {
            // Default to Parent design
            tvDashboardTitle.text = getString(R.string.parent_dashboard_title)
            appBarLayout.setBackgroundColor(getColor(R.color.colorAccent)) // Green for Parent
        }
    }
}
