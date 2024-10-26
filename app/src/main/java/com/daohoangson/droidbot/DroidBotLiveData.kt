package com.daohoangson.droidbot

import androidx.lifecycle.MutableLiveData
import java.util.Date

class DroidBotLiveData {
    companion object {
        val screenshots = MutableLiveData<Date>()
        val taps = MutableLiveData<Pair<Float, Float>>()
    }
}