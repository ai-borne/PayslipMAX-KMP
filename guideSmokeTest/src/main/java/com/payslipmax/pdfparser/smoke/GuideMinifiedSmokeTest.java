package com.payslipmax.pdfparser.smoke;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

import android.content.Context;
import android.content.Intent;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.uiautomator.By;
import androidx.test.uiautomator.UiDevice;
import androidx.test.uiautomator.UiObject2;
import androidx.test.uiautomator.Until;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * R8 Check 2 (docs/Plan/rule_cards/16_guide_phase_plan.md): green debug tests do not prove the minified app can
 * read the Guide. This drives the installed {@code minifiedTest} app (release R8 config) like a user: open the
 * Guide tab, an area, a case, then its first card. A model or serializer R8 removed shows the load error instead and
 * fails here.
 *
 * <p>Black-box on purpose: it finds text on screen and reads the bundle from the app's own assets, so nothing pins
 * an app class against R8. The UI strings come from the app's string files through the generated {@code SmokeStrings}.
 */
@RunWith(AndroidJUnit4.class)
public class GuideMinifiedSmokeTest {
    private static final String APP_ID = "in.aiborne.payslipmax";
    private static final String BUNDLE_ASSET =
            "composeResources/pdfparser.composeapp.generated.resources/files/guide/guide_bundle.json";
    private static final long LONG_MS = 15_000L;
    private static final long SHORT_MS = 3_000L;
    private static final long POLL_MS = 500L;
    // Google Play's own sheet, not app copy: installing with Play as installer can make Play offer the Gemma asset
    // pack ("Download additional files?") over the app. Dismissing it downloads nothing.
    private static final String PLAY_ASSET_PACK_DISMISS = "Dismiss update dialogue";

    private final UiDevice device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());
    private final Context context = InstrumentationRegistry.getInstrumentation().getContext();

    @Test
    public void theMinifiedAppOpensGuideHomeAnAreaACaseAndACard() throws Exception {
        JSONObject bundle = new JSONObject(readBundle());
        JSONObject firstArea = bundle.getJSONArray("nav").getJSONObject(0);
        String areaTitle = firstArea.getString("title");
        JSONObject firstCase = firstArea.getJSONArray("cases").getJSONObject(0);
        String caseTitle = firstCase.getString("title");
        String cardTitle = cardTitle(bundle, firstCase.getJSONArray("cards").getString(0));

        launchApp();
        dismissIfShown(SmokeStrings.onboardingSkip);
        dismissIfShown(SmokeStrings.onboardingCoachmarkDismiss);

        await(SmokeStrings.tabLabel, "the Guide tab (is the app locked, or blocked by the install guard?)").click();
        assertFalse("the Guide failed to load in the minified build",
                device.wait(Until.hasObject(By.text(SmokeStrings.loadFailedTitle)), SHORT_MS));
        await(SmokeStrings.homeSection, "Guide Home");
        await(areaTitle, "the first area tile").click();
        await(caseTitle, "the first case tile in " + areaTitle).click();
        await(cardTitle, "the first card in the " + caseTitle + " feed").click();
        await(SmokeStrings.sectionKeyPoints, "the card's key points");
    }

    private static String cardTitle(JSONObject bundle, String cardId) throws Exception {
        JSONArray cards = bundle.getJSONArray("cards");
        for (int i = 0; i < cards.length(); i++) {
            if (cards.getJSONObject(i).getString("id").equals(cardId)) return cards.getJSONObject(i).getString("title");
        }
        throw new AssertionError("the bundle has no card " + cardId);
    }

    private String readBundle() throws Exception {
        Context app = context.createPackageContext(APP_ID, 0);
        try (InputStream in = app.getAssets().open(BUNDLE_ASSET)) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private void launchApp() {
        Intent intent = context.getPackageManager().getLaunchIntentForPackage(APP_ID);
        assertNotNull("the minifiedTest app is not installed", intent);
        context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
        device.wait(Until.hasObject(By.pkg(APP_ID).depth(0)), LONG_MS);
    }

    private void dismissIfShown(String text) {
        UiObject2 button = device.wait(Until.findObject(By.text(text)), SHORT_MS);
        if (button != null) button.click();
        device.waitForIdle();
    }

    /** Waits for [text], dismissing the Play asset-pack sheet whenever it appears on top (it can come late). */
    private UiObject2 await(String text, String what) {
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
