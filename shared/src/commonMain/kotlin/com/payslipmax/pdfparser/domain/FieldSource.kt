package com.payslipmax.pdfparser.domain

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.CompositeDecoder
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

/**
 * Provenance sidecar for a parsed field: which tier actually produced the value carried in
 * [ParsedPayslip.fieldConfidence]. Distinguishes a value the deterministic geometry solver was
 * certain of from one Tier 6 Gemma inferred for an ambiguous/unmatched raw entry (SWOT weakness
 * in `docs/AI_INSIGHTS_PIPELINE.md` — "Gemma fallback is a black box relative to the deterministic
 * solver").
 */
@Serializable
enum class FieldSource {
    GEOMETRY,
    GEMMA_FALLBACK,
}

/**
 * `ParsedPayslip.fieldSource` read tolerantly (docs/Plan/14a P5, D16): a value a later app adds is dropped from the map, so that payslip
 * still reads, on restore and on the device after a rollback. It is written by the plain map serializer, so every known value is
 * byte-identical to before (FieldSourceWireTest). A value that is not text is still an error: the payslip is damaged. One pass over the map
 * the decoder already holds, with nothing kept but known values, so a hostile file cannot make it loop or grow.
 */
internal object FieldSourceMapSerializer : KSerializer<Map<String, FieldSource>> {
    private val written = MapSerializer(String.serializer(), FieldSource.serializer())
    private val read = MapSerializer(String.serializer(), String.serializer())

    override val descriptor: SerialDescriptor = written.descriptor

    override fun serialize(
        encoder: Encoder,
        value: Map<String, FieldSource>,
    ) = written.serialize(encoder, value)

    // getElementIndex matches the enum's serial names, the same names the writer uses.
    @OptIn(ExperimentalSerializationApi::class)
    override fun deserialize(decoder: Decoder): Map<String, FieldSource> {
        val names = FieldSource.serializer().descriptor
        return read.deserialize(decoder).mapNotNull { (field, name) ->
            val index = names.getElementIndex(name)
            if (index == CompositeDecoder.UNKNOWN_NAME) null else field to FieldSource.entries[index]
        }.toMap()
    }
}
