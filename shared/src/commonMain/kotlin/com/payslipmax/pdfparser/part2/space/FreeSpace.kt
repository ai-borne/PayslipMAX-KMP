package com.payslipmax.pdfparser.part2.space

import com.payslipmax.pdfparser.part2.domain.Part2Limits

/** Free bytes on the volume that holds the vault, or null when the platform cannot say. A platform adapter; never a path, never a name. */
fun interface FreeSpaceReader {
    fun freeBytes(): Long?
}

/** What the device's free space allows before a scan or an attach. */
enum class SpaceVerdict { ENOUGH, LOW, REFUSED }

/**
 * Judges free space against `Part2Limits`: below the refusal line no scan opens, below the warning line it opens with a warning. A reader
 * that cannot say, or throws, never blocks the officer: the vault's own ceiling and the encoder's failures still guard the write.
 */
class FreeSpaceGate(
    private val reader: FreeSpaceReader,
) {
    fun verdict(): SpaceVerdict {
        val free =
            try {
                reader.freeBytes()
            } catch (e: Exception) {
                null
            } ?: return SpaceVerdict.ENOUGH
        return when {
            free < Part2Limits.FREE_SPACE_REFUSE_BYTES -> SpaceVerdict.REFUSED
            free < Part2Limits.FREE_SPACE_WARN_BYTES -> SpaceVerdict.LOW
            else -> SpaceVerdict.ENOUGH
        }
    }

    companion object {
        /** For tests and builds that do not read space: always enough. */
        val Unknown = FreeSpaceGate { null }
    }
}

/** The platform's reader of free space on the volume that holds the app's private files (the vault's volume). */
expect fun provideFreeSpaceReader(): FreeSpaceReader
