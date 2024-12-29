package com.example.loginmultiplatform.viewmodel


import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.loginmultiplatform.model.ResponseWrapper
import com.example.loginmultiplatform.model.TeacherAssignmentRequest
import com.example.loginmultiplatform.model.TeacherClassResponse
import com.example.loginmultiplatform.model.TeacherCourseResponse
import com.example.loginmultiplatform.repository.TeacherHomeworkRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import retrofit2.HttpException


class TeacherAssignmentViewModel : ViewModel(){

    private val repository = TeacherHomeworkRepository()

    private val _teacherClasses = MutableStateFlow<List<TeacherClassResponse>>(emptyList())
    val teacherClasses: StateFlow<List<TeacherClassResponse>> = _teacherClasses

    private val _courseId = MutableStateFlow<List<TeacherCourseResponse>>(emptyList())
    val courseId: StateFlow<List<TeacherCourseResponse>> = _courseId


    private val _courseSearchId = MutableStateFlow<List<TeacherCourseResponse>>(emptyList())
    val courseSearchId: StateFlow<List<TeacherCourseResponse>> = _courseSearchId

    private val _coursePastId = MutableStateFlow<List<TeacherCourseResponse>>(emptyList())
    val coursePastId: StateFlow<List<TeacherCourseResponse>> = _coursePastId

    private val _isLoading = MutableStateFlow(false)
    //val isLoading: StateFlow<Boolean> = _isLoading

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage

    private val _isSaving = MutableStateFlow(false)

    private val _saveResult = MutableStateFlow<ResponseWrapper<Int>?>(null)

    private val _bulkOperationStatus = MutableStateFlow<String?>(null)
    val bulkOperationStatus: StateFlow<String?> = _bulkOperationStatus

    private val _saveError = MutableStateFlow<String?>(null)


    fun addAssignment (newAssignment : TeacherAssignmentRequest) {
        viewModelScope.launch {
            _isSaving.value = true

            try {
                val response = repository.TeacherNewAssignment(newAssignment)
                _saveResult.value = response
                _bulkOperationStatus.value = "İşlem başarılı bir şekilde tamamlandı"
            } catch (e : HttpException){
                _saveError.value = "Bir hata oluştu ${e.message()}"
                _bulkOperationStatus.value = "İşlem sırasında hata oluştu!"
            } catch (e: Exception) {
                _saveError.value = "Bir hata oluştu: ${e.localizedMessage}"
                _bulkOperationStatus.value = "İşlem sırasında hata oluştu!"
            } finally {
                _isLoading.value = false
            }
        }


    }



    fun fetchTeacherClasses() {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null

            try {
                val classes = repository.getTeacherClasses()
                if (classes.success) {
                    _teacherClasses.value = classes.data
                } else {
                    _errorMessage.value = classes.message
                }
            } catch (e: Exception) {
                _errorMessage.value = "Bir hata oluştu: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }


    // These three fetch are same but I want to seperate datas for adding homework and diaplying homeworks in UI
    fun fetchTeacherCourses(teacherId : Int) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null

            try {
                val courses = repository.getTeacherClassCourses(teacherId)
                if (courses.success) {
                    _courseId.value = courses.data
                } else {
                    _errorMessage.value = courses.message
                }
            } catch (e: Exception) {
                _errorMessage.value = "Bir hata oluştu: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun fetchTeacherCoursesSearch(teacherId : Int) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null

            try {
                val courses = repository.getTeacherClassCourses(teacherId)
                if (courses.success) {
                    _courseSearchId.value = courses.data
                } else {
                    _errorMessage.value = courses.message
                }
            } catch (e: Exception) {
                _errorMessage.value = "Bir hata oluştu: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun fetchTeacherCoursesPast(teacherId : Int,) {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null

            try {
                val courses = repository.getTeacherClassCourses(teacherId)
                if (courses.success) {
                    _coursePastId.value = courses.data
                } else {
                    _errorMessage.value = courses.message
                }
            } catch (e: Exception) {
                _errorMessage.value = "Bir hata oluştu: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }





}