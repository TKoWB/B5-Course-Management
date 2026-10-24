package com.example.coursemanagement.data

data class Category(val id: String = "", val name: String = "", val description: String = "")

data class Course(
    val id: String = "", val title: String = "", val description: String = "",
    val categoryId: String = "", val instructor: String = "", val price: Long = 0,
    val duration: Int = 0, val thumbnail: String = "", val trailerUrl: String = ""
)