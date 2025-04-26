package com.prostologik.lv12.ui.home

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class HomeViewModel : ViewModel() {

    var photoDirectory = "/storage/emulated/0/Android/media/com.prostologik.lv12/image"

    private var _photoFileName = MutableLiveData<String>().apply { value = null }
    val photoFileName: LiveData<String> = _photoFileName
    fun setPhotoFileName(fileName: String) {
        _photoFileName.value = fileName
    }

    var modelAlreadyPopulated = false

    var analyzerOption = 0

    var snippetLayer = 0 // sharedPref

    var patchWidth = 28 // sharedPref
    var patchHeight = 28 // sharedPref

    var snippetWidth = 64
    var snippetHeight = 64

    var resolutionWidth = 640 // sharedPref
    var resolutionHeight = 480 // sharedPref

    var cameraInfo = "dummy"

    var arrayOutputWidth = arrayOf<Int>(320, 640, 800)
    var arrayOutputHeight = arrayOf<Int>(240, 480, 600)
}