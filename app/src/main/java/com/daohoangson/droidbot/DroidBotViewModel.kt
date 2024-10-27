package com.daohoangson.droidbot

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import java.util.Date

object DroidBotViewModel {
    private val _screenshots = MutableLiveData<Date>()
    val screenshots: LiveData<Date> = _screenshots

    private val _taps = MutableLiveData<Pair<Float, Float>>()
    val taps: LiveData<Pair<Float, Float>> = _taps

    fun dispatchTap(x: Float, y: Float) {
        _taps.value = Pair(x, y)
    }

    fun takeScreenshot() {
        _screenshots.value = Date()
    }
}