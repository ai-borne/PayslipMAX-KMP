package com.payslipmax.pdfparser.smoke;

import static org.junit.Assert.assertNotNull;

import android.content.Context;
import android.content.Intent;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.uiautomator.By;
import androidx.test.uiautomator.UiDevice;
import androidx.test.uiautomator.UiObject2;
import androidx.test.uiautomator.Until;

/** What every minified-app smoke test shares: the device, launching the app, and waiting for or scrolling to text on screen. */
abstract class SmokeBase {
    protected static final String APP_ID = "in.aiborne.payslipmax";
    protected static final long LONG_MS = 15_000L;
    protected static final long SHORT_MS = 3_000L;
    protected static final long POLL_MS = 500L;
    // Google Play's own sheet, not app copy: installing with Play as installer can make Play offer the Gemma asset
    // pack ("Download additional files?") over the app. Dismissing it downloads nothing.
    protected static final String PLAY_ASSET_PACK_DISMISS = "Dismiss update dialogue";

    protected final UiDevice device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());
    protected final Context context = InstrumentationRegistry.getInstrumentation().getContext();

protected void launchApp() {
        Intent intent = context.getPackageManager().getLaunchIntentForPackage(APP_ID);
        assertNotNull("the minifiedTest app is not installed", intent);
        context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
        device.wait(Until.hasObject(By.pkg(APP_ID).depth(0)), LONG_MS);
    }

    protected void dismissIfShown(String text) {
        UiObject2 button = device.wait(Until.findObject(By.text(text)), SHORT_MS);
        if (button != null) button.click();
        device.waitForIdle();
    }

    /** Swipes the worksheet up until a node whose text contains [text] is on screen (the list only composes what is visible). */
    protected UiObject2 scrollTo(String text, String what) {
        int w = device.getDisplayWidth();
        int h = device.getDisplayHeight();
        for (int i = 0; i < 12; i++) {
            UiObject2 found = device.wait(Until.findObject(By.textContains(text)), POLL_MS);
            if (found != null) {
                device.waitForIdle();
                return found;
            }
            device.swipe(w / 2, h * 7 / 10, w / 2, h * 3 / 10, 20);
            device.waitForIdle();
        }
        assertNotNull("not shown after scrolling: " + what, null);
        return null;
    }

    /** Waits for [text], dismissing the Play asset-pack sheet whenever it appears on top (it can come late). */
    protected UiObject2 await(String text, String what) {
        long deadline = System.currentTimeMillis() + LONG_MS;
        while (System.currentTimeMillis() < deadline) {
            UiObject2 playSheet = device.findObject(By.desc(PLAY_ASSET_PACK_DISMISS));
            if (playSheet != null) playSheet.click();
            UiObject2 found = device.wait(Until.findObject(By.text(text)), POLL_MS);
            if (found != null) return found;
        }
        assertNotNull("not shown: " + what, null);
        return null;
    }
}
