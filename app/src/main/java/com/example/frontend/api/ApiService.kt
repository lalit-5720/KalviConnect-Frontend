package com.example.frontend.api

import com.example.frontend.models.*
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.http.*

interface ApiService {
    @POST("/api/auth/send-otp/")
    fun sendOtp(@Body request: SendOtpRequest): Call<SendOtpResponse>

    @POST("/api/auth/verify-otp/")
    fun verifyOtp(@Body request: VerifyOtpRequest): Call<VerifyOtpResponse>

    @GET("/api/teacher/dashboard-stats/")
    fun getTeacherDashboardStats(@Header("Authorization") token: String): Call<DashboardStats>

    @GET("/api/teacher/students/")
    fun getStudents(
        @Header("Authorization") token: String,
        @Query("search") query: String? = null
    ): Call<List<StudentResponse>>

    @POST("/api/teacher/students/")
    fun createStudent(
        @Header("Authorization") token: String,
        @Body request: CreateStudentRequest
    ): Call<ResponseBody>

    @GET("/api/teacher/attendance/")
    fun getAttendance(
        @Header("Authorization") token: String,
        @Query("date") date: String,
        @Query("class") className: String? = null
    ): Call<List<AttendanceResponse>>
    @GET("/api/teacher/attendance/")
    fun getStudentAttendanceHistory(
        @Header("Authorization") token: String,
        @Query("student_id") studentId: String
    ): Call<List<AttendanceResponse>>

    @POST("/api/teacher/attendance/bulk-mark/")
    fun saveAttendance(
        @Header("Authorization") token: String,
        @Body request: BulkAttendanceRequest
    ): Call<ResponseBody>

    @GET("/api/teacher/marks/")
    fun getMarks(
        @Header("Authorization") token: String,
        @Query("subject") subject: String,
        @Query("exam") exam: String
    ): Call<List<MarkResponse>>

    @POST("/api/teacher/marks/")
    fun createMarks(
        @Header("Authorization") token: String,
        @Body request: CreateMarkRequest
    ): Call<ResponseBody>

    @GET("/api/teacher/fees/")
    fun getFees(
        @Header("Authorization") token: String
    ): Call<List<FeeResponse>>

    @POST("/api/teacher/fees/")
    fun createFee(
        @Header("Authorization") token: String,
        @Body request: CreateFeeRequest
    ): Call<ResponseBody>

    @POST("/api/teacher/fees/record-payment/")
    fun updateFee(
        @Header("Authorization") token: String,
        @Body request: UpdateFeeRequest
    ): Call<ResponseBody>

    @DELETE("/api/teacher/students/{id}/")
    fun deleteStudent(
        @Header("Authorization") token: String,
        @Path("id") studentId: String
    ): Call<ResponseBody>

    @GET("/api/teacher/announcements/")
    fun getAnnouncements(
        @Header("Authorization") token: String
    ): Call<List<AnnouncementResponse>>

    @POST("/api/teacher/announcements/")
    fun createAnnouncement(
        @Header("Authorization") token: String,
        @Body request: CreateAnnouncementRequest
    ): Call<ResponseBody>

    @DELETE("/api/teacher/announcements/{id}/")
    fun deleteAnnouncement(
        @Header("Authorization") token: String,
        @Path("id") announcementId: String
    ): Call<ResponseBody>

    @GET("/api/parent/dashboard/")
    fun getParentDashboard(
        @Header("Authorization") token: String
    ): Call<ParentDashboardResponse>
}
