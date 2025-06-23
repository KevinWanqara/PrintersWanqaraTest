package com.example.printerswanqaratest.data

import android.content.Context
import android.content.SharedPreferences

object AppStorage {
    private const val PREFS_NAME = "wanqara_prefs"
    private const val KEY_RUC = "ruc"

    fun saveRuc(context: Context, ruc: String) {
        val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_RUC, ruc).apply()
    }

    fun getRuc(context: Context): String? {
        val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_RUC, null)
    }
}

