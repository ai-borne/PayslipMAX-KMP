package com.payslipmax.pdfparser.smoke;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

import android.os.Bundle;
import android.os.SystemClock;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.uiautomator.By;
import androidx.test.uiautomator.UiObject2;
import androidx.test.uiautomator.Until;
import java.util.List;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Plan 14a, P9: green debug tests do not prove the R8 build can read or write a backup (the DTOs, their serializers, the crypto and the
 * sheet all have to survive). This drives the installed {@code minifiedTest} app like an officer: open Backup &amp; Restore in Settings,
 * Restore a committed v5 golden through the system file picker (a wrong password first, then the right one), check the preview counts
 * against the manifest, and Cancel; then Back Up what the device holds and see the counts-only result.
 *
 * <p>It never commits a restore, so no synthetic row reaches the device's real data, and it prints and asserts only fixed copy and counts.
 * {@code scripts/run_guide_minified_smoke.sh} pushes the golden as {@value #GOLDEN_ON_DEVICE}, passes its counts from the manifest as
 * instrumentation arguments, and deletes the file afterwards. Back Up needs a Premium device (it is gated); a locked device fails here
 * with that said, since a skipped leg would prove nothing.
 */
@RunWith(AndroidJUnit4.class)
public class BackupMinifiedSmokeTest extends SmokeBase {
    private static final String GOLDEN_ON_DEVICE = "p9smoke-v5.pcda";
    // The committed goldens' password (golden-backups/manifest.json): synthetic, public in the repo.
    private static final String PASSWORD = "BackupPass#1";
    private static final String WRONG_PASSWORD = "NotThePassword#1";
    private static final long BACKUP_MS = 60_000L;

    @Test
    public void theMinifiedAppPreviewsAV5GoldenAndTurnsAWrongPasswordIntoAFixedLine() {
        openBackupSheet();

        setPassword(WRONG_PASSWORD);
        pickGolden();
        await(SmokeStrings.wrongPasswordOrNotABackup, "the fixed wrong-password line (a v5 file could not be opened on the minified build)");

        setPassword(PASSWORD);
        pickGolden();
        String holds = holdsLine();
        await(holds, "the preview of the golden: " + holds);
        await(SmokeStrings.cancelBtn, "Cancel on the preview").click();
        assertFalse("the preview is still open after Cancel", device.wait(Until.hasObject(By.text(holds)), SHORT_MS));
    }

    @Test
    public void theMinifiedAppBacksUpAndSaysHowMany() {
        openBackupSheet();

        setPassword(PASSWORD);
        List<UiObject2> fields = device.findObjects(By.clazz("android.widget.EditText"));
        assertNotNull("the confirm field is not shown", fields.size() > 1 ? fields.get(1) : null);
        fields.get(1).setText(PASSWORD);
        await(SmokeStrings.settingsBackupSaveBtn, "Back Up").click();

        // The system share sheet opens over the app when the file is ready; closing it (Back) shares nothing.
        long deadline = System.currentTimeMillis() + BACKUP_MS;
        while (System.currentTimeMillis() < deadline) {
            if (device.hasObject(By.textContains(SmokeStrings.labelPayslipsBackedUp))) return;
            if (!APP_ID.equals(device.getCurrentPackageName())) device.pressBack();
            device.waitForIdle();
        }
        assertNotNull("Back Up did not end with the counts-only result in " + BACKUP_MS / 1000 + " s (is the device Premium? Back Up is gated)", null);
    }

    private void openBackupSheet() {
        launchApp();
        dismissIfShown(SmokeStrings.onboardingSkip);
        dismissIfShown(SmokeStrings.onboardingCoachmarkDismiss);
        await(SmokeStrings.navigationSettings, "the Settings tab (is the app locked?)").click();
        scrollTo(SmokeStrings.settingsRowBackupLabel, "the Backup & Restore row");
        // The row can be found while it is still under the bottom bar: scroll a little further, then tap it where it has settled.
        device.swipe(device.getDisplayWidth() / 2, device.getDisplayHeight() * 7 / 10, device.getDisplayWidth() / 2, device.getDisplayHeight() * 5 / 10, 20);
        device.waitForIdle();
        SystemClock.sleep(1_000L); // Compose's own fling is invisible to UiAutomator's idle check
        await(SmokeStrings.settingsRowBackupLabel, "the Backup & Restore row after scrolling").click();
        await(SmokeStrings.settingsBackupSaveBtn, "the Backup & Restore sheet");
    }

    /** Replaces what the Backup Password field holds (the first text field on the sheet). */
    private void setPassword(String password) {
        UiObject2 field = device.wait(Until.findObject(By.clazz("android.widget.EditText")), LONG_MS);
        assertNotNull("the password field is not shown", field);
        field.setText(password);
        device.waitForIdle();
    }

    /** Taps Restore and chooses the golden in the system file picker, which opens on the Downloads folder or the last one used. */
    private void pickGolden() {
        await(SmokeStrings.restoreBtn, "Restore").click();
        UiObject2 file = device.wait(Until.findObject(By.text(GOLDEN_ON_DEVICE)), LONG_MS);
        assertNotNull("the file picker does not show " + GOLDEN_ON_DEVICE + " (did the script push it to Downloads?)", file);
        file.click();
    }

    /** The preview line for the golden's counts, which the script reads from the manifest. */
    private static String holdsLine() {
        Bundle args = InstrumentationRegistry.getArguments();
        String[] counts = {args.getString("payslips"), args.getString("entries"), args.getString("orders"), args.getString("pages")};
        for (String c : counts) assertNotNull("the script passes the golden's counts (payslips, entries, orders, pages)", c);
        return "This backup holds " + counts[0] + (counts[0].equals("1") ? " payslip, " : " payslips, ")
                + counts[1] + (counts[1].equals("1") ? " Orders Bank entry, " : " Orders Bank entries, ")
                + counts[2] + (counts[2].equals("1") ? " order, " : " orders, ")
                + counts[3] + (counts[3].equals("1") ? " page." : " pages.");
    }
}
