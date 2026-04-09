package com.example.frontend.models

import com.google.gson.annotations.SerializedName

data class SendOtpRequest(
    val phone: String,
    val role: String
)

data class VerifyOtpRequest(
    val phone: String,
    @SerializedName("otp") val otp: String,
    val role: String
)

data class CreateStudentRequest(
    val full_name: String,
    val class_name: String,
    val section: String,
    @SerializedName("parent_name") val parentName: String,
    @SerializedName("parent_phone") val parentPhone: String,
    val address: String? = null,
    val dob: String? = null,
    val subjects: List<String>? = null
)

data class AttendanceRequest(
    val student_id: String,
    val date: String,
    val is_present: Boolean,
    @SerializedName("marked_by") val markedBy: String? = null
)

data class CreateMarkRequest(
    @SerializedName("student") val studentId: String,
    val subject: String,
    @SerializedName("exam_type") val examType: String,
    @SerializedName("marks_obtained") val marksObtained: Float,
    @SerializedName("max_marks") val maxMarks: Float,
    val grade: String? = null,
    @SerializedName("exam_date") val examDate: String
)

data class UpdateFeeRequest(
    @SerializedName("student_id") val studentId: String,
    @SerializedName("amount_paid") val amountPaid: Double
)

data class CreateFeeRequest(
    @SerializedName("student") val studentId: String,
    @SerializedName("total_amount") val totalAmount: Double,
    @SerializedName("amount_paid") val amountPaid: Double
)

data class CreateAnnouncementRequest(
    val title: String,
    val message: String,
    val visibility: String,
    @SerializedName("target_student") val targetStudentId: String? = null
)
