package com.example.prm_02

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.prm_02.models.SettingsModel

class SettingsViewModel(): ViewModel() {

    val size = MutableLiveData<Int>()
    val color = MutableLiveData<Int>()
    val range = MutableLiveData<Int>()
    val model = SettingsModel();

    init {
        range.value = model.range
        size.value = model.size
        color.value = model.color
    }

    fun setFontSize(size: Int) {
        model.size = size
    }
    fun setFontColor(color: Int) {
        model.color = color
    }
    fun setRange(data: Int) {
        model.range = data
    }

}