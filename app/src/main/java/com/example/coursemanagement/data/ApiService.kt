package com.example.coursemanagement.data

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.*

interface ApiService {
    @GET("Categories") suspend fun getCategories(): List<Category>
    @POST("Categories") suspend fun addCategory(@Body category: Category): Category // Mới thêm
    @PUT("Categories/{id}") suspend fun updateCategory(@Path("id") id: String, @Body category: Category): Category // Mới thêm
    @DELETE("Categories/{id}") suspend fun deleteCategory(@Path("id") id: String)

    @GET("Courses") suspend fun getCourses(): List<Course>
    @POST("Courses") suspend fun addCourse(@Body course: Course): Course
    @PUT("Courses/{id}") suspend fun updateCourse(@Path("id") id: String, @Body course: Course): Course
    @DELETE("Courses/{id}") suspend fun deleteCourse(@Path("id") id: String)

    @GET("Categories/{categoryId}/Courses")
    suspend fun getCoursesByCategory(@Path("categoryId") categoryId: String): List<Course>
}

object RetrofitClient {
    val api: ApiService by lazy {
        Retrofit.Builder()
            .baseUrl("https://6adc43cefa5e73d4ab2d4c12.mockapi.io/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }
}