package com.example.pagoflex.data.session

import android.content.Context
import android.content.SharedPreferences

object SessionManager {
    private const val PREFS = "pagoflex_session"
    private const val KEY_USER_ID = "user_id"
    private const val KEY_ROLE = "role"

    private fun prefs(context: Context): SharedPreferences =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun save(context: Context, userId: Long, role: String) {
        prefs(context).edit().putLong(KEY_USER_ID, userId).putString(KEY_ROLE, role).apply()
    }

    fun userId(context: Context): Long = prefs(context).getLong(KEY_USER_ID, -1L)
    fun role(context: Context): String? = prefs(context).getString(KEY_ROLE, null)

    fun clear(context: Context) {
        prefs(context).edit().clear().apply()
    }

    fun isLoggedIn(context: Context): Boolean = userId(context) > 0
}
