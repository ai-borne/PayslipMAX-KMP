package com.payslipmax.pdfparser.ui.screens.guide.part2.vault

import com.payslipmax.pdfparser.part2.domain.Part2Limits
import com.payslipmax.pdfparser.ui.theme.Part2VaultStrings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * WHY: the vault is capped at 100 MB and a backup of a big one is slow, so Settings warns once it passes 60 MB (Round 10, 13). The line
 * must not appear for a small vault (it would nag everyone) and must appear the moment the vault is over, with the real figures.
 */
class VaultSizeWarningTest {
    private val mb = 1024L * 1024

    @Test
    fun anEmptyOrSmallVaultHasNoWarning() {
        assertNull(VaultSizeWarning.text(0))
        assertNull(VaultSizeWarning.text(30 * mb))
    }

    @Test
    fun exactlyAtTheWarningSizeIsNotYetOverIt() {
        assertNull(VaultSizeWarning.text(Part2Limits.VAULT_WARN_BYTES))
    }

    @Test
    fun oneByteOverTheWarningSizeWarnsAndRoundsTheFigureUp() {
        assertEquals(Part2VaultStrings.sizeWarning(61, 100), VaultSizeWarning.text(Part2Limits.VAULT_WARN_BYTES + 1))
    }

    @Test
    fun theWarningNamesTheUsedSizeAndTheCeiling() {
        assertEquals(Part2VaultStrings.sizeWarning(75, 100), VaultSizeWarning.text(75 * mb))
        assertEquals(Part2VaultStrings.sizeWarning(100, 100), VaultSizeWarning.text(Part2Limits.VAULT_BYTES_MAX))
    }

    @Test
    fun theWordsCarryTheTwoFigures() {
        assertEquals("Your vault of published orders is using 61 MB of the 100 MB it can hold. Delete orders you no longer need in Guide, Part II Orders.", Part2VaultStrings.sizeWarning(61, 100))
    }
}
