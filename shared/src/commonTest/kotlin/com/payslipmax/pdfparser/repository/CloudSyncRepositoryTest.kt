package com.payslipmax.pdfparser.repository

import com.payslipmax.pdfparser.database.*
import com.payslipmax.pdfparser.domain.*
import com.payslipmax.pdfparser.testing.*
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.OutgoingContent
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlin.test.*

class CloudSyncRepositoryTest {
    private lateinit var fakeDao: FakePayslipDao
    private lateinit var fakeParser: FakePdfParser
    private lateinit var payslipRepository: PayslipRepository

    @BeforeTest
    fun setUp() {
        fakeDao = FakePayslipDao()
        fakeParser = FakePdfParser()
        payslipRepository = PayslipRepository(fakeDao, fakeParser, kotlinx.coroutines.Dispatchers.Unconfined)
    }

    @Test
    fun testUploadBackupSuccess() =
        runTest {
            // Prepare database state to have something to backup
            val settings =
                AppSettingsEntity(
                    isPremiumEnabled = true,
                )
            fakeDao.insertSettings(settings)

            var requestUrl = ""
            var requestMethod = ""
            var authHeader = ""
            var contentTypeHeader = ""
            var requestBodyBytes: ByteArray? = null

            val mockEngine =
                MockEngine { request ->
                    requestUrl = request.url.toString()
                    requestMethod = request.method.value
                    authHeader = request.headers["Authorization"] ?: ""
                    contentTypeHeader = request.headers["Content-Type"] ?: request.body.contentType?.toString() ?: ""
                    val body = request.body
                    if (body is OutgoingContent.ByteArrayContent) {
                        requestBodyBytes = body.bytes()
                    }
                    respond(
                        content = "{}",
                        status = HttpStatusCode.OK,
                        headers = headersOf("Content-Type", "application/json"),
                    )
                }

            val client = HttpClient(mockEngine)
            val cloudSyncRepository = CloudSyncRepository(client, payslipRepository)

            val result =
                cloudSyncRepository.uploadBackup(
                    userId = "user123",
                    authToken = "token123",
                    password = "pwd",
                )

            assertTrue(result.isSuccess)
            assertEquals("POST", requestMethod)
            assertTrue(requestUrl.contains("users%2Fuser123%2Fbackups%2Fbackup.bin"))
            assertEquals("Bearer token123", authHeader)
            assertEquals("application/octet-stream", contentTypeHeader)
            assertNotNull(requestBodyBytes)
        }

    @Test
    fun testUploadBackupHttpFailure() =
        runTest {
            val mockEngine =
                MockEngine { _ ->
                    respond(
                        content = "Unauthorized",
                        status = HttpStatusCode.Unauthorized,
                        headers = headersOf("Content-Type", "text/plain"),
                    )
                }

            val client = HttpClient(mockEngine)
            val cloudSyncRepository = CloudSyncRepository(client, payslipRepository)

            val result =
                cloudSyncRepository.uploadBackup(
                    userId = "user123",
                    authToken = "token123",
                    password = "pwd",
                )

            assertTrue(result.isFailure)
            assertTrue(result.exceptionOrNull()?.message?.contains("Upload failed with status 401") == true)
        }

    @Test
    fun testDownloadAndRestoreBackupSuccess() =
        runTest {
            // Let's create a valid backup bytes locally first by exporting from a helper repo
            val settings =
                AppSettingsEntity(
                    isPremiumEnabled = true,
                )
            fakeDao.insertSettings(settings)
            val backupBytes = payslipRepository.exportUniversalBackup("pwd").getOrThrow()

            // Clear local DAO so we can assert it was restored
            fakeDao.clearSettings()
            assertNull(payslipRepository.getSettings())

            var requestUrl = ""
            var requestMethod = ""
            var authHeader = ""

            val mockEngine =
                MockEngine { request ->
                    requestUrl = request.url.toString()
                    requestMethod = request.method.value
                    authHeader = request.headers["Authorization"] ?: ""
                    respond(
                        content = backupBytes,
                        status = HttpStatusCode.OK,
                        headers = headersOf("Content-Type", "application/octet-stream"),
                    )
                }

            val client = HttpClient(mockEngine)
            val cloudSyncRepository = CloudSyncRepository(client, payslipRepository)

            val result =
                cloudSyncRepository.downloadAndRestoreBackup(
                    userId = "user123",
                    authToken = "token123",
                    password = "pwd",
                )

            assertTrue(result.isSuccess)
            assertEquals("GET", requestMethod)
            assertTrue(requestUrl.contains("users%2Fuser123%2Fbackups%2Fbackup.bin"))
            assertTrue(requestUrl.contains("alt=media"))
            assertEquals("Bearer token123", authHeader)

            // Verify state is restored, but entitlement never travels inside a backup (D3): the
            // device was free at restore time (settings cleared above), so it stays free.
            val restoredSettings = payslipRepository.getSettings()
            assertNotNull(restoredSettings)
            assertFalse(restoredSettings.isPremiumEnabled)
        }

    @Test
    fun testDownloadAndRestoreBackupHttpFailure() =
        runTest {
            val mockEngine =
                MockEngine { _ ->
                    respond(
                        content = "Not Found",
                        status = HttpStatusCode.NotFound,
                        headers = headersOf("Content-Type", "text/plain"),
                    )
                }

            val client = HttpClient(mockEngine)
            val cloudSyncRepository = CloudSyncRepository(client, payslipRepository)

            val result =
                cloudSyncRepository.downloadAndRestoreBackup(
                    userId = "user123",
                    authToken = "token123",
                    password = "pwd",
                )

            assertTrue(result.isFailure)
            assertTrue(result.exceptionOrNull()?.message?.contains("Download failed with status 404") == true)
        }

    @Test
    fun testDownloadAndRestoreBackupDecryptFailure() =
        runTest {
            val mockEngine =
                MockEngine { _ ->
                    // Invalid/wrong encrypted bytes
                    respond(
                        content = byteArrayOf(1, 2, 3, 4),
                        status = HttpStatusCode.OK,
                        headers = headersOf("Content-Type", "application/octet-stream"),
                    )
                }

            val client = HttpClient(mockEngine)
            val cloudSyncRepository = CloudSyncRepository(client, payslipRepository)

            val result =
                cloudSyncRepository.downloadAndRestoreBackup(
                    userId = "user123",
                    authToken = "token123",
                    password = "pwd",
                )

            // Decryption or decoding must fail
            assertTrue(result.isFailure)
        }
}
