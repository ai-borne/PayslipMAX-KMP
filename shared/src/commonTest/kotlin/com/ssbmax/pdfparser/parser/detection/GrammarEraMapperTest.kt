package com.ssbmax.pdfparser.parser.detection

import kotlin.test.Test
import kotlin.test.assertEquals

class GrammarEraMapperTest {
    @Test
    fun mapsPre2015ToLegacyStatement() {
        assertEquals(
            GrammarFamily.PCDA_LEGACY_STATEMENT,
            GrammarEraMapper.mapToFamily(StatementPeriod(month = 12, year = 2014)),
        )
    }

    @Test
    fun mapsMid2016ToEarlyDualCol() {
        assertEquals(
            GrammarFamily.PCDA_EARLY_DUAL_COL,
            GrammarEraMapper.mapToFamily(StatementPeriod(month = 5, year = 2016)),
        )
    }

    @Test
    fun mapsSep2023ToTransitional() {
        assertEquals(
            GrammarFamily.PCDA_TRANSITIONAL_7TH_CPC,
            GrammarEraMapper.mapToFamily(StatementPeriod(month = 9, year = 2023)),
        )
    }

    @Test
    fun mapsOct2023ToTransitional() {
        assertEquals(
            GrammarFamily.PCDA_TRANSITIONAL_7TH_CPC,
            GrammarEraMapper.mapToFamily(StatementPeriod(month = 10, year = 2023)),
        )
    }

    @Test
    fun mapsNov2023ToModernGrid() {
        assertEquals(
            GrammarFamily.PCDA_MODERN_GRID,
            GrammarEraMapper.mapToFamily(StatementPeriod(month = 11, year = 2023)),
        )
    }

    @Test
    fun mapsJan2024ToModernGrid() {
        assertEquals(
            GrammarFamily.PCDA_MODERN_GRID,
            GrammarEraMapper.mapToFamily(StatementPeriod(month = 1, year = 2024)),
        )
    }

    @Test
    fun mapsFeb2025ToModernGrid() {
        assertEquals(
            GrammarFamily.PCDA_MODERN_GRID,
            GrammarEraMapper.mapToFamily(StatementPeriod(month = 2, year = 2025)),
        )
    }

    @Test
    fun mapsMar2025ToExtendedGrid() {
        assertEquals(
            GrammarFamily.PCDA_EXTENDED_GRID,
            GrammarEraMapper.mapToFamily(StatementPeriod(month = 3, year = 2025)),
        )
    }

    @Test
    fun mapsFarFutureMonthToExtendedGrid() {
        assertEquals(
            GrammarFamily.PCDA_EXTENDED_GRID,
            GrammarEraMapper.mapToFamily(StatementPeriod(month = 6, year = 2031)),
        )
    }
}
