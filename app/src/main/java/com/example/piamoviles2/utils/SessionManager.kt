package com.example.piamoviles2.utils

import android.content.Context
import android.content.SharedPreferences
import com.example.piamoviles2.data.models.UsuarioResponse
import com.google.gson.Gson

class SessionManager(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(
        "sazon_toto_session", Context.MODE_PRIVATE
    )
    private val gson = Gson()

    companion object {
        private const val KEY_ACCESS_TOKEN = "access_token"
        private const val KEY_USER_DATA = "user_data"
        private const val KEY_IS_LOGGED_IN = "is_logged_in"
    }

    fun saveLoginData(token: String, usuario: UsuarioResponse) {
        prefs.edit().apply {
            putString(KEY_ACCESS_TOKEN, token)
            putString(KEY_USER_DATA, gson.toJson(usuario))
            putBoolean(KEY_IS_LOGGED_IN, true)
            apply()
        }
    }

    fun getAccessToken(): String? {
        return prefs.getString(KEY_ACCESS_TOKEN, null)
    }

    fun getCurrentUser(): UsuarioResponse? {
        val userJson = prefs.getString(KEY_USER_DATA, null)
        return if (userJson != null) {
            gson.fromJson(userJson, UsuarioResponse::class.java)
        } else null
    }

    fun isLoggedIn(): Boolean {
        return prefs.getBoolean(KEY_IS_LOGGED_IN, false) && getAccessToken() != null
    }

    fun logout() {
        prefs.edit().clear().apply()
    }
}
