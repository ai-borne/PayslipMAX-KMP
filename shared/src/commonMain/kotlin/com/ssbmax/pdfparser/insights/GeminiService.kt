package com.ssbmax.pdfparser.insights

import com.ssbmax.pdfparser.domain.Officer
import com.ssbmax.pdfparser.domain.ParsedPayslip
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.request.url
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class GeminiRequest(
    val contents: List<Content>
)

@Serializable
data class Content(
    val parts: List<Part>
)

@Serializable
data class Part(
    val text: String
)

@Serializable
data class GeminiResponse(
    val candidates: List<Candidate>? = null
)

@Serializable
data class Candidate(
    val content: ResponseContent? = null
)

@Serializable
data class ResponseContent(
    val parts: List<Part>? = null
)

class GeminiService {
    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = true
        isLenient = true
    }

    private val client = HttpClient {
        install(ContentNegotiation) {
            json(json)
        }
    }

    suspend fun getFinancialInsights(
        payslip: ParsedPayslip,
        apiKey: String
    ): Result<String> {
        if (apiKey.isBlank()) {
            return Result.failure(IllegalArgumentException("Gemini API Key is empty. Please configure it in Settings."))
        }

        return try {
            // 1. Redact PII (Officer Name, PAN, CDA Account No, Filename)
            val redactedPayslip = payslip.redactPii()
            val payslipJson = json.encodeToString(ParsedPayslip.serializer(), redactedPayslip)

            // 2. Prepare the prompt for Gemini
            val prompt = """
                You are a world-class chartered accountant and financial advisor specialized in Indian Defence Services pay structures and tax rules.
                Analyze the following monthly payslip data and provide 3-4 highly personalized tax saving, investment, and financial planning tips.
                Ensure all advice:
                1. References specific numbers/anomalies/deductions from the data (e.g. basic pay, DSOP, AGIF, electricity, tax).
                2. Mentions GPF/DSOP ₹5 Lakh annual tax-free contribution limits if relevant.
                3. Adheres strictly to Indian Income Tax Act guidelines (e.g., standard deduction, Section 80C, new vs old tax regime benefits).
                
                Keep the tone professional, direct, and encouraging. Return your response in clear markdown format.
                
                Monthly Payslip Data:
                $payslipJson
            """.trimIndent()

            // 3. Make HTTP request to Gemini API
            val response: GeminiResponse = client.post {
                url("https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=$apiKey")
                contentType(ContentType.Application.Json)
                setBody(
                    GeminiRequest(
                        contents = listOf(
                            Content(
                                parts = listOf(
                                    Part(text = prompt)
                                )
                            )
                        )
                    )
                )
            }.body()

            val text = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
            if (text != null) {
                Result.success(text)
            } else {
                Result.failure(Exception("Failed to parse Gemini response or response was blocked/empty."))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun ParsedPayslip.redactPii(): ParsedPayslip {
        return this.copy(
            officer = Officer(
                name = "[REDACTED]",
                accountNo = "[REDACTED]",
                pan = "[REDACTED]"
            ),
            file = "[REDACTED].pdf"
        )
    }
}
