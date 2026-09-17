package com.payslipmax.pdfparser.ui.screens

import com.payslipmax.pdfparser.domain.Officer
import kotlin.test.Test
import kotlin.test.assertEquals

class ReplicaOfficerDetailsTest {
    private val parsedOfficer =
        Officer(
            name = "Lt Col Amit Sharma",
            accountNo = "123456/A",
            pan = "ABCDE1234F",
        )

    @Test
    fun testResolveOfficerDetailsWhenOverridesEmpty() {
        val details =
            resolveOfficerDisplayDetails(
                parsedOfficer = parsedOfficer,
                overrideName = "",
                overrideCda = "",
                overridePan = "",
            )

        assertEquals("Lt Col Amit Sharma", details.name)
        assertEquals("123456/A", details.cda)
        assertEquals("ABCDE1234F", details.pan)
    }

    @Test
    fun testResolveOfficerDetailsWhenOverridesPopulated() {
        val details =
            resolveOfficerDisplayDetails(
                parsedOfficer = parsedOfficer,
                overrideName = "Col Amit Sharma",
                overrideCda = "654321/B",
                overridePan = "XYZAB5678C",
            )

        assertEquals("Col Amit Sharma", details.name)
        assertEquals("654321/B", details.cda)
        assertEquals("XYZAB5678C", details.pan)
    }

    @Test
    fun testResolveOfficerDetailsPartialOverrides() {
        val nameOnly =
            resolveOfficerDisplayDetails(
                parsedOfficer = parsedOfficer,
                overrideName = "Col Amit Sharma",
                overrideCda = "",
                overridePan = "",
            )
        assertEquals("Col Amit Sharma", nameOnly.name)
        assertEquals("123456/A", nameOnly.cda)
        assertEquals("ABCDE1234F", nameOnly.pan)

        val cdaOnly =
            resolveOfficerDisplayDetails(
                parsedOfficer = parsedOfficer,
                overrideName = "",
                overrideCda = "654321/B",
                overridePan = "",
            )
        assertEquals("Lt Col Amit Sharma", cdaOnly.name)
        assertEquals("654321/B", cdaOnly.cda)
        assertEquals("ABCDE1234F", cdaOnly.pan)

        val panOnly =
            resolveOfficerDisplayDetails(
                parsedOfficer = parsedOfficer,
                overrideName = "",
                overrideCda = "",
                overridePan = "XYZAB5678C",
            )
        assertEquals("Lt Col Amit Sharma", panOnly.name)
        assertEquals("123456/A", panOnly.cda)
        assertEquals("XYZAB5678C", panOnly.pan)
    }

    @Test
    fun testResolveOfficerDetailsWhenOverridesBlankWhitespaceOnly() {
        val details =
            resolveOfficerDisplayDetails(
                parsedOfficer = parsedOfficer,
                overrideName = "   ",
                overrideCda = "\t",
                overridePan = " \n ",
            )

        assertEquals("Lt Col Amit Sharma", details.name)
        assertEquals("123456/A", details.cda)
        assertEquals("ABCDE1234F", details.pan)
    }
}
