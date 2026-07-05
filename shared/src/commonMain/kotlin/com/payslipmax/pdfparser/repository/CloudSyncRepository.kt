package com.payslipmax.pdfparser.repository

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.headers
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.http.isSuccess

class CloudSyncRepository(
    private val httpClient: HttpClient,
    private val payslipRepository: PayslipRepository,
) {
    suspend fun uploadBackup(
        userId: String,
        authToken: String,
        password: String,
        bucket: String = "payslipmax.appspot.com",
    ): Result<Unit> {
        return try {
            val backupResult = payslipRepository.exportUniversalBackup(password)
            if (backupResult.isFailure) {
                return Result.failure(backupResult.exceptionOrNull() ?: Exception("Export failed"))
            }
            val encryptedBytes = backupResult.getOrThrow()

            val encodedName = "users/$userId/backups/backup.bin".replace("/", "%2F")
            val url = "https://firebasestorage.googleapis.com/v0/b/$bucket/o?name=$encodedName"

            val response =
                httpClient.post(url) {
                    headers {
                        append(HttpHeaders.Authorization, "Bearer $authToken")
                    }
                    contentType(ContentType.Application.OctetStream)
                    setBody(encryptedBytes)
                }

            if (response.status.isSuccess()) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Upload failed with status ${response.status}: ${response.bodyAsText()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun downloadAndRestoreBackup(
        userId: String,
        authToken: String,
        password: String,
        bucket: String = "payslipmax.appspot.com",
    ): Result<Unit> {
        return try {
            val encodedName = "users/$userId/backups/backup.bin".replace("/", "%2F")
            val url = "https://firebasestorage.googleapis.com/v0/b/$bucket/o/$encodedName?alt=media"

            val response =
                httpClient.get(url) {
                    headers {
                        append(HttpHeaders.Authorization, "Bearer $authToken")
                    }
                }

            if (response.status.isSuccess()) {
                val encryptedBytes = response.body<ByteArray>()
                val restoreResult = payslipRepository.importUniversalBackup(encryptedBytes, password)
                restoreResult
            } else {
                Result.failure(Exception("Download failed with status ${response.status}: ${response.bodyAsText()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
