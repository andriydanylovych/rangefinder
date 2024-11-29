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


    private var _photoDirectory = MutableLiveData<String>().apply {
        value = null
    }
    val photoDirectory: LiveData<String> = _photoDirectory
    fun setPhotoDirectory(dir: String) {
        _photoDirectory.value = dir
    }

}