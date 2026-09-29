package com.payslipmax.pcdao.redressal

import com.payslipmax.pcdao.model.LedgerDifferenceItem
import com.payslipmax.pcdao.reconciliation.ShadowLedgerReconciliationResult
import com.payslipmax.pdfparser.domain.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class RedressalLetterGeneratorTest {
    @Test
    fun testOfficialPuneAddressAndSubjectLine() {
        val request =
            RedressalRequest(
                officerName = "Vikram Batra",
                serviceNumber = "IC-54321A",
                rank = "Captain",
                cdaAccountNo = "12/345/678901",
                disputeMonth = "March 2026",
                lineItems =
                    listOf(
                        DiffLineItem(
                            serialNo = 1,
                            lineItemName = "High Altitude Allowance (Cat III)",
                            entitledAmount = 31250.0,
                            creditedAmount = 0.0,
                            netDue = 31250.0,
                            statutoryAuthority = "MoD Letter No. 1(16)/2017/D(Pay/Services) dated 18 Sep 2017",
                            ruleId = "RH_HA_CAT_III",
                        ),
                    ),
            )

        val letter = RedressalLetterGenerator.generateLetter(request, "28 September 2026")

        assertTrue(letter.recipient.contains("Principal Controller of Defence Accounts (Officers)"))
        assertTrue(letter.recipient.contains("Golibar Maidan, Pune - 411001 (Maharashtra)"))
        assertTrue(letter.attention.contains("Section R (Regimental Officers)"))
        assertEquals("FORMAL REPRESENTATION REGARDING DISCREPANCY IN RUNNING LEDGER ACCOUNT (IRLA)", letter.subject)
        assertTrue(letter.fullBodyText.contains("To,"))
        assertTrue(letter.fullBodyText.contains("Golibar Maidan, Pune - 411001"))
        assertEquals(31250.0, letter.totalNetDue)
    }

    @Test
    fun testOfficialTitleWithoutEnumLeaking() {
        val request =
            RedressalRequest(
                officerName = "Capt Vikram Batra",
                serviceNumber = "IC-12345K",
                rank = "Captain",
                cdaAccountNo = "12/345/678901",
                disputeMonth = "March 2026",
            )
        val letter = RedressalLetterGenerator.generateLetter(request)
        assertEquals(DEFAULT_REDRESSAL_TITLE, letter.title)
        assertEquals("PCDA(O) Official Representation", letter.title)
        assertFalse(letter.title.contains("PCDAO_AUDIT_DISCREPANCY"))

        val draftEntity = letter.toRepresentationDraftEntity()
        assertEquals("PCDA(O) Official Representation", draftEntity.disputeType)
        assertFalse(draftEntity.disputeType.contains("PCDAO_AUDIT_DISCREPANCY"))
    }

    @Test
    fun testPiiMaskingModeProtectsSensitiveData() {
        val request =
            RedressalRequest(
                officerName = "Rajeshwar Singh Rathore",
                serviceNumber = "IC-72345K",
                rank = "Colonel",
                cdaAccountNo = "04/551/123456",
                ledgerSection = "Section L-1 (Colonels & Brigadiers)",
                disputeMonth = "May 2026",
                lineItems =
                    listOf(
                        DiffLineItem(
                            serialNo = 1,
                            lineItemName = "Dress Allowance",
                            entitledAmount = 25000.0,
                            creditedAmount = 20000.0,
                            netDue = 5000.0,
                            statutoryAuthority = "MoD Letter No. 19051/1/2017-E.IV dated 02.08.2017",
                        ),
                    ),
                maskPii = true,
                pan = "ABCDE1234F",
            )

        val letter = RedressalLetterGenerator.generateLetter(request)

        assertTrue(letter.isPiiMasked)
        assertTrue(letter.fullBodyText.contains("R. *******"))
        assertFalse(letter.fullBodyText.contains("Rajeshwar Singh Rathore"))
        assertTrue(letter.fullBodyText.contains("IC-72***"))
        assertFalse(letter.fullBodyText.contains("IC-72345K"))
        assertTrue(letter.fullBodyText.contains("04/55******"))
        assertFalse(letter.fullBodyText.contains("123456"))

        val maskedPan = RedressalLetterGenerator.maskPan("ABCDE1234F")
        assertEquals("ABCDE****F", maskedPan)
    }

    @Test
    fun testLedgerSectionResolution() {
        assertEquals("Section L-1 (Colonels & Brigadiers)", RedressalLetterGenerator.resolveLedgerSection("Colonel"))
        assertEquals("Section L-1 (Colonels & Brigadiers)", RedressalLetterGenerator.resolveLedgerSection("Brigadier"))
        assertEquals("Section L-1 (Colonels & Brigadiers)", RedressalLetterGenerator.resolveLedgerSection("Major General"))
        assertEquals("Section L-1 (Colonels & Brigadiers)", RedressalLetterGenerator.resolveLedgerSection("Lieutenant General"))
        assertEquals("Section M (Medical Corps)", RedressalLetterGenerator.resolveLedgerSection("AMC Officer"))
        assertEquals("Section M (Medical Corps)", RedressalLetterGenerator.resolveLedgerSection("MNS Captain"))
        assertEquals("Section R (Regimental Officers)", RedressalLetterGenerator.resolveLedgerSection("Major"))
        assertEquals("Section R (Regimental Officers)", RedressalLetterGenerator.resolveLedgerSection("Captain"))
        assertEquals("Section R (Regimental Officers)", RedressalLetterGenerator.resolveLedgerSection("Lieutenant"))
    }

    @Test
    fun testConversionToRepresentationDraftEntity() {
        val request =
            RedressalRequest(
                officerName = "Sunil Pawar",
                serviceNumber = "IC-45678P",
                rank = "Major",
                cdaAccountNo = "12/345/678901",
                disputeMonth = "March 2026",
                lineItems =
                    listOf(
                        DiffLineItem(
                            serialNo = 1,
                            lineItemName = "Children Education Allowance (CEA)",
                            entitledAmount = 33750.0,
                            creditedAmount = 27000.0,
                            netDue = 6750.0,
                            statutoryAuthority = "DoPT OM dated 16/17 July 2018",
                        ),
                    ),
            )

        val letter = RedressalLetterGenerator.generateLetter(request)
        val entity = letter.toRepresentationDraftEntity()

        assertEquals("March 2026", entity.disputeMonth)
        assertEquals(DEFAULT_REDRESSAL_TITLE, entity.disputeType)
        assertEquals("PCDA(O) Official Representation", entity.disputeType)
        assertFalse(entity.disputeType.contains("PCDAO_AUDIT_DISCREPANCY"))
        assertEquals("PCDA_O_PUNE", entity.recipient)
        assertEquals(letter.subject, entity.subject)
        assertEquals(letter.fullBodyText, entity.bodyText)
        assertTrue(entity.createdAt > 0L)
    }

    @Test
    fun testCreateRequestFromReconciliation() {
        val dummyPayslip =
            ParsedPayslip(
                file = "test.pdf",
                year = 2026,
                monthNum = 3,
                monthName = "March",
                dateStr = "03/2026",
                officer = Officer(name = "Karan Thapar", accountNo = "01/223/445566", pan = "XYZAB9988C"),
                earnings = Earnings(basicPay = 121200.0, transportAllowance = 7200.0),
                deductions = Deductions(),
                ledgerBalances = LedgerBalances(),
                summary = PayslipSummary(128400.0, 20000.0, 108400.0),
                taxAndSavings = null,
            )

        val reconciliation =
            ShadowLedgerReconciliationResult(
                lineItems =
                    listOf(
                        LedgerDifferenceItem(
                            allowanceKey = "CEA",
                            allowanceName = "Children Education Allowance (CEA)",
                            creditedAmount = 0.0,
                            entitledAmount = 67500.0,
                            authorityRef = "DoPT OM dated 16/17 July 2018",
                        ),
                    ),
                totalUnclaimedAnnual = 67500.0,
                summaryMessage = "1 Unclaimed Allowance",
            )

        val request =
            RedressalLetterGenerator.createRequestFromReconciliation(
                payslip = dummyPayslip,
                reconciliationResult = reconciliation,
                rank = "Colonel",
                serviceNumber = "IC-11223L",
            )

        assertEquals("Karan Thapar", request.officerName)
        assertEquals("01/223/445566", request.cdaAccountNo)
        assertEquals("Section L-1 (Colonels & Brigadiers)", request.ledgerSection)
        assertEquals(1, request.lineItems.size)
        assertEquals(67500.0, request.lineItems.first().netDue)

        val letter = RedressalLetterGenerator.generateLetter(request)
        assertTrue(letter.fullBodyText.contains("Colonel Karan Thapar (IC-11223L)"))
        assertTrue(letter.fullBodyText.contains("TOTAL STATUTORY NET DUE: Rs. 67500"))
    }

    @Test
    fun testEdgeCasesGracefulHandling() {
        val emptyRequest =
            RedressalRequest(
                officerName = "",
                serviceNumber = "A",
                rank = "Captain",
                cdaAccountNo = "12",
                disputeMonth = "",
                maskPii = true,
            )
        val letter = RedressalLetterGenerator.generateLetter(emptyRequest)
        assertNotNull(letter)
        assertTrue(letter.discrepancyTableText.contains("No discrepancies detected"))
        assertEquals(0.0, letter.totalNetDue)

        assertEquals("****", RedressalLetterGenerator.maskName(""))
        assertEquals("****", RedressalLetterGenerator.maskServiceNumber("123"))
        assertEquals("******", RedressalLetterGenerator.maskCdaAccount("123"))
        assertEquals("******", RedressalLetterGenerator.maskPan("ABC"))
        assertEquals(null, RedressalLetterGenerator.maskPan(null))
    }
}
