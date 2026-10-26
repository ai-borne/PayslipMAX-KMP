package com.payslipmax.pdfparser.reminders

import androidx.work.Configuration
import com.payslipmax.pdfparser.PayslipApplication
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * WHY (plan: a flag-off build behaves exactly as before): WorkManager's default start-up runs in every launch, creates its database and
 * registers itself, whether or not Part II exists. So its automatic initializer is removed from the manifest and the Application supplies
 * the configuration instead, which starts WorkManager the first time something asks for it: only while Part II is on, and also when the
 * system starts a reminder job in a cold process.
 */
class WorkManagerOnDemandTest {
    private val manifest = File("src/androidMain/AndroidManifest.xml").readText()

    @Test
    fun theApplicationSuppliesWorkManagersConfigurationSoItStartsOnDemand() {
        assertTrue(Configuration.Provider::class.java.isAssignableFrom(PayslipApplication::class.java))
        assertNotNull(PayslipApplication::class.java.getDeclaredMethod("getWorkManagerConfiguration"), "the property WorkManager reads")
    }

    @Test
    fun theAutomaticInitializerIsRemovedFromTheMergedManifest() {
        val removal = Regex("""<meta-data\s+android:name="androidx.work.WorkManagerInitializer"[^>]*tools:node="remove"[^>]*/>""")
        assertEquals(1, removal.findAll(manifest).count(), "exactly one removal of WorkManager's start-up entry")
        assertTrue("xmlns:tools=\"http://schemas.android.com/tools\"" in manifest)
        assertTrue("""android:name="androidx.startup.InitializationProvider"""" in manifest && "tools:node=\"merge\"" in manifest, "the shared provider is merged, not replaced")
    }
}
