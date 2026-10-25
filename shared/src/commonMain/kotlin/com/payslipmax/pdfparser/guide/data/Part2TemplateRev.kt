package com.payslipmax.pdfparser.guide.data

import com.payslipmax.pdfparser.crypto.CryptoHelper
import com.payslipmax.pdfparser.guide.model.Part2Template
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

/**
 * A template's revision, recomputed in the app exactly as `part2_templates.rev()` computes it: the first [LENGTH] hex characters of the
 * SHA-256 of the shipped template as compact JSON with its keys sorted, every non-ASCII character left as it is and nothing but quotes,
 * backslashes and control characters escaped. `rev` itself is not part of what is hashed, and an empty `rules` or `links` list is
 * written out because the tool always writes it. `GuidePart2TemplateValidator` refuses a template whose `rev` is not this, so a
 * template whose words changed without its revision changing never loads. `Part2TemplateRevTest` pins the bytes against the tool.
 */
internal object Part2TemplateRev {
    const val LENGTH = 8

    private val json = Json { encodeDefaults = false }

    fun of(template: Part2Template): String {
        val full = json.encodeToJsonElement(Part2Template.serializer(), template) as JsonObject
        val shipped = (full - "rev") + mapOf("rules" to (full["rules"] ?: JsonArray(emptyList())), "links" to (full["links"] ?: JsonArray(emptyList())))
        val text = StringBuilder().also { write(JsonObject(shipped), it) }.toString()
        return CryptoHelper.sha256(text).take(LENGTH)
    }

    private fun write(
        element: JsonElement,
        out: StringBuilder,
    ) {
        when (element) {
            is JsonObject -> {
                out.append('{')
                element.keys.sorted().forEachIndexed { i, key ->
                    if (i > 0) out.append(',')
                    quote(key, out)
                    out.append(':')
                    write(element.getValue(key), out)
                }
                out.append('}')
            }
            is JsonArray -> {
                out.append('[')
                element.forEachIndexed { i, item ->
                    if (i > 0) out.append(',')
                    write(item, out)
                }
                out.append(']')
            }
            is JsonPrimitive -> if (element.isString) quote(element.content, out) else out.append(element.content)
        }
    }

    private fun quote(
        text: String,
        out: StringBuilder,
    ) {
        out.append('"')
        for (c in text) {
            when {
                c == '"' -> out.append("\\\"")
                c == '\\' -> out.append("\\\\")
                c == '\n' -> out.append("\\n")
                c == '\r' -> out.append("\\r")
                c == '\t' -> out.append("\\t")
                c == '\b' -> out.append("\\b")
                c == '\u000C' -> out.append("\\f")
                c < ' ' -> out.append("\\u").append(c.code.toString(16).padStart(4, '0'))
                else -> out.append(c)
            }
        }
        out.append('"')
    }
}
