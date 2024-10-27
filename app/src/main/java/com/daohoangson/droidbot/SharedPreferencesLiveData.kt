package com.daohoangson.droidbot

import android.content.Context
import android.content.SharedPreferences
import androidx.activity.ComponentActivity
import androidx.lifecycle.LiveData

class SharedPreferencesLiveData(
    context: Context,
) : LiveData<SharedPreferencesLiveData.Values>() {
    companion object {
        private const val PREFS_NAME = "DroidBotPrefs"
        private const val KEY_AWS_ACCESS_KEY_ID = "awsAccessKeyId"
        private const val KEY_AWS_SECRET_ACCESS_KEY = "awsSecretAccessKey"
    }

    data class Values(
        val awsAccessKeyId: String? = null,
        val awsSecretAccessKey: String? = null,
    )

    private val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, _ ->
        getValues()
    }

    private val prefs = context.getSharedPreferences(
        PREFS_NAME, ComponentActivity.MODE_PRIVATE
    )


    override fun onActive() {
        super.onActive()
        getValues()
        prefs.registerOnSharedPreferenceChangeListener(listener)
    }

    override fun onInactive() {
        prefs.unregisterOnSharedPreferenceChangeListener(listener)
        super.onInactive()
    }

    fun apply(awsAccessKeyId: String, awsSecretAccessKey: String) {
        prefs.edit()
            .putString(KEY_AWS_ACCESS_KEY_ID, awsAccessKeyId)
            .putString(KEY_AWS_SECRET_ACCESS_KEY, awsSecretAccessKey)
            .apply()
    }

    private fun getValues() {
        value = Values(
            awsAccessKeyId = prefs.getString(KEY_AWS_ACCESS_KEY_ID, null),
            awsSecretAccessKey = prefs.getString(KEY_AWS_SECRET_ACCESS_KEY, null),
        )
    }
}
