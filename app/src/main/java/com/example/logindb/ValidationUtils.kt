package com.example.logindb

import android.text.TextUtils
import android.util.Patterns

object ValidationUtils {

    fun isValidEmail(text: String): Boolean {
        return if (TextUtils.isEmpty(text)) false
        else Patterns.EMAIL_ADDRESS.matcher(text).matches()
    }

    fun isValidUsername(text: String): Boolean {
        return !TextUtils.isEmpty(text) && text.matches(Regex("^[a-zA-Z0-9_-]{3,18}\$"))
    }

    fun isTextNotEmpty(text: String?): Boolean {
        return !TextUtils.isEmpty(text)
    }
}