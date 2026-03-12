package com.printerswanqara.data

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import com.printerswanqara.api.Setting
import com.printerswanqara.api.UserDetails
import com.google.gson.Gson

object AppStorage {
    private const val PREFS_NAME = "wanqara_prefs"
    private const val KEY_RUC = "ruc"
    private const val AUTH_TOKEN = "auth_token"
    private const val SETTINGS = "settings"
    private const val USER_DATA = "user_data"
    private const val ONBOARDING_COMPLETED = "onboarding_completed"

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
        println("Saving settings: $settings") // Debug log to check settings value
        val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit {
            putString(SETTINGS, Gson().toJson(settings))
        }
    }
    fun getSettings(context: Context): String? {
        val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(SETTINGS, null)
    }

    fun saveUserData(context: Context, userData: UserDetails) {

        val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit {
            putString(USER_DATA, Gson().toJson(userData))
        }
    }


    fun getUserData(context: Context): UserDetails? {
        val prefs: SharedPreferences =
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val userDataJson = prefs.getString(USER_DATA, null)
        return if (userDataJson != null) {
            Gson().fromJson(userDataJson, UserDetails::class.java)
        } else {
            null
        }
    }

    fun clearSession(context: Context) {
        val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit {
            remove(KEY_RUC)
            remove(AUTH_TOKEN)
            remove(SETTINGS)
            remove(USER_DATA)
        }
        val intent = android.content.Intent(context, com.printerswanqara.MainActivity::class.java)
        intent.flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK
        context.startActivity(intent)
    }

    fun handleSessionExpiry(context: Context) {
        android.os.Handler(android.os.Looper.getMainLooper()).post {
            android.widget.Toast.makeText(context, "Sesión inválida, por favor reingrese", android.widget.Toast.LENGTH_LONG).show()
        }
        clearSession(context)
    }

    fun isOnboardingCompleted(context: Context): Boolean {
        val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(ONBOARDING_COMPLETED, false)
    }

    fun setOnboardingCompleted(context: Context, completed: Boolean) {
        val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit { putBoolean(ONBOARDING_COMPLETED, completed) }
    }
}
