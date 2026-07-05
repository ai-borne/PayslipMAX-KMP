package com.payslipmax.pdfparser.logging

import com.payslipmax.pdfparser.subscription.isDebugBuild

object Logger {
    fun d(
        tag: String,
        message: String,
    ) {
        if (isDebugBuild()) {
            println("[DEBUG] [$tag] $message")
        }
    }

    fun w(
        tag: String,
        message: String,
    ) {
        println("[WARN] [$tag] $message")
    }

    fun e(
        tag: String,
        message: String,
        throwable: Throwable? = null,
    ) {
        val errorMsg = throwable?.let { " - ${it.message}" } ?: ""
        println("[ERROR] [$tag] $message$errorMsg")
    }
}
