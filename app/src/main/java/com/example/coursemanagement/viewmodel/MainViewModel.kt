package com.example.coursemanagement.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.coursemanagement.data.*
import kotlinx.coroutines.launch

sealed class ConnectionState {
    object Loading : ConnectionState()
    object Success : ConnectionState()
    class Error(val message: String) : ConnectionState()
}

class MainViewModel : ViewModel() {
    val categories = mutableStateListOf<Category>()
    val courses = mutableStateListOf<Course>()

    var connectionState by mutableStateOf<ConnectionState>(ConnectionState.Loading)
        private set

    init { fetchData() }

    fun fetchData() {
        viewModelScope.launch {
            connectionState = ConnectionState.Loading
            try {
                val fetchedCategories = RetrofitClient.api.getCategories()
                val fetchedCourses = RetrofitClient.api.getCourses()
                categories.clear()
                categories.addAll(fetchedCategories)
                courses.clear()
                courses.addAll(fetchedCourses)
                connectionState = ConnectionState.Success
            } catch (e: Exception) {
                e.printStackTrace()
                connectionState = ConnectionState.Error("Lỗi kết nối MockAPI: ${e.localizedMessage}")
            }
        }
    }

    /* CRUD CATEGORY */
    fun addCategory(category: Category, onSuccess: () -> Unit) = viewModelScope.launch {
        try { RetrofitClient.api.addCategory(category); fetchData(); onSuccess() } catch (e: Exception) { e.printStackTrace() }
    }

    fun updateCategory(id: String, category: Category, onSuccess: () -> Unit) = viewModelScope.launch {
        try { RetrofitClient.api.updateCategory(id, category); fetchData(); onSuccess() } catch (e: Exception) { e.printStackTrace() }
    }

    fun deleteCategory(id: String, onFail: () -> Unit) {
        if (courses.any { it.categoryId == id }) {
            onFail()
        } else {
            viewModelScope.launch {
                try { RetrofitClient.api.deleteCategory(id); fetchData() } catch (e: Exception) { e.printStackTrace() }
            }
        }
    }

    /* CRUD COURSE */
    fun addCourse(course: Course, onSuccess: () -> Unit) = viewModelScope.launch {
        try { RetrofitClient.api.addCourse(course); fetchData(); onSuccess() } catch (e: Exception) { e.printStackTrace() }
    }

    fun updateCourse(id: String, course: Course, onSuccess: () -> Unit) = viewModelScope.launch {
        try { RetrofitClient.api.updateCourse(id, course); fetchData(); onSuccess() } catch (e: Exception) { e.printStackTrace() }
    }

    fun deleteCourse(id: String) = viewModelScope.launch {
        try { RetrofitClient.api.deleteCourse(id); fetchData() } catch (e: Exception) { e.printStackTrace() }
    }
}