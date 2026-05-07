package com.example.frontend.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.frontend.ui.theme.KalviConnectTheme
import com.example.frontend.utils.TokenManager

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        // 1. Handle splash screen transition
        installSplashScreen()
        
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        setContent {
            KalviConnectTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color.White
                ) {
                    MainNavigation()
                }
            }
        }
    }
}

@Composable
fun MainNavigation() {
    val navController = rememberNavController()
    val context = LocalContext.current
    
    val tokenManager = remember { TokenManager(context) }
    
    // Calculate start destination immediately to avoid initial black frame / broken channel crashes
    val startDestination = remember {
        val token = tokenManager.getAccessToken()
        if (!token.isNullOrEmpty()) {
            val role = tokenManager.getUserRole()?.lowercase()?.trim()
            if (role == "teacher" || role == "admin") "teacher_dashboard" else "parent_dashboard"
        } else {
            "login"
        }
    }

    NavHost(
        navController = navController, 
        startDestination = startDestination
    ) {
        composable("login") { LoginScreen(navController) }
        composable("otp/{phone}/{role}") { backStackEntry ->
            val phone = backStackEntry.arguments?.getString("phone") ?: ""
            val role = backStackEntry.arguments?.getString("role") ?: ""
            OtpScreen(navController, phone, role)
        }
        composable("teacher_dashboard") { TeacherDashboardScreen(navController) }
        composable("parent_dashboard") { ParentDashboardScreen(navController) }
        composable("student_management") { StudentManagementScreen(navController) }
        composable("add_student") { AddStudentScreen(navController) }
        composable("attendance") { AttendanceScreen(navController) }
        composable("marks") { MarksScreen(navController) }
        composable("fees") { FeesScreen(navController) }
        composable("announcements") { AnnouncementsScreen(navController) }
        composable("settings") { SettingsScreen(navController) }
    }
}
