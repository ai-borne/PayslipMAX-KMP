package com.payslipmax.pdfparser.part2.data

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.payslipmax.pdfparser.crypto.ContextHolder
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * WHY: the "last backup" time must survive the app being closed (so it is a real preference, not memory), must be absent before the first
 * backup, and must live in a preferences file of its own, never in the encrypted database or in a backup archive.
 */
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class AndroidLastBackupStorageTest {
    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        ContextHolder.context = context
        context.getSharedPreferences("payslipmax_part2_prefs", Context.MODE_PRIVATE).edit().clear().commit()
    }

    @After
    fun tearDown() {
        ContextHolder.context = null
    }

    @Test
    fun theTimeIsAbsentThenSavedThenReadByANewInstance() {
        assertNull(AndroidLastBackupStorage().load())

        AndroidLastBackupStorage().save(1_800_000_000_000L)

        assertEquals(1_800_000_000_000L, AndroidLastBackupStorage().load(), "a new instance, as after a restart, reads what an old one saved")
    }

    @Test
    fun itIsOneNumberInItsOwnPreferencesFile() {
        AndroidLastBackupStorage().save(42L)

        val prefs = context.getSharedPreferences("payslipmax_part2_prefs", Context.MODE_PRIVATE)
        assertEquals(mapOf("last_backup_v1" to 42L), prefs.all)
        assertTrue(provideLastBackupStorage() is AndroidLastBackupStorage)
    }
}
