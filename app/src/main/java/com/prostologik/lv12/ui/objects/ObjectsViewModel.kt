package com.prostologik.lv12.ui.objects

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class ObjectsViewModel : ViewModel() {

    private val _text = MutableLiveData<String>().apply {
        value = "Identified objects"
    }
    val text: LiveData<String> = _text
}