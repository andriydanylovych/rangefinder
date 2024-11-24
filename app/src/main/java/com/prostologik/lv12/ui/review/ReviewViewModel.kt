package com.prostologik.lv12.ui.review

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class ReviewViewModel : ViewModel() {

    private val _text = MutableLiveData<String>().apply {
        value = "Range = 2'345m"
    }
    val text: LiveData<String> = _text
}