package com.example.printerswanqaratest.data

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import com.example.printerswanqaratest.api.Setting
import com.example.printerswanqaratest.api.sales.Settings
import com.google.gson.Gson

object AppStorage {
    private const val PREFS_NAME = "wanqara_prefs"
    private const val KEY_RUC = "ruc"
    private const val AUTH_TOKEN = "auth_token"
    private const val SETTINGS = "settings"

    fun saveRuc(context: Context, ruc: String) {
        val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit { putString(KEY_RUC, ruc) }
    }

    fun getRuc(context: Context): String? {
        val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_RUC, null)
    }

    fun saveToken(context: Context, token: String) {
        val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit { putString(AUTH_TOKEN, token) }
    }
    fun getToken(context: Context): String? {
        val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(AUTH_TOKEN, null)
    }

    fun saveSettings(context: Context, settings: Setting) {
        val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit {
            putString(SETTINGS, Gson().toJson(settings))
        }
    }
    fun getSettings(context: Context): String? {
        val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(SETTINGS, null)
    }

    fun clearSession(context: Context) {
        val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit {
            remove(KEY_RUC)
            remove(AUTH_TOKEN)
        }
    }
}
