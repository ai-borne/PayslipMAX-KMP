package com.payslipmax.pdfparser.insights.gemma

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
    private val sinkFactory: (String) -> ModelSink = { path -> ModelFileSink(path) },
) {
    fun downloadModel(
        url: String,
        destinationPath: String,
        headers: Map<String, String> = emptyMap(),
    ): Flow<DownloadStatus> =
        flow {
            emit(DownloadStatus.Idle)
            try {
                var currentProgress = 0f
                val response =
                    httpClient.get(url) {
                        headers.forEach { (name, value) -> this.headers.append(name, value) }
                        onDownload { bytesSentTotal, contentLength ->
                            if (contentLength > 0) {
                                currentProgress = bytesSentTotal.toFloat() / contentLength.toFloat()
                            }
                        }
                    }

                if (!response.status.isSuccess()) {
                    emit(DownloadStatus.Error("HTTP Error ${response.status.value}: ${response.status.description}"))
                    return@flow
                }

                emit(DownloadStatus.Downloading(currentProgress))
                val fullPath = "${gemmaModelStorageDir()}/$destinationPath"
                val sink = sinkFactory(fullPath)
                try {
                    val channel: ByteReadChannel = response.bodyAsChannel()
                    while (!channel.isClosedForRead) {
                        val packet = channel.readRemaining(8192)
                        if (packet.isEmpty) break
                        sink.append(packet.readBytes())
                    }
                } finally {
                    sink.close()
                }

                emit(DownloadStatus.Success(fullPath))
            } catch (e: Exception) {
                emit(DownloadStatus.Error(e.message ?: "Unknown download error"))
            }
        }
}
