package com.prostologik.lv12.ui.home

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.io.File

data class PhotoUiState(
    val photoDirectory: File? = null, // "/storage/emulated/0/Android/media/com.prostologik.lv12/image",
    val photoUriDir: String? = "file://$photoDirectory/",
    val photoFileName: String? = "cup.jpg",
    val photoSavedUri: String? = "file://$photoDirectory/$photoFileName",
    val photoLt: Int = 0,
    val photoLg: Int = 0,
    val photoH: Int = 0,
    val photoAz: Int = 0,
    val photoL: Int = 0,
    val photoTimeDate: Int = 0,

    val numberOfPhotos: Int = 0,
)

class HomeViewModel : ViewModel() {

    // Expose screen UI state
    private val _uiState = MutableStateFlow(PhotoUiState())
    val uiState: StateFlow<PhotoUiState> = _uiState.asStateFlow()

    // Handle business logic
    fun setPhotoUiState(outputDirectory: File) {
        _uiState.update { currentState ->
            currentState.copy(
                photoDirectory = outputDirectory,
                numberOfPhotos = currentState.numberOfPhotos + 1,
            )
        }
    }

    var photoDirectory2: File? = null

    private var _photoDirectory = MutableLiveData<File>().apply {
        value = null
    }
    val photoDirectory: LiveData<File> = _photoDirectory
    fun addFile(file: File) {
        _photoDirectory.value = file
    }

    private var _test = MutableLiveData<String>().apply {
        value = "112"
    }
    val test: LiveData<String> = _test
    fun changeValue(test: String) {
        _test.value = test
    }

    private val _text = MutableLiveData<String>().apply {
        value = "HomeViewModel text"
    }
    val text: LiveData<String> = _text
}