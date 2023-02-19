package com.example.moaiplanner.util

interface ErrorHandler {
    fun enable(enable: Boolean)
    fun reportException(t: Throwable, message: String? = null)
}
