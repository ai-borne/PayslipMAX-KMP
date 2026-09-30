package com.payslipmax.pcdao.redressal

import com.payslipmax.pcdao.model.LedgerDifferenceItem
import com.payslipmax.pcdao.reconciliation.ShadowLedgerReconciliationResult
import com.payslipmax.pdfparser.domain.Deductions
import com.payslipmax.pdfparser.domain.Earnings
import com.payslipmax.pdfparser.domain.LedgerBalances
import com.payslipmax.pdfparser.domain.Officer
import com.payslipmax.pdfparser.domain.ParsedPayslip
import com.payslipmax.pdfparser.domain.PayslipSummary
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class RedressalLetterGeneratorArmyTest {
    private fun createDummyPayslip(
        rank: String = "Major",
        name: String = "Vikram Batra",
        accountNo: String = "01/142/987654",
        basicPay: Double = 69000.0,
    ): ParsedPayslip {
        return ParsedPayslip(
            file = "payslip.pdf",
            year = 2026,
            monthNum = 3,
            monthName = "March",
            dateStr = "03/2026",
            officer = Officer(name, accountNo, "ABCDE1234F"),
            earnings = Earnings(basicPay, 34500.0, 15500.0, 7200.0, 0.0, 0.0, 0.0, 0.0),
            deductions = Deductions(20000.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0),
            ledgerBalances = LedgerBalances(0.0, 0.0, 0.0, 0.0),
            summary = PayslipSummary(basicPay + 34500.0 + 15500.0 + 7200.0, 20000.0, basicPay),
            taxAndSavings = null,
        )
    }

    @Test
    fun testTlcDisallowanceLetterFormattingAndSectionTRouting() {
        val payslip = createDummyPayslip("Major", "Rohit Sharma", "02/334/556677")
        val request =
            RedressalLetterGenerator.createTlcDisallowanceRequest(
                payslip = payslip,
                entitledHra = 24000.0,
                rank = "Major",
                serviceNumber = "IC-65432X",
                sprCityTier = "X",
            )

        assertEquals("Section T (Transportation & Travel Claims)", request.ledgerSection)
        assertEquals(1, request.lineItems.size)
        assertEquals(24000.0, request.lineItems[0].netDue)

        val letter = RedressalLetterGenerator.generateLetter(request, "29 September 2026")
        assertTrue(letter.attention.contains("Section T (Transportation & Travel Claims)"))
        assertTrue(letter.discrepancyTableText.contains("Two-Location Concession"))
        assertTrue(letter.statutoryCitationsText.contains("Handbook Chapter 10 Para 5"))
        assertTrue(letter.statutoryCitationsText.contains("SAO 10/S/86"))
        assertTrue(letter.fullBodyText.contains("Selected Place of Residence (SPR)"))
        assertEquals(24000.0, letter.totalNetDue)
    }

    @Test
    fun testNpaOmissionLetterFormattingAndSectionMRouting() {
        val payslip = createDummyPayslip("Captain", "Dr. Priya Nair", "05/112/334455", basicPay = 121200.0)
        val request =
            RedressalLetterGenerator.createNpaOmissionRequest(
                payslip = payslip,
                entitledNpa = 24240.0,
                rank = "Captain",
                serviceNumber = "MS-19876Y",
            )

        assertEquals("Section M (Medical Corps)", request.ledgerSection)
        val letter = RedressalLetterGenerator.generateLetter(request, "29 September 2026")
        assertTrue(letter.attention.contains("Section M (Medical Corps)"))
        assertTrue(letter.discrepancyTableText.contains("Non-Practicing Allowance"))
        assertTrue(letter.statutoryCitationsText.contains("1(7)/2017/D(Pay/Services)"))
        assertTrue(letter.statutoryCitationsText.contains("20% NPA Compounding"))
        assertTrue(letter.fullBodyText.contains("compounded Basic Pay + NPA"))
        assertEquals(24240.0, letter.totalNetDue)
    }

    @Test
    fun testCfaaUnderpaymentLetterFormattingAndSectionRRouting() {
        val payslip = createDummyPayslip("Major", "Deepak Rao", "03/445/667788")
        val request =
            RedressalLetterGenerator.createCfaaUnderpaymentRequest(
                payslip = payslip,
                entitledCfaa = 13125.0,
                creditedCfaa = 10500.0,
                rank = "Major",
                serviceNumber = "IC-77889Z",
            )

        assertEquals("Section R (Regimental Officers)", request.ledgerSection)
        assertEquals(2625.0, request.lineItems[0].netDue)

        val letter = RedressalLetterGenerator.generateLetter(request, "29 September 2026")
        assertTrue(letter.attention.contains("Section R (Regimental Officers)"))
        assertTrue(letter.statutoryCitationsText.contains("8(3)/2000"))
        assertTrue(letter.statutoryCitationsText.contains("1(16)/2017"))
        assertEquals(2625.0, letter.totalNetDue)
    }

    @Test
    fun testCmfaaUnderpaymentLetterFormatting() {
        val payslip = createDummyPayslip("Major", "Deepak Rao", "03/445/667788")
        val request =
            RedressalLetterGenerator.createCfaaUnderpaymentRequest(
                payslip = payslip,
                entitledCfaa = 7875.0,
                creditedCfaa = 6300.0,
                rank = "Major",
                serviceNumber = "IC-77889Z",
                isModifiedField = true,
            )

        assertEquals("Section R (Regimental Officers)", request.ledgerSection)
        assertEquals(1575.0, request.lineItems[0].netDue)

        val letter = RedressalLetterGenerator.generateLetter(request, "29 September 2026")
        assertTrue(letter.discrepancyTableText.contains("Modified Field"))
        assertTrue(letter.statutoryCitationsText.contains("Compensatory Modified Field"))
        assertEquals(1575.0, letter.totalNetDue)
    }

    @Test
    fun testSeniorOfficerColonelSectionL1Routing() {
        val payslip = createDummyPayslip("Colonel", "Col Ajay Bakshi", "01/999/888777")
        val request =
            RedressalLetterGenerator.createCfaaUnderpaymentRequest(
                payslip = payslip,
                entitledCfaa = 13125.0,
                creditedCfaa = 0.0,
                rank = "Colonel",
                serviceNumber = "IC-44332K",
            )

        assertEquals("Section L-1 (Colonels & Brigadiers)", request.ledgerSection)
        val letter = RedressalLetterGenerator.generateLetter(request)
        assertTrue(letter.attention.contains("Section L-1 (Colonels & Brigadiers)"))
        assertTrue(letter.officerDetailsHeader.contains("Colonel Col Ajay Bakshi"))
    }

    @Test
    fun testLeaveTptaWaiverLetterFormattingAndSectionTRouting() {
        val payslip = createDummyPayslip("Captain", "Aditya Verma", "02/555/444333")
        val request =
            RedressalLetterGenerator.createLeaveTptaWaiverRequest(
                payslip = payslip,
                disputedDebitAmount = 13083.84,
                rank = "Captain",
                serviceNumber = "IC-88776M",
                dutyDaysPresent = 2,
            )

        assertEquals("Section T (Transportation & Travel Claims)", request.ledgerSection)
        val letter = RedressalLetterGenerator.generateLetter(request, "29 September 2026")
        assertTrue(letter.attention.contains("Section T (Transportation & Travel Claims)"))
        assertTrue(letter.statutoryCitationsText.contains("Travel Regulations Rule 230(B)"))
        assertTrue(letter.fullBodyText.contains("performed duty on 2 day(s)"))
        assertEquals(13083.84, letter.totalNetDue)
    }

    @Test
    fun testReconciliationAutoPopulatesArmyStatutoryCitations() {
        val payslip = createDummyPayslip("Major", "Siddharth Malhotra", "04/667/889900")
        val reconciliation =
            ShadowLedgerReconciliationResult(
                lineItems =
                    listOf(
                        LedgerDifferenceItem(
                            allowanceKey = "TLC",
                            allowanceName = "Two-Location Concession HRA",
                            creditedAmount = 0.0,
                            entitledAmount = 24000.0,
                            authorityRef = "",
                        ),
                    ),
                totalUnclaimedAnnual = 24000.0,
                summaryMessage = "1 Unclaimed Concession",
            )

        val request =
            RedressalLetterGenerator.createRequestFromReconciliation(
                payslip = payslip,
                reconciliationResult = reconciliation,
                rank = "Major",
                serviceNumber = "IC-33221L",
            )

        assertEquals("Section T (Transportation & Travel Claims)", request.ledgerSection)
        val item = request.lineItems.first()
        assertTrue(item.statutoryAuthority.contains("Handbook Chapter 10 Para 5"))
        assertTrue(item.statutoryAuthority.contains("SAO 10/S/86"))
    }

    @Test
    fun testInferRankFromPayLevelCanonicalMappings() {
        assertEquals("Lieutenant", RedressalLetterGenerator.inferRankFromPayLevel(10, 56100.0))
        assertEquals("Lieutenant", RedressalLetterGenerator.inferRankFromPayLevel(10, 57800.0))
        assertEquals("Captain", RedressalLetterGenerator.inferRankFromPayLevel(10, 61300.0))
        assertEquals("Captain", RedressalLetterGenerator.inferRankFromPayLevel(10, 0.0))
        assertEquals("Captain", RedressalLetterGenerator.inferRankFromPayLevel("10"))
        assertEquals("Captain", RedressalLetterGenerator.inferRankFromPayLevel("10B"))
        assertEquals("Major", RedressalLetterGenerator.inferRankFromPayLevel(11))
        assertEquals("Major", RedressalLetterGenerator.inferRankFromPayLevel("11", 69400.0))
        assertEquals("Lt Colonel", RedressalLetterGenerator.inferRankFromPayLevel(12))
        assertEquals("Lt Colonel", RedressalLetterGenerator.inferRankFromPayLevel("12A", 121200.0))
        assertEquals("Colonel", RedressalLetterGenerator.inferRankFromPayLevel(13))
        assertEquals("Colonel", RedressalLetterGenerator.inferRankFromPayLevel("13", 167800.0))
        assertEquals("Brigadier", RedressalLetterGenerator.inferRankFromPayLevel("13A"))
        assertEquals("Major General", RedressalLetterGenerator.inferRankFromPayLevel(14))
        assertEquals("Major General", RedressalLetterGenerator.inferRankFromPayLevel("14", 216000.0))
    }

    @Test
    fun testInferRankFromPayLevelFallbacks() {
        assertEquals("Lieutenant", RedressalLetterGenerator.inferRankFromPayLevel(null, 56100.0))
        assertEquals("Captain", RedressalLetterGenerator.inferRankFromPayLevel(null, 63100.0))
        assertEquals("Major", RedressalLetterGenerator.inferRankFromPayLevel(null, 69400.0))
        assertEquals("Lt Colonel", RedressalLetterGenerator.inferRankFromPayLevel(null, 121200.0))
        assertEquals("Colonel", RedressalLetterGenerator.inferRankFromPayLevel(null, 167800.0))
        assertEquals("Major General", RedressalLetterGenerator.inferRankFromPayLevel(null, 216000.0))
        assertEquals("Serving Officer", RedressalLetterGenerator.inferRankFromPayLevel(null, 0.0))
        assertEquals("Serving Officer", RedressalLetterGenerator.inferRankFromPayLevel("", 0.0))
        assertEquals("Serving Officer", RedressalLetterGenerator.inferRankFromPayLevel("INVALID", 0.0))
        assertEquals("Serving Officer", RedressalLetterGenerator.inferRankFromPayLevel(99, 0.0))
    }

    @Test
    fun testFormalMilitarySignOffBlockWithCdaAccount() {
        val payslip = createDummyPayslip("Major", "Vikram Batra", "01/142/987654")
        val request =
            RedressalLetterGenerator.createCfaaUnderpaymentRequest(
                payslip = payslip,
                entitledCfaa = 13125.0,
                creditedCfaa = 10500.0,
                rank = "Major",
                serviceNumber = "IC-54321A",
            )
        val letter = RedressalLetterGenerator.generateLetter(request)
        assertTrue(letter.fullBodyText.contains("Yours faithfully,\n\n(Vikram Batra)\nMajor, Indian Army\nCDA A/C: 01/142/987654"))

        val maskedRequest = request.copy(maskPii = true)
        val maskedLetter = RedressalLetterGenerator.generateLetter(maskedRequest)
        assertTrue(maskedLetter.fullBodyText.contains("Yours faithfully,\n\n(V. *******)\nMajor, Indian Army\nCDA A/C: 01/14******"))
    }

    @Test
    fun testLtColonelLedgerRoutingAndAutoInferredRank() {
        assertEquals("Section R (Regimental Officers)", RedressalLetterGenerator.resolveLedgerSection("Lt Colonel"))
        assertEquals("Section R (Regimental Officers)", RedressalLetterGenerator.resolveLedgerSection("Lieutenant Colonel"))
        assertEquals("Section T (Transportation & Travel Claims)", RedressalLetterGenerator.resolveLedgerSection("Lt Colonel", "TLC"))

        val ltColSlip = createDummyPayslip(rank = "Officer", name = "Sanjay Kumar", accountNo = "02/112/334455", basicPay = 121200.0)
        val dummyRecon =
            ShadowLedgerReconciliationResult(
                lineItems =
                    listOf(
                        LedgerDifferenceItem(
                            allowanceKey = "RH_CFAA",
                            allowanceName = "CFAA",
                            creditedAmount = 0.0,
                            entitledAmount = 10500.0,
                            authorityRef = "MoD",
                        ),
                    ),
                totalUnclaimedAnnual = 10500.0,
                summaryMessage = "1 item",
            )
        val req = RedressalLetterGenerator.createRequestFromReconciliation(ltColSlip, dummyRecon)
        assertEquals("Lt Colonel", req.rank)
        assertEquals("Section R (Regimental Officers)", req.ledgerSection)
    }
}
