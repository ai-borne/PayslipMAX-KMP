package com.ssbmax.pdfparser.insights

import io.ktor.client.network.sockets.ConnectTimeoutException
import io.ktor.client.network.sockets.SocketTimeoutException
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.plugins.ResponseException

object NetworkErrorMapper {
    fun getUserFriendlyMessage(throwable: Throwable): String {
        return when (throwable) {
            is SocketTimeoutException,
            is ConnectTimeoutException,
            is HttpRequestTimeoutException,
            -> {
                "The server is taking too long to respond. Please check your internet connection and try again."
            }
            is ResponseException -> {
                mapResponseException(throwable)
            }
            else -> {
                mapGenericException(throwable)
            }
        }
    }

    private fun mapResponseException(exception: ResponseException): String {
        val statusCode = exception.response.status.value
        return when (statusCode) {
            401, 403 -> "Unauthorized access. Please log in again."
            404 -> "The requested service was not found."
            in 500..599 -> "Our servers are experiencing issues. Please try again later."
            else -> "An error occurred on the server (Status: $statusCode). Please try again."
        }
    }

    private fun mapGenericException(throwable: Throwable): String {
        val message = throwable.message?.lowercase() ?: ""
        return when {
            message.contains("timeout") ||
                message.contains("timed out") ||
                message.contains("time out") -> {
                "The request timed out. Please try again."
            }
            message.contains("unknown host") ||
                message.contains("dns") ||
                message.contains("connect") ||
                message.contains("resolv") -> {
                "Cannot connect to the server. Please check your internet connection and try again."
            }
            else -> {
                "Unable to generate AI insights due to a connection issue. Please check your network and try again."
            }
        }
    }
}
