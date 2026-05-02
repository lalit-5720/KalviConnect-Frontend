package com.example.frontend.models

import com.google.gson.annotations.SerializedName

data class SendOtpResponse(
    val message: String,
    @SerializedName("otp_code") val otpCode: String? = null
)

data class VerifyOtpResponse(
    val message: String? = null,
    val access: String? = null,
    val refresh: String? = null,
    val user: User? = null,
    val student_id: String? = null,
    val error: String? = null,
    @SerializedName("is_used") val isUsed: Boolean? = null
)

data class User(
    val id: String,
    val phone: String,
    val role: String,
    @SerializedName("full_name") val fullName: String? = null
)

data class DashboardStats(
    val total_students: Int,
    val pending_fees: Double,
    val recent_announcements: Int
)

data class StudentResponse(
    val id: String,
    @SerializedName("full_name") val fullName: String,
    @SerializedName("class_name") val className: String,
    val section: String,
    @SerializedName("parent_name") val parentName: String,
    @SerializedName("parent_phone") val parentPhone: String,
    val address: String? = null,
    val dob: String? = null,
    val photo_url: String? = null,
    val teacher: String? = null,
    val parent: String? = null,
    val created_at: String? = null,
    val subjects: List<String>? = null,
    @SerializedName("attendance_percentage") val attendance_percentage: Float? = null
)

data class AttendanceResponse(
    val id: String,
    @SerializedName("student_name") val studentName: String,
    @SerializedName("is_present") val isPresent: Boolean,
    val date: String,
    @SerializedName("marked_by") val markedBy: String? = null
)

data class MarkResponse(
    val id: String,
    val student: String,
    @SerializedName("student_name") val studentName: String,
    @SerializedName("exam_type") val examType: String,
    val subject: String,
    @SerializedName("marks_obtained") val marksObtained: Float,
    @SerializedName("max_marks") val maxMarks: Float,
    val grade: String,
    @SerializedName("exam_date") val examDate: String,
    val created_at: String
)

data class FeeResponse(
    val id: String,
    @SerializedName("student_name") val studentName: String,
    val status: String,
    @SerializedName("total_amount") val amount: String,
    @SerializedName("amount_paid") val amountPaid: String? = null,
    @SerializedName("last_payment_date") val date: String? = null,
    @SerializedName("due_date") val dueDate: String? = null,
    val history: List<PaymentHistory>? = null
)

data class PaymentHistory(
    val id: String,
    val title: String,
    val amount: String,
    val date: String
)

data class AnnouncementResponse(
    val id: String,
    val title: String,
    val message: String,
    val sender: String? = null,
    @SerializedName("target_class") val targetClass: String? = null,
    @SerializedName("target_student_name") val targetStudentName: String? = null,
    val visibility: String,
    val created_at: String
)

data class ParentStudentData(
    val id: String,
    val full_name: String,
    val class_name: String,
    val section: String,
    val parent_name: String,
    val parent_phone: String,
    val address: String? = null,
    val dob: String? = null,
    val photo_url: String? = null,
    val teacher: String? = null,
    val parent: String? = null,
    val created_at: String? = null,
    val attendance_percentage: Float,
    val pending_fees: Float,
    val recent_marks: List<MarkResponse>,
    val recent_attendance: List<AttendanceResponse>? = null,
    val fee_details: List<FeeResponse>? = null
)

data class ParentDashboardResponse(
    val students: List<ParentStudentData>,
    val announcements: List<AnnouncementResponse>
)
