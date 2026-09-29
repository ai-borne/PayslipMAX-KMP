package com.payslipmax.pcdao

import com.payslipmax.pcdao.reconciliation.*
import com.payslipmax.pcdao.redressal.ExportFormat
import com.payslipmax.pcdao.redressal.RedressalLetterGenerator
import com.payslipmax.pcdao.repository.PcdaoAssetProvider
import com.payslipmax.pcdao.repository.PcdaoRulesRepository
import com.payslipmax.pdfparser.domain.*
import kotlinx.coroutines.runBlocking
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class CanonicalRedressalIntegrationTest {
    private val localAssetProvider =
        object : PcdaoAssetProvider {
            override suspend fun loadAsset(fileName: String): String {
                val possiblePaths =
                    listOf(
                        "../scripts/pcdao_factory/output/$fileName",
                        "scripts/pcdao_factory/output/$fileName",
                        "../composeApp/src/commonMain/composeResources/files/pcdao/$fileName",
                        "composeApp/src/commonMain/composeResources/files/pcdao/$fileName",
                        "../../scripts/pcdao_factory/output/$fileName",
                    )
                for (path in possiblePaths) {
                    val file = File(path)
                    if (file.exists()) {
                        return file.readText()
                    }
                }
                val userDir = System.getProperty("user.dir")
                throw IllegalArgumentException("Asset not found for $fileName. Current dir: $userDir")
            }
        }

    private val repository = PcdaoRulesRepository(localAssetProvider)

    @Test
    fun testEndToEndRedressalGenerationWithCanonicalRules() =
        runBlocking {
            val canonicalRules = repository.getCanonicalRules()
            assertTrue(canonicalRules.totalRules >= 378)

            val resolver = SituationalRuleResolver()
            val reconciler = ShadowLedgerReconciler(resolver)

            val testPayslip =
                ParsedPayslip(
                    file = "sample_colonel_payslip.pdf",
                    year = 2026,
                    monthNum = 3,
                    monthName = "March",
                    dateStr = "03/2026",
                    officer = Officer(name = "Abhinav Bindra", accountNo = "04/552/789123", pan = "ABCDE1234F"),
                    earnings =
                        Earnings(
                            basicPay = 136400.0,
                            dearnessAllowance = 81840.0,
                            militaryServicePay = 15500.0,
                            transportAllowance = 7200.0,
                            transportAllowanceDa = 4320.0,
                            childrenEducationAllowance = 0.0,
                        ),
                    deductions = Deductions(dsopSubscription = 30000.0),
                    ledgerBalances = LedgerBalances(),
                    summary = PayslipSummary(245260.0, 30000.0, 215260.0),
                    taxAndSavings = null,
                )

            val context =
                ActiveSituationalContext(
                    activeTileIds = setOf(SituationalTileKeys.CEA_TWO_CHILDREN),
                    numberOfChildrenCea = 2,
                )

            val reconResult = reconciler.reconcile(testPayslip, context)
            assertTrue(reconResult.lineItems.isNotEmpty())

            val request =
                RedressalLetterGenerator.createRequestFromReconciliation(
                    payslip = testPayslip,
                    reconciliationResult = reconResult,
                    rank = "Colonel",
                    serviceNumber = "IC-58992K",
                    maskPii = false,
                    exportFormat = ExportFormat.TXT,
                )

            assertEquals("Section L-1 (Colonels & Brigadiers)", request.ledgerSection)
            assertEquals("Abhinav Bindra", request.officerName)
            assertEquals("04/552/789123", request.cdaAccountNo)

            val letter = RedressalLetterGenerator.generateLetter(request, "28 September 2026")
            assertNotNull(letter)
            assertTrue(letter.recipient.contains("Golibar Maidan, Pune - 411001"))
            assertTrue(letter.attention.contains("Section L-1 (Colonels & Brigadiers)"))
            assertTrue(letter.subject.contains("FORMAL REPRESENTATION"))
            assertTrue(letter.discrepancyTableText.contains("Children Education Allowance"))
            assertTrue(letter.fullBodyText.contains("Colonel Abhinav Bindra (IC-58992K)"))

            val draftEntity = letter.toRepresentationDraftEntity()
            assertEquals("03/2026", draftEntity.disputeMonth)
            assertEquals("PCDA(O) Official Representation", draftEntity.disputeType)
            assertEquals("PCDA_O_PUNE", draftEntity.recipient)
            assertEquals(letter.fullBodyText, draftEntity.bodyText)
        }
}
