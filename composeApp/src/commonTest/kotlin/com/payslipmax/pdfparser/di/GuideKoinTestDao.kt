package com.payslipmax.pdfparser.di

import com.payslipmax.pdfparser.testing.FakePayslipDao

/**
 * The one DAO every test that builds the Guide's notes repository through Koin must use. The `single` definitions inside the
 * shared `guideModule` value are reused by every Koin application that loads it in one JVM, so the first test to resolve
 * the notes repository decides which DAO it is bound to for all later ones; with one DAO the order no longer matters.
 */
object GuideKoinTestDao {
    val dao = FakePayslipDao()
}
