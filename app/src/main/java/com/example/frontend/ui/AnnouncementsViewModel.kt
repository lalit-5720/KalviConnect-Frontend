package com.example.frontend.ui

import androidx.lifecycle.ViewModel
import com.example.frontend.api.RetrofitClient
import com.example.frontend.models.AnnouncementResponse
import com.example.frontend.models.CreateAnnouncementRequest
import com.example.frontend.models.StudentResponse
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class AnnouncementsViewModel : ViewModel() {
    private val _announcements = MutableStateFlow<List<AnnouncementResponse>>(emptyList())
    val announcements: StateFlow<List<AnnouncementResponse>> = _announcements

    private val _students = MutableStateFlow<List<StudentResponse>>(emptyList())
    val students: StateFlow<List<StudentResponse>> = _students

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    private val _isCreating = MutableStateFlow(false)
    val isCreating: StateFlow<Boolean> = _isCreating

    fun fetchAnnouncements(token: String) {
        _isLoading.value = true
        RetrofitClient.instance.getAnnouncements("Bearer $token")
            .enqueue(object : Callback<List<AnnouncementResponse>> {
                override fun onResponse(call: Call<List<AnnouncementResponse>>, response: Response<List<AnnouncementResponse>>) {
                    _isLoading.value = false
                    if (response.isSuccessful) {
                        _announcements.value = response.body() ?: emptyList()
                    } else {
                        _error.value = "Failed to fetch announcements"
                    }
                }

                override fun onFailure(call: Call<List<AnnouncementResponse>>, t: Throwable) {
                    _isLoading.value = false
                    _error.value = t.message ?: "Network error"
                }
            })
    }

    fun fetchStudents(token: String) {
        RetrofitClient.instance.getStudents("Bearer $token")
            .enqueue(object : Callback<List<StudentResponse>> {
                override fun onResponse(call: Call<List<StudentResponse>>, response: Response<List<StudentResponse>>) {
                    if (response.isSuccessful) {
                        _students.value = response.body() ?: emptyList()
                    }
                }
                override fun onFailure(call: Call<List<StudentResponse>>, t: Throwable) {
                    _error.value = t.message ?: "Failed to load students"
                }
            })
    }

    fun createAnnouncement(token: String, title: String, message: String, isPublic: Boolean, targetStudentId: String?, onSuccess: () -> Unit) {
        _isCreating.value = true
        val visibility = if (isPublic) "all" else if (targetStudentId != null) "specific_student" else "parents_only"
        val request = CreateAnnouncementRequest(title, message, visibility, targetStudentId)

        RetrofitClient.instance.createAnnouncement("Bearer $token", request)
            .enqueue(object : Callback<ResponseBody> {
                override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                    _isCreating.value = false
                    if (response.isSuccessful) {
                        onSuccess()
                        fetchAnnouncements(token) // Refresh
                    } else {
                        _error.value = "Error: ${response.code()}"
                    }
                }

                override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                    _isCreating.value = false
                    _error.value = t.message ?: "Network error"
                }
            })
    }

    fun clearError() {
        _error.value = null
    }
}
