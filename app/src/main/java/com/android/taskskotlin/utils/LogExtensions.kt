package com.android.taskskotlin.utils

import android.util.Log

fun <T : Any> T.logDTag(message: String) {
    Log.d(this::class.simpleName, message)
}

fun <T : Any> T.logITag(message: String) {
    Log.i(this::class.simpleName, message)
}

fun <T : Any> T.logETag(message: String) {
    Log.e(this::class.simpleName, message)
}

fun <T : Any> T.logException(exception: Exception) {
    Log.e(this::class.simpleName, "${exception::class.simpleName}: ${exception.message}")
}