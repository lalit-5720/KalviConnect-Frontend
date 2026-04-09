package com.example.frontend.ui

import androidx.lifecycle.ViewModel
import com.example.frontend.api.RetrofitClient
import com.example.frontend.models.FeeResponse
import com.example.frontend.models.UpdateFeeRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class FeesViewModel : ViewModel() {
    private val _studentsFees = MutableStateFlow<List<FeeResponse>>(emptyList())
    val studentsFees: StateFlow<List<FeeResponse>> = _studentsFees

    private val _students = MutableStateFlow<List<com.example.frontend.models.StudentResponse>>(emptyList())
    val students: StateFlow<List<com.example.frontend.models.StudentResponse>> = _students

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _isUpdating = MutableStateFlow(false)
    val isUpdating: StateFlow<Boolean> = _isUpdating

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    fun fetchFees(token: String, selectedStudentId: String?, onUpdateSelection: (FeeResponse?) -> Unit) {
        _isLoading.value = true
        RetrofitClient.instance.getFees("Bearer $token")
            .enqueue(object : Callback<List<FeeResponse>> {
                override fun onResponse(call: Call<List<FeeResponse>>, response: Response<List<FeeResponse>>) {
                    _isLoading.value = false
                    if (response.isSuccessful) {
                        val currentList = response.body() ?: emptyList()
                        _studentsFees.value = currentList
                        
                        // Update selection reference if one was selected
                        if (selectedStudentId != null) {
                            val matching = currentList.find { it.id == selectedStudentId }
                            onUpdateSelection(matching)
                        }
                    } else {
                        _error.value = "Failed to load fee details"
                    }
                }

                override fun onFailure(call: Call<List<FeeResponse>>, t: Throwable) {
                    _isLoading.value = false
                    _error.value = t.message ?: "Network error"
                }
            })
    }

    fun updatePayment(token: String, studentId: String, amount: Double, onSuccess: () -> Unit) {
        _isUpdating.value = true
        val request = UpdateFeeRequest(studentId, amount)

        RetrofitClient.instance.updateFee("Bearer $token", request)
            .enqueue(object : Callback<ResponseBody> {
                override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                    _isUpdating.value = false
                    if (response.isSuccessful) {
                        onSuccess()
                    } else {
                        _error.value = "Failed to update payment"
                    }
                }

                override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                    _isUpdating.value = false
                    _error.value = t.message ?: "Network error"
                }
            })
    }

    fun fetchStudents(token: String) {
        RetrofitClient.instance.getStudents("Bearer $token")
            .enqueue(object : Callback<List<com.example.frontend.models.StudentResponse>> {
                override fun onResponse(
                    call: Call<List<com.example.frontend.models.StudentResponse>>,
                    response: Response<List<com.example.frontend.models.StudentResponse>>
                ) {
                    if (response.isSuccessful) {
                        _students.value = response.body() ?: emptyList()
                    }
                }
                override fun onFailure(call: Call<List<com.example.frontend.models.StudentResponse>>, t: Throwable) {
                    _error.value = "Failed to load students list"
                }
            })
    }

    fun createFeeRecord(token: String, studentId: String, totalAmount: Double, amountPaid: Double, onSuccess: () -> Unit) {
        _isUpdating.value = true
        val request = com.example.frontend.models.CreateFeeRequest(studentId, totalAmount, amountPaid)
        RetrofitClient.instance.createFee("Bearer $token", request)
            .enqueue(object : Callback<ResponseBody> {
                override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                    _isUpdating.value = false
                    if (response.isSuccessful) {
                        onSuccess()
                    } else {
                        _error.value = "Failed to create fee record"
                    }
                }
                override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                    _isUpdating.value = false
                    _error.value = t.message ?: "Network error"
                }
            })
    }
    
    fun clearError() {
        _error.value = null
    }
}
