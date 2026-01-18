package com.example.fitnesstracker

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONObject

class SessionManager(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("FitnessTrackerPrefs", Context.MODE_PRIVATE)

    companion object {
        const val USER_ID = "user_id"
        const val FNAME = "fname"
        const val LNAME = "lname"
        const val EMAIL = "email"
        const val USERNAME = "username"
        const val WEIGHT = "weight"
        const val HEIGHT = "height"
        const val AGE = "age"
        const val GENDER = "gender"
    }

    fun saveUser(user: JSONObject) {
        val editor = prefs.edit()
        editor.putInt(USER_ID, user.optInt("id", getUserId())) // Persist user id if already exists

        // Handle inconsistent API keys for names
        val fname = user.optString("firstname", user.optString("fname"))
        val lname = user.optString("lastname", user.optString("lname"))

        editor.putString(FNAME, fname)
        editor.putString(LNAME, lname)
        editor.putString(EMAIL, user.optString("email"))
        editor.putString(USERNAME, user.optString("username"))
        editor.putFloat(WEIGHT, user.optDouble("weight", 0.0).toFloat())
        editor.putFloat(HEIGHT, user.optDouble("height", 0.0).toFloat())
        editor.putInt(AGE, user.optInt("age", 0))
        editor.putString(GENDER, user.optString("gender"))
        editor.apply()
    }

    fun getFirstName(): String? {
        return prefs.getString(FNAME, "")
    }

    fun getLastName(): String? {
        return prefs.getString(LNAME, "")
    }

    fun getUserId(): Int {
        return prefs.getInt(USER_ID, -1)
    }

    fun getUserDetails(): Map<String, *> {
        return prefs.all
    }

    fun clearSession() {
        prefs.edit().clear().apply()
    }

    // Added to be called from MainActivity to log the user out.
    fun logoutUser() {
        clearSession()
    }

    fun updateUser(
        fname: String,
        lname: String,
        email: String,
        username: String,
        weight: Float,
        height: Float,
        age: Int,
        gender: String
    ) {
        val editor = prefs.edit()
        editor.putString(FNAME, fname)
        editor.putString(LNAME, lname)
        editor.putString(EMAIL, email)
        editor.putString(USERNAME, username)
        editor.putFloat(WEIGHT, weight)
        editor.putFloat(HEIGHT, height)
        editor.putInt(AGE, age)
        editor.putString(GENDER, gender)
        editor.apply()
    }

}
