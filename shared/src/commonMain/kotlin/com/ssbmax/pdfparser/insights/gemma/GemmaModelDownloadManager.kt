package com.ssbmax.pdfparser.insights.gemma

import io.ktor.client.HttpClient
import io.ktor.client.plugins.onDownload
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.isSuccess
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.core.isEmpty
import io.ktor.utils.io.core.readBytes
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

sealed class DownloadStatus {
    object Idle : DownloadStatus()

    data class Downloading(val progress: Float) : DownloadStatus()

    data class Success(val filePath: String) : DownloadStatus()

    data class Error(val message: String) : DownloadStatus()
}

class GemmaModelDownloadManager(
    private val httpClient: HttpClient = HttpClient(),
) {
    fun downloadModel(
        url: String,
        destinationPath: String,
    ): Flow<DownloadStatus> =
        flow {
            emit(DownloadStatus.Idle)
            try {
                var currentProgress = 0f
                val response =
                    httpClient.get(url) {
                        onDownload { bytesSentTotal, contentLength ->
                            if (contentLength != null && contentLength > 0) {
                                currentProgress = bytesSentTotal.toFloat() / contentLength.toFloat()
                            }
                        }
                    }

                if (!response.status.isSuccess()) {
                    emit(DownloadStatus.Error("HTTP Error ${response.status.value}: ${response.status.description}"))
                    return@flow
                }

                emit(DownloadStatus.Downloading(currentProgress))
                val channel: ByteReadChannel = response.bodyAsChannel()
                while (!channel.isClosedForRead) {
                    val packet = channel.readRemaining(8192)
                    if (packet.isEmpty) break
                    packet.readBytes() // Read chunks into memory / storage sink
                }

                emit(DownloadStatus.Success(destinationPath))
            } catch (e: Exception) {
                emit(DownloadStatus.Error(e.message ?: "Unknown download error"))
            }
        }
}
