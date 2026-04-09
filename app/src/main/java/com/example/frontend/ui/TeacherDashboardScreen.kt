package com.example.frontend.ui

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.frontend.api.RetrofitClient
import com.example.frontend.models.DashboardStats
import com.example.frontend.ui.theme.BluePrimary
import com.example.frontend.utils.TokenManager
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeacherDashboardScreen(navController: NavController) {
    val context = LocalContext.current
    val tokenManager = remember { TokenManager(context) }
    val scrollState = rememberScrollState()
    
    var stats by remember { mutableStateOf<DashboardStats?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    // Fetch stats from backend
    LaunchedEffect(Unit) {
        val token = tokenManager.getAccessToken()
        if (token != null) {
            RetrofitClient.instance.getTeacherDashboardStats("Bearer $token")
                .enqueue(object : Callback<DashboardStats> {
                    override fun onResponse(call: Call<DashboardStats>, response: Response<DashboardStats>) {
                        isLoading = false
                        if (response.isSuccessful) {
                            stats = response.body()
                        } else {
                            Toast.makeText(context, "Failed to load dashboard data", Toast.LENGTH_SHORT).show()
                        }
                    }

                    override fun onFailure(call: Call<DashboardStats>, t: Throwable) {
                        isLoading = false
                        Toast.makeText(context, "Network error: ${t.message}", Toast.LENGTH_SHORT).show()
                    }
                })
        }
    }

    Scaffold(
        topBar = {
            Surface(shadowElevation = 4.dp) {
                TopAppBar(
                    title = {
                        Text(
                            "Kalvi Academy",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    actions = {
                        IconButton(onClick = {
                            navController.navigate("settings")
                        }) {
                            Icon(Icons.Default.Settings, contentDescription = "Settings", tint = Color.White)
                        }
                        IconButton(onClick = {
                            tokenManager.clearTokens()
                            navController.navigate("login") {
                                popUpTo(0)
                            }
                        }) {
                            Icon(Icons.Default.ExitToApp, contentDescription = "Logout", tint = Color.White)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = BluePrimary)
                )
            }
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
                    .verticalScroll(scrollState)
                    .padding(16.dp)
            ) {
                // Stats Row 1
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    StatCard(
                        title = "Total Students",
                        value = "${stats?.total_students ?: 0}",
                        icon = Icons.Default.Group,
                        iconTint = Color(0xFF3B82F6),
                        iconBg = Color(0xFFEFF6FF),
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        title = "Pending Fees",
                        value = "₹${stats?.pending_fees?.toInt() ?: 0}",
                        icon = Icons.Default.ErrorOutline,
                        iconTint = Color(0xFFEF4444),
                        iconBg = Color(0xFFFEF2F2),
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Stats Row 2
                StatCard(
                    title = "Recent Announcements",
                    value = "${stats?.recent_announcements ?: 0}",
                    icon = Icons.Default.TrendingUp,
                    iconTint = Color(0xFF10B981),
                    iconBg = Color(0xFFECFDF5),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = "Quick Actions",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF374151),
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                // Quick Actions Grid
                GridRow {
                    ActionCard(
                        title = "Student\nManagement",
                        icon = Icons.Default.Group,
                        color = Color(0xFF3B82F6),
                        modifier = Modifier.weight(1f)
                    ) { navController.navigate("student_management") }
                    
                    ActionCard(
                        title = "Attendance",
                        icon = Icons.Default.AssignmentTurnedIn,
                        color = Color(0xFF4ADE80),
                        modifier = Modifier.weight(1f)
                    ) { navController.navigate("attendance") }
                }

                Spacer(modifier = Modifier.height(16.dp))

                GridRow {
                    ActionCard(
                        title = "Marks",
                        icon = Icons.Default.EmojiEvents,
                        color = Color(0xFFA855F7),
                        modifier = Modifier.weight(1f)
                    ) { navController.navigate("marks") }
                    
                    ActionCard(
                        title = "Fees",
                        icon = Icons.Default.Payments,
                        color = Color(0xFFF97316),
                        modifier = Modifier.weight(1f)
                    ) { navController.navigate("fees") }
                }

                Spacer(modifier = Modifier.height(16.dp))

                GridRow {
                    ActionCard(
                        title = "Announcements",
                        icon = Icons.Default.Campaign,
                        color = Color(0xFFEF4444),
                        modifier = Modifier.weight(1f)
                    ) { navController.navigate("announcements") }
                    
                    Spacer(modifier = Modifier.weight(1f))
                }
                
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
fun StatCard(
    title: String,
    value: String,
    icon: ImageVector,
    iconTint: Color,
    iconBg: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(iconBg, RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(20.dp))
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(title, fontSize = 12.sp, color = Color(0xFF6B7280))
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(value, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color(0xFF111827))
        }
    }
}

@Composable
fun ActionCard(
    title: String,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .height(150.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .background(color, RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(28.dp))
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = title,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF374151),
                textAlign = TextAlign.Center,
                lineHeight = 18.sp
            )
        }
    }
}

@Composable
fun GridRow(content: @Composable RowScope.() -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        content = content
    )
}
