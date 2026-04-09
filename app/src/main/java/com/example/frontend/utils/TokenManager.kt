package com.example.frontend.utils

import android.content.Context
import android.content.SharedPreferences

class TokenManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("kalviconnect_prefs", Context.MODE_PRIVATE)

    fun saveTokens(accessToken: String, refreshToken: String) {
        prefs.edit().apply {
            putString("ACCESS_TOKEN", accessToken)
            putString("REFRESH_TOKEN", refreshToken)
            apply()
        }
    }

    fun saveUserRole(role: String) {
        prefs.edit().putString("USER_ROLE", role).apply()
    }
    
    fun saveUserPhone(phone: String) {
        prefs.edit().putString("USER_PHONE", phone).apply()
    }

    fun saveStudentId(studentId: String) {
        prefs.edit().putString("STUDENT_ID", studentId).apply()
    }

    fun getAccessToken(): String? = prefs.getString("ACCESS_TOKEN", null)
    
    fun getUserRole(): String? = prefs.getString("USER_ROLE", null)
    
    fun getUserPhone(): String? = prefs.getString("USER_PHONE", null)

    fun getStudentId(): String? = prefs.getString("STUDENT_ID", null)

    fun clearTokens() {
        prefs.edit().clear().apply()
    }
}
