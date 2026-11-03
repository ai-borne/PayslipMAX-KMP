package com.payslipmax.pdfparser.repository.backup

import com.payslipmax.pdfparser.database.AppSettingsEntity
import com.payslipmax.pdfparser.part2.data.Part2Backup
import com.payslipmax.pdfparser.repository.BACKUP_PASSWORD
import com.payslipmax.pdfparser.repository.GoldenBackupCatalog
import com.payslipmax.pdfparser.repository.PayslipBackupService
import com.payslipmax.pdfparser.repository.RestoreMode
import com.payslipmax.pdfparser.testing.FakePayslipDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * WHY (D5, F8): the app lock, its PIN hash, telemetry consent and Premium belong to this device and its store account. A backup made on a
 * locked phone, restored onto a new one, must not lock the officer out with a PIN set elsewhere, switch telemetry back on after they
 * turned it off, or grant or revoke Premium. What the officer chose about the app itself (theme, profile, on-device AI) still travels.
 */
class SettingsBackupDeviceOnlyTest {
    @Test
    fun anExportCarriesNoDeviceOnlySetting() =
        runTest {
            val dao = FakePayslipDao()
            dao.insertSettings(LOCKED_PREMIUM_DEVICE.copy(appTheme = "dark"))

            val settings = exportedDocument(dao, BACKUP_PASSWORD).getValue("settings").jsonObject

            assertEquals(emptySet(), settings.keys intersect DEVICE_ONLY_KEYS)
            assertEquals("dark", settings.getValue("appTheme").jsonPrimitive.content, "the theme still travels")
        }

    @Test
    fun replacingFromThe150GoldensKeepsTheDevicesLockPinTelemetryAndPremium() =
        runTest {
            val goldens = GoldenBackupCatalog.goldens.filter { "1.5.0" in it.file }
            assertEquals(2, goldens.size, "the Android and iOS 1.5.0 goldens")
            for (golden in goldens) {
                val bytes = GoldenBackupCatalog.bytes(golden.file)
                assertCarriesEveryDeviceOnlySetting(bytes, golden.password, golden.file)
                for ((index, device) in listOf(AppSettingsEntity(appTheme = "light"), LOCKED_PREMIUM_DEVICE).withIndex()) {
                    val dao = FakePayslipDao()
                    dao.insertSettings(device)

                    PayslipBackupService(dao, Dispatchers.Unconfined, part2Backup = Part2Backup(dao.part2))
                        .restore(bytes, golden.password, RestoreMode.REPLACE)
                        .getOrThrow()

                    val after = dao.getSettings()!!
                    assertEquals(device.deviceOnly(), after.deviceOnly(), "${golden.file} onto device $index")
                    assertEquals(golden.theme, after.appTheme, "${golden.file}: the backup's theme travels")
                }
            }
        }

    @Test
    fun aReplaceRestoreBringsTheThemeAndProfile() =
        runTest {
            val source = FakePayslipDao()
            val preferences =
                AppSettingsEntity(
                    appTheme = "dark",
                    profileName = "Synthetic Officer",
                    profileCdaNumber = "CDA-0000",
                    profilePanNumber = "AAAAA0000A",
                    useLocalAi = true,
                )
            source.insertSettings(preferences.copy(isLockEnabled = true, appPinHash = "source-pin"))
            val archive = PayslipBackupService(source, Dispatchers.Unconfined).export(BACKUP_PASSWORD).getOrThrow()
            val target = FakePayslipDao()
            target.insertSettings(AppSettingsEntity(appTheme = "light", profileName = "Old", isPremiumEnabled = true))

            PayslipBackupService(target, Dispatchers.Unconfined).restore(archive, BACKUP_PASSWORD, RestoreMode.REPLACE).getOrThrow()

            assertEquals(preferences.copy(isPremiumEnabled = true), target.getSettings())
        }

    /** The precondition that gives the restore test its teeth: the golden really carries a lock, a PIN hash, telemetry off and Premium. */
    private fun assertCarriesEveryDeviceOnlySetting(
        bytes: ByteArray,
        password: String,
        file: String,
    ) {
        val settings = decryptedDocument(bytes, password).getValue("settings").jsonObject
        assertTrue(settings.getValue("isLockEnabled").jsonPrimitive.boolean, "$file: lock on")
        assertTrue(settings.getValue("appPinHash").jsonPrimitive.content.isNotEmpty(), "$file: a PIN hash")
        assertEquals(false, settings.getValue("isTelemetryEnabled").jsonPrimitive.boolean, "$file: telemetry off")
        assertTrue(settings.getValue("isPremiumEnabled").jsonPrimitive.boolean, "$file: Premium on")
    }

    private fun AppSettingsEntity.deviceOnly() = listOf(isPremiumEnabled, isLockEnabled, appPinHash, isTelemetryEnabled)
}
