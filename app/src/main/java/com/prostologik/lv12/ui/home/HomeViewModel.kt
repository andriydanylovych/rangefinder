package com.prostologik.lv12.ui.home

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

//data class PhotoUiState(
//    val photoDirectory: String? = null, // "/storage/emulated/0/Android/media/com.prostologik.lv12/image",
//    val photoUriDir: String? = "file://$photoDirectory/",
//    val photoFileName: String? = "cup.jpg",
//    val photoSavedUri: String? = "file://$photoDirectory/$photoFileName",
//    val photoLt: Int = 0,
//    val photoLg: Int = 0,
//    val photoH: Int = 0,
//    val photoAz: Int = 0,
//    val photoL: Int = 0,
//    val photoTimeDate: Int = 0,
//
//    val numberOfPhotos: Int = 0,
//)

class HomeViewModel : ViewModel() {

    private var _photoDirectory = MutableLiveData<String>().apply { value = null }
    // "/storage/emulated/0/Android/media/com.prostologik.lv12/image"
    val photoDirectory: LiveData<String> = _photoDirectory
    fun setPhotoDirectory(dir: String) {
        _photoDirectory.value = dir
    }

//    var photoDirectory = "/storage/emulated/0/Android/media/com.prostologik.lv12/image"
//    fun setPhotoDirectory2(dir: String) {
//        photoDirectory = dir
//    }

    private var _photoFileName = MutableLiveData<String>().apply { value = null }
    val photoFileName: LiveData<String> = _photoFileName
    fun setPhotoFileName(fileName: String) {
        _photoFileName.value = fileName
    }

    private var _snippetWidth = MutableLiveData<Int>().apply { value = 64 }
    val snippetWidth: LiveData<Int> = _snippetWidth
    fun setSnippetWidth(value: Int) {
        if (value in 1..256) _snippetWidth.value = value
    }

    private var _snippetHeight = MutableLiveData<Int>().apply { value = 64 }
    val snippetHeight: LiveData<Int> = _snippetHeight
    fun setSnippetHeight(value: Int) {
        if (value in 1..256) _snippetHeight.value = value
    }

    private var _snippetLayer = MutableLiveData<Int>().apply { value = 0 }
    val snippetLayer: LiveData<Int> = _snippetLayer
    fun setSnippetLayer(value: Int) {
        if (value in 0..2) _snippetLayer.value = value
    }

    private var _analyzerOption = MutableLiveData<Int>().apply { value = 0 }
    val analyzerOption: LiveData<Int> = _analyzerOption
    fun setAnalyzerOption(value: Int) {
        if (value in 0..2) _analyzerOption.value = value
    }

    private var _resolutionWidth = MutableLiveData<Int>().apply { value = 640 }
    val resolutionWidth: LiveData<Int> = _resolutionWidth
    fun setResolutionWidth(value: Int) {
        if (value in 1..12800) _resolutionWidth.value = value
    }

    private var _resolutionHeight = MutableLiveData<Int>().apply { value = 480 }
    val resolutionHeight: LiveData<Int> = _resolutionHeight
    fun setResolutionHeight(value: Int) {
        if (value in 1..9600) _resolutionHeight.value = value
    }

    var info = "info placeholder"
}