package com.daohoangson.droidbot

import androidx.lifecycle.MutableLiveData

class DroidBotLiveData {
    companion object {
        val taps = MutableLiveData<Pair<Float, Float>>()
    }
}